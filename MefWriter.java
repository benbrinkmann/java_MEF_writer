/*
# Copyright 2013, Mayo Foundation, Rochester MN. All rights reserved
# Written by Ben Brinkmann, Matt Stead, Dan Crepeau, and Vince Vasoli
# usage and modification of this source code is governed by the Apache 2.0 license
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/

import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;

class MefWriterException extends RuntimeException {

	public MefWriterException(String msg){
		super(msg);
	}
}

public class MefWriter {

	/**
	 * @param args
	 */
	int INT_MAX = 2147483647;
	int INT_MIN = -INT_MAX - 1;
	int MEF_HEADER_LENGTH = 1024;

	int[] raw_data_array;
	int raw_data_ptr_current;
	byte[] temp_block_buffer;
	long block_hdr_time;
	long block_boundary;
	long last_chan_timestamp;
	long max_block_size;
	long max_block_len;
	int max_data_value_file;
	int min_data_value_file;
	long outfile_data_offset;
	long number_of_index_entries;
	long number_of_discontinuity_entries;
	long number_of_samples;
	long block_sample_index;
	int discontinuity_flag;
	int bit_shift_flag;
	double secs_per_block;
	BLOCK_INDEX_ELEMENT block_index_head;
	BLOCK_INDEX_ELEMENT block_index_current;
	DISCONTINUITY_INDEX_ELEMENT discontinuity_index_head;
	DISCONTINUITY_INDEX_ELEMENT discontinuity_index_current;
	long discontinuity_time_threshold;
	FileOutputStream out_file;
	String out_file_name;
	int file_closed;
	int called_writeData;
	int called_writeDataBlock;

	RED red_object;
	
	MefHeader header;

	// si4 private addBlockIndexToChannelList(ui8 block_hdr_time, ui8
	// outfile_data_offset, ui8 num_elements_processed)
	private void addBlockIndexToChannelList(long block_hdr_time,
			long outfile_data_offset, long num_elements_processed) {
		if (block_index_head == null) {
			// empty list, create first element
			block_index_head = new BLOCK_INDEX_ELEMENT();
			block_index_current = block_index_head;
		} else {
			// list is not empty, so add a new element
			block_index_current.next = new BLOCK_INDEX_ELEMENT();
			block_index_current = block_index_current.next;
		}

		// TBD do error checking
		/*
		 * if (block_index_current == null) { //fprintf(stderr,
		 * "Insufficient memory to allocate additional block index storage\n");
		 * //exit(1); }
		 */

		block_index_current.block_hdr_time = block_hdr_time;
		block_index_current.outfile_data_offset = outfile_data_offset;
		block_index_current.num_entries_processed = block_sample_index;

		// increase block_sample_index so the next block contains the correct
		// index value
		block_sample_index += num_elements_processed;

		return;
	}

	private void addDiscontinuityIndexToChannelList(long block_index) {
		if (discontinuity_index_head == null) {
			// empty list, create first element
			discontinuity_index_head = new DISCONTINUITY_INDEX_ELEMENT();
			discontinuity_index_current = discontinuity_index_head;
		} else {
			// list is not empty, so add a new element
			discontinuity_index_current.next = new DISCONTINUITY_INDEX_ELEMENT();
			discontinuity_index_current = discontinuity_index_current.next;
		}

		// TBD do error checking
		if (discontinuity_index_current == null) {
			// fprintf(stderr,
			// "Insufficient memory to allocate additional discontinuity index storage\n");
			// exit(1);
		}

		discontinuity_index_current.block_index = block_index;

		return;
	}

	public MefWriter(String filename, double seconds_per_block,
			double sample_frequency, long discontinuity_time_threshold_in_ms) {
		long block_len;

		header = new MefHeader();
		
		// set local constants
		block_len = (long) Math.ceil(seconds_per_block * sample_frequency); // user-defined
																			// block
																			// size
																			// (s),
																			// convert
																			// to
																			// #
																			// of
																			// samples

		// add 10% to buffer size to account for possible sample frequency drift
		raw_data_array = new int[(int) (seconds_per_block * sample_frequency * 1.10)];
		// TBD - check allocation
		/*
		 * if (raw_data_ptr_start == NULL) { fprintf(stderr,
		 * "Insufficient memory to allocate temporary channel buffer\n");
		 * exit(1); }
		 */

		raw_data_ptr_current = 0;
		temp_block_buffer = new byte[(int) (block_len * 8)];
		block_hdr_time = 0;
		block_boundary = 0;
		last_chan_timestamp = 0;
		max_block_size = 0;
		max_block_len = 0;
		max_data_value_file = INT_MIN;
		min_data_value_file = INT_MAX;
		outfile_data_offset = MEF_HEADER_LENGTH;
		number_of_index_entries = 0;
		number_of_discontinuity_entries = 0;
		number_of_samples = 0;
		block_sample_index = 0;
		discontinuity_flag = 1; // first block is by definition discontinuous
		bit_shift_flag = 0; // TBD: make this a variable? bit shifting is used
							// for neuralynx data
		secs_per_block = seconds_per_block;
		header.sampling_frequency = sample_frequency;
		header.block_interval = (int) (seconds_per_block * 1000000.0);
		block_index_head = null;
		block_index_current = null;
		discontinuity_index_head = null;
		discontinuity_index_current = null;
		discontinuity_time_threshold = discontinuity_time_threshold_in_ms;

		out_file_name = filename;
		
		// Open channel output file, and write header to it
		try {
			out_file = new FileOutputStream(out_file_name);
		} catch (FileNotFoundException e) {
			throw new MefWriterException("Can't create output mef file: " + out_file_name);
		}

		byte[] out_header = new byte[MEF_HEADER_LENGTH];

		// this writes a bogus header to the beginning of the file.
		// The header will be properly written upon closing the file.
		try {
			out_file.write(out_header, 0, MEF_HEADER_LENGTH);
		} catch (IOException e) {
			throw new MefWriterException("Can't write to output mef file: " + out_file_name);
		}

		// TBD do error checking
		/*
		 * if (nr != MEF_HEADER_LENGTH) { fprintf(stderr,
		 * "Error writing file\n"); exit(1); }
		 */

		red_object = new RED();
		
		file_closed = 0;
		called_writeData = 0;
		called_writeDataBlock = 0;
	}
	
	 private void PackInt8(byte[] buffer, int buffer_beginning, long the_int) {
	    	buffer[buffer_beginning] = (byte)(the_int & 0xFF);
	    	buffer[buffer_beginning+1] = (byte)((the_int >> 8) & 0xFF);
	    	buffer[buffer_beginning+2] = (byte)((the_int >> 16) & 0xFF);
	    	buffer[buffer_beginning+3] = (byte)((the_int >> 24) & 0xFF);
	    	the_int = the_int >> 32;
	    	buffer[buffer_beginning+4] = (byte)(the_int & 0xFF);
	    	buffer[buffer_beginning+5] = (byte)((the_int >> 8) & 0xFF);
	    	buffer[buffer_beginning+6] = (byte)((the_int >> 16) & 0xFF);
	    	buffer[buffer_beginning+7] = (byte)((the_int >> 24) & 0xFF);
	    }

	public void close() {
		if (file_closed == 1)
			throw new MefWriterException("Can't close a MefWriter object that has already been closed!");
		
		file_closed = 1;
		
		// finish and write the last block with leftover buffers
		if (called_writeData == 1)
			processFilledBlock(raw_data_ptr_current, discontinuity_flag,
				block_hdr_time, null);

		// update remaining unfilled mef header fields
		long discontinuity_data_offset = outfile_data_offset
				+ (number_of_index_entries * 24);
		header.maximum_compressed_block_size = max_block_size;
		header.maximum_block_length = max_block_len;
		header.maximum_data_value = max_data_value_file;
		header.minimum_data_value = min_data_value_file;
		header.index_data_offset = outfile_data_offset;
		header.number_of_index_entries = number_of_index_entries;
		header.discontinuity_data_offset = discontinuity_data_offset;
		header.number_of_discontinuity_entries = number_of_discontinuity_entries;
		header.number_of_samples = number_of_samples;
		header.recording_end_time = last_chan_timestamp;
		
		// extrapolate end of last block, if blocks are being directly entered
		if (called_writeDataBlock == 1)
			header.recording_end_time += secs_per_block * 1000000;

		// append block index
		BLOCK_INDEX_ELEMENT index_ptr = block_index_head;
		while (index_ptr != null) {
			// write first three elements (24 bytes) of block index
			byte[] bytes = new byte[8];
			PackInt8(bytes,0,index_ptr.block_hdr_time);
			try {
				out_file.write(bytes, 0, 8);
			} catch (IOException e) { // TBD add error checking
			}

			PackInt8(bytes,0,index_ptr.outfile_data_offset);
			try {
				out_file.write(bytes, 0, 8);
			} catch (IOException e) { // TBD add error checking
			}

			PackInt8(bytes,0,index_ptr.num_entries_processed);
			try {
				out_file.write(bytes, 0, 8);
			} catch (IOException e) { // TBD add error checking
			}

			index_ptr = index_ptr.next;
		}

		// append discontinuity index
		DISCONTINUITY_INDEX_ELEMENT discontinuity_ptr = discontinuity_index_head;
		while (discontinuity_ptr != null) {
			// write element of discontinuity index element
			byte[] bytes = new byte[8];
			PackInt8(bytes,0,discontinuity_ptr.block_index);
			try {
				out_file.write(bytes, 0, 8);
			} catch (IOException e) { // TBD add error checking
			}

			discontinuity_ptr = discontinuity_ptr.next;
		}
        
		try {
			out_file.close();
		} catch (IOException e) { 
			throw new MefWriterException("Can't close output mef file: " + out_file_name);
		}

		// Rewrite header, with completely filled in data
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(out_file_name, "rw");
		} catch (FileNotFoundException e) { 
			throw new MefWriterException("Can't open output mef file: " + out_file_name);
		}
		try {
			raf.seek(0);
			raf.write(header.serialize());
		} catch (IOException e) { 
			throw new MefWriterException("Can't write to output mef file: " + out_file_name);
		}
		try {
			raf.close();
		} catch (IOException e) { 
			throw new MefWriterException("Can't close output mef file: " + out_file_name);
		}

		// TBD, incorporate encryption?
	}

	private void processFilledBlock(int num_entries, int discontinuity_flag,
			long block_hdr_time, int samps[]) {
		byte[] out_data;
		int ddp;
		long RED_block_size;
		RED.HeaderOfRED block_hdr = null;

		// do nothing if there is nothing to be done
		if (num_entries == 0)
			return;

		if (header.recording_start_time == 0)
			header.recording_start_time = block_hdr_time;
		
		out_data = temp_block_buffer;

		if (samps != null)
			raw_data_array = samps;
		
		if (bit_shift_flag != 0) {
			// shift 2 bits to 18 bit resolution
			ddp = 0;
			for (int i = num_entries; (i-- != 0);) {
				if (raw_data_array[ddp] >= 0)
					raw_data_array[ddp++] = (int) (((double) raw_data_array[ddp] / (double) 4.0) + 0.5);
				else
					raw_data_array[ddp++] = (int) (((double) raw_data_array[ddp] / (double) 4.0) - 0.5);
			}
		}

		// RED compress data block
		RED_block_size = red_object.compress_block(raw_data_array, out_data,
				num_entries, block_hdr_time, discontinuity_flag);
		block_hdr = red_object.GetHeaderOfRED();

		// write block to output file
		try {
			out_file.write(out_data, 0, (int) RED_block_size);
		} catch (IOException e) { // TBD add error checking
		}

		// write block index info to linked list
		addBlockIndexToChannelList(block_hdr_time, outfile_data_offset,
				num_entries);

		// save extra info for .mef header
		if (RED_block_size > max_block_size)
			max_block_size = RED_block_size;
		if (num_entries > max_block_len)
			max_block_len = num_entries;
		if (block_hdr.GetMaxValue() > max_data_value_file)
			max_data_value_file = (int) block_hdr.GetMaxValue();
		if (block_hdr.GetMinValue() < min_data_value_file)
			min_data_value_file = (int) block_hdr.GetMinValue();

		// update mef header fields relating to block index
		outfile_data_offset += RED_block_size;
		number_of_index_entries++;

		number_of_samples += num_entries;

		// update discontinuity index and count
		if (discontinuity_flag != 0) {
			// fprintf(stderr, "discontinuity beginning at %ld\n",
			// block_hdr_time);
			addDiscontinuityIndexToChannelList(number_of_index_entries);
			number_of_discontinuity_entries++;
		}
	}

	public void setInstitution(String new_institution) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.institution = new_institution;
	}
	public void setUnencryptedTextField(String new_text) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.unencrypted_text_field = new_text;
	}
	public void setFirstName(String new_firstname) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.subject_first_name = new_firstname;
	}
	public void setSecondName(String new_secondname) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.subject_second_name = new_secondname;
	}
	public void setThirdName(String new_thirdname) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.subject_third_name = new_thirdname;
	}
	public void setSessionUniqueIdentifier(byte[] new_id) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		for (int i=0;i<4;i++)
			header.session_unique_ID[i] = new_id[i];
	}
	public void setSubjectID(String new_id) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.subject_id = new_id;
	}
	public void setChannelName(String new_channel_name) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.channel_name = new_channel_name;
	}
	public void setSamplingFrequency(double new_freq) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.sampling_frequency = new_freq;
	}
	public void setLowFrequencyFilterSetting(double new_filter) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.low_frequency_filter_setting = new_filter;
	}
	public void setHighFrequencyFilterSetting(double new_filter) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.high_frequency_filter_setting = new_filter;
	}
	public void setNotchFrequencyFilterSetting(double new_filter) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.notch_filter_frequency = new_filter;
	}
	public void setVoltageConversionFactor(double new_factor) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.voltage_conversion_factor = new_factor;
	}
	public void setAcquisitionSystem(String new_system) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.acquisition_system = new_system;
	}
	public void setChannelComments(String comments) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.channel_comments = comments;
	}
	public void setStudyComments(String comments) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.study_comments = comments;
	}
	public void setPhysicalChannelNumber(int num) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.physical_channel_number = num;
	}
	// this method doesn't exist, but the value is set in the constructor, and should never be changed
	/*public void setBlockInterval(long interval) {
				if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
			header.block_interval = interval;
	}*/
	public void setGMToffset(float offset) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.GMT_offset = offset;
	}
	public void setAnonymizedSubjectName(String name) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		header.anonymized_subject_name = name;
	}
	public byte[] get8RandomBytes() {
		return header.get8RandomBytes();
	}

	public void writeData(int[] samps, long[] timestamps, int n_packets_to_process) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		
		if (samps.length < n_packets_to_process)
			throw new MefWriterException("writeData(): n_packets_to_process ("+n_packets_to_process+") > length of samps array ("+samps.length+").");
		
		if (timestamps.length < n_packets_to_process)
			throw new MefWriterException("writeData(): n_packets_to_process ("+n_packets_to_process+") > length of timestamps array ("+timestamps.length+").");
		
		if (called_writeDataBlock == 1)
			throw new MefWriterException("writeData(): Can't call after writeDataBlock() has been called");
		
		called_writeData = 1;
		int samps_counter = 0;

		for (int j = 0; j < n_packets_to_process; ++j) {
			// set timestamp for the first block processed
			if (block_hdr_time == 0) {
				// block_hdr_time is the actual time put into the block header
				// (timestamp of the first
				// block sample), while block_boundary is used only for
				// calculation of which samples go
				// into which blocks. block_boundary is never written to the mef
				// file.
				block_hdr_time = timestamps[j];
				block_boundary = timestamps[j];
			}

			if (((timestamps[j] - last_chan_timestamp) >= discontinuity_time_threshold)
					|| ((timestamps[j] - block_boundary) >= header.block_interval)) {
				// Block needs to be compressed and written

				// See if data exists in the buffer before processing it. Data
				// might not exist if
				// this is the first sample we've processed so far.
				if (raw_data_ptr_current > 0) {
					// process block of previously collected data
					processFilledBlock(raw_data_ptr_current,
							discontinuity_flag, block_hdr_time, null);
				}

				// mark next block as being discontinuous if discontinuity is
				// found
				if ((timestamps[j] - last_chan_timestamp) >= discontinuity_time_threshold) {
					discontinuity_flag = 1;
					block_boundary = timestamps[j];
				} else {
					discontinuity_flag = 0;
					block_boundary += header.block_interval;
				}

				// set next block's timestamp
				block_hdr_time = timestamps[j];

				// move back to the beginning of the raw block
				raw_data_ptr_current = 0;
			}

			raw_data_array[raw_data_ptr_current++] = samps[samps_counter++];

			if (timestamps[j] < last_chan_timestamp)
				throw new MefWriterException("writeData(): Out-of-order timestamps detected ("+timestamps[j]+" < "+last_chan_timestamp+").");
			
			if (timestamps[j] == last_chan_timestamp)
				throw new MefWriterException("writeData(): Duplicate timestamps detected ("+timestamps[j]+").");
				
			last_chan_timestamp = timestamps[j];
		}
	}
	
	public void writeDataBlock(int[] samps, long timestamp, int n_packets_to_process, boolean discontinuity) {
		if (file_closed == 1)
			throw new MefWriterException("Can't modify a MefWriter object that has already been closed!");
		
		if (samps.length < n_packets_to_process)
			throw new MefWriterException("writeDatBlocka(): n_packets_to_process ("+n_packets_to_process+") > length of samps array ("+samps.length+").");
		
		if (timestamp < last_chan_timestamp)
			throw new MefWriterException("writeDatBlock(): Out-of-order timestamps detected ("+timestamp+" < "+last_chan_timestamp+").");
		
		if (timestamp == last_chan_timestamp)
			throw new MefWriterException("writeDataBlock(): Duplicate timestamps detected ("+timestamp+").");
			
		if (called_writeData == 1)
			throw new MefWriterException("writeDataBlock(): Can't call after writeData() has been called.");
		
		called_writeDataBlock = 1;
		last_chan_timestamp = timestamp;
		raw_data_ptr_current = 0;
		
		if (discontinuity)
			discontinuity_flag = 1;
		else
			discontinuity_flag = 0;
		
		// TBD have it detect discontinuities properly
		if (number_of_index_entries == 0)
			discontinuity_flag = 1;
		
		processFilledBlock(n_packets_to_process, discontinuity_flag, timestamp, samps);
	}

	private class BLOCK_INDEX_ELEMENT {
		public long block_hdr_time;
		public long outfile_data_offset;
		public long num_entries_processed;
		public BLOCK_INDEX_ELEMENT next;

		public BLOCK_INDEX_ELEMENT() {
			block_hdr_time = 0;
			outfile_data_offset = 0;
			num_entries_processed = 0;
			next = null;
		}
	}

	private class DISCONTINUITY_INDEX_ELEMENT {
		public long block_index;
		public DISCONTINUITY_INDEX_ELEMENT next;

		public DISCONTINUITY_INDEX_ELEMENT() {
			block_index = 0;
			next = null;
		}
	}

}
