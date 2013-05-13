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

import java.util.Random;

public class MefHeader {
	public String	institution;
	public String	unencrypted_text_field;
	public String	encryption_algorithm;
	public int	subject_encryption_used;
	public int	session_encryption_used;
	public int	data_encryption_used;
	public int	byte_order_code;
	public int	header_version_major;
	public int	header_version_minor;
	public byte[]	session_unique_ID;
	public int	header_length;
	public String	subject_first_name;
	public String	subject_second_name;
	public String	subject_third_name;
	public String	subject_id;
	public String	session_password;
	public String	subject_validation_field;
	public String	session_validation_field;
	public long	number_of_samples;
	public String	channel_name;
	public long	recording_start_time;
	public long	recording_end_time;
	public double	sampling_frequency;
	public double	low_frequency_filter_setting;
	public double	high_frequency_filter_setting;
	public double	notch_filter_frequency;
	public double	voltage_conversion_factor;
	public String	acquisition_system;
	public String	channel_comments;
	public String	study_comments;
	public int	physical_channel_number;
	public String	compression_algorithm;
	public long	maximum_compressed_block_size;
	public long maximum_block_length; 
	public long	block_interval;
	public int maximum_data_value;
	public int minimum_data_value;
	public long	index_data_offset;
	public long number_of_index_entries;
	public int block_header_length;
	public float GMT_offset;
	public long discontinuity_data_offset;
	public long number_of_discontinuity_entries;
	public byte[] file_unique_ID;
	public String anonymized_subject_name;
	public byte[] header_crc;
	private Random random;
	
	public MefHeader() {
		random = new Random();
		
		// set defaults, some of these can be over-ridden
		byte_order_code = 1;              // little-endian
		header_version_major = 2;
		header_version_minor = 1;
		header_length = 1024;
		GMT_offset = (float)-6.0;         // CST time zone
		voltage_conversion_factor = 1.0;
		low_frequency_filter_setting = -1.0;
		high_frequency_filter_setting = -1.0;
		notch_filter_frequency = -1.0;
		physical_channel_number = -1;
		subject_encryption_used = 0;
		session_encryption_used = 0;
		data_encryption_used = 0;
		recording_start_time = 0;
		encryption_algorithm = "128-bit AES";
		compression_algorithm = "Range Encoded Differences (RED)";
		block_header_length = 287;
		
		// default random ID's in case none are ever specified
		generate_unique_file_ID();
		generate_unique_session_ID();
	}
	
	public void generate_unique_file_ID() {
		file_unique_ID = new byte[8];
		random.nextBytes(file_unique_ID);
	}
	
	public void generate_unique_session_ID() {
		session_unique_ID = new byte[8];
		random.nextBytes(session_unique_ID);
	}
	
	public byte[] get8RandomBytes() {
		byte[] new_bytes = new byte[8];
		random.nextBytes(new_bytes);
		return new_bytes;
	}
    private void CalcCRCchecksum(byte[] checksum, byte[] out_buffer, long num_bytes) {
    	checksum[0] = (byte)0xFF;
    	checksum[1] = (byte)0xFF;
    	checksum[2] = (byte)0xFF;
    	checksum[3] = (byte)0xFF;
    	
    	byte[] new_checksum = new byte[4];
        for (int i=0;i<num_bytes-4;i++){
        	update_crc_32(checksum, out_buffer[i], new_checksum);
            for (int j=0;j<4;j++)
            	checksum[j] = new_checksum[j];
        } 	
    }
    
    private void update_crc_32(byte[] checksum, int c, byte[] new_checksum) {
       	int tmp = checksum[0] ^ (c & 0xFF);
    	if (tmp < 0) tmp += 256;
    	
    	new_checksum[0] = (byte)(checksum[1] ^ ((byte)(crc_tab32[tmp] & 0xFF)));
    	new_checksum[1] = (byte)(checksum[2] ^ ((byte)((crc_tab32[tmp] >> 8) & 0xFF)));
    	new_checksum[2] = (byte)(checksum[3] ^ ((byte)((crc_tab32[tmp] >> 16) & 0xFF)));
    	new_checksum[3] = (byte)((crc_tab32[tmp] >> 24) & 0xFF);
    }  
	
    private void PackString(byte[] buffer, int buffer_beginning, int max_chars, String str) {
    	if (str == null)
    		return;
    	int max_string = str.length();
    	if (str.length() > max_chars)
    		max_string = max_chars;
    	for (int i=0;i<max_string;i++)
    		buffer[buffer_beginning+i] = (byte)(str.charAt(i));
    }
    
    private void PackInt1(byte[] buffer, int buffer_beginning, int the_int) {
    	buffer[buffer_beginning] = (byte)(the_int & 0xFF);
    }
    
    private void PackInt2(byte[] buffer, int buffer_beginning, int the_int) {
    	buffer[buffer_beginning] = (byte)(the_int & 0xFF);
    	buffer[buffer_beginning+1] = (byte)((the_int >> 8) & 0xFF);
    }
    
    private void PackInt4(byte[] buffer, int buffer_beginning, int the_int) {
    	buffer[buffer_beginning] = (byte)(the_int & 0xFF);
    	buffer[buffer_beginning+1] = (byte)((the_int >> 8) & 0xFF);
    	buffer[buffer_beginning+2] = (byte)((the_int >> 16) & 0xFF);
    	buffer[buffer_beginning+3] = (byte)((the_int >> 24) & 0xFF);
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
    
    private void PackFloat4(byte[] buffer, int buffer_beginning, float the_float) {
    	PackInt4(buffer, buffer_beginning, Float.floatToRawIntBits(the_float));
    }

    private void PackFloat8(byte[] buffer, int buffer_beginning, double the_double) {
    	PackInt8(buffer, buffer_beginning, Double.doubleToRawLongBits(the_double));
    }
    
    private void PackByte8(byte[] buffer, int buffer_beginning, byte[] the_ID) {
    	for (int i=0;i<8;i++)
    		buffer[buffer_beginning+i] = the_ID[i];
    }
    
	public byte[] serialize() {
		// allocate and init buffer
	    byte[] out_data = new byte[1024];
	    for (int i=0;i<1024;i++)
	    	out_data[i] = 0;
	    
	    // Pack header data
	    PackString(out_data, 0, 63, institution);
	    PackString(out_data, 64, 63, unencrypted_text_field);
	    PackString(out_data, 128, 31, encryption_algorithm);
	    PackInt1(out_data, 160, subject_encryption_used);
	    PackInt1(out_data, 161, session_encryption_used);
	    PackInt1(out_data, 162, data_encryption_used);
	    PackInt1(out_data, 163, byte_order_code);
	    PackInt1(out_data, 164, header_version_major);
	    PackInt1(out_data, 165, header_version_minor);
	    PackInt2(out_data, 166, header_length);
	    PackByte8(out_data, 168, session_unique_ID);
	    PackString(out_data, 176, 31, subject_first_name);
	    PackString(out_data, 208, 31, subject_second_name);
	    PackString(out_data, 240, 31, subject_third_name);
	    PackString(out_data, 272, 31, subject_id);
	    PackInt8(out_data, 368, number_of_samples);
	    PackString(out_data, 376, 31, channel_name);
	    PackInt8(out_data, 408, recording_start_time);
	    PackInt8(out_data, 416, recording_end_time);
	    PackFloat8(out_data, 424, sampling_frequency);
	    PackFloat8(out_data, 432, low_frequency_filter_setting);
	    PackFloat8(out_data, 440, high_frequency_filter_setting);
	    PackFloat8(out_data, 448, notch_filter_frequency);
	    PackFloat8(out_data, 456, voltage_conversion_factor);
	    PackString(out_data, 464, 31, acquisition_system);
	    PackString(out_data, 496, 127, channel_comments);
	    PackString(out_data, 624, 127, study_comments);
	    PackInt4(out_data, 752, physical_channel_number);
	    PackString(out_data, 756, 31, compression_algorithm);
	    PackInt4(out_data, 788, (int)maximum_compressed_block_size);
	    PackInt8(out_data, 792, maximum_block_length);
	    PackInt8(out_data, 800, block_interval);
	    PackInt4(out_data, 808, maximum_data_value);
	    PackInt4(out_data, 812, minimum_data_value);
	    PackInt8(out_data, 816, index_data_offset);
	    PackInt8(out_data, 824, number_of_index_entries);
	    PackInt2(out_data, 832, block_header_length);
	    PackFloat4(out_data, 836, GMT_offset);
	    PackInt8(out_data, 840, discontinuity_data_offset);
	    PackInt8(out_data, 848, number_of_discontinuity_entries);
	    PackByte8(out_data, 948, file_unique_ID);
	    PackString(out_data, 956, 63, anonymized_subject_name);
	    
	    // Pack checksum
        byte[] checksum = new byte[4];
        CalcCRCchecksum(checksum, out_data,1024);
        for (int i=0;i<4;i++)
        	out_data[1020+i] = checksum[i];
	    
		return out_data;
	}
	
	static final long[] crc_tab32 = {0x0, 0x09695c4ca, 0xfb4839c9, 0x6dddfd03, 0x20f3c3cf, 0xb6660705, 0xdbbbfa06, 0x4d2e3ecc, 0x41e7879e, 0xd7724354, 0xbaafbe57, 0x2c3a7a9d, 0x61144451, 0xf781809b, 0x9a5c7d98, 0xcc9b952, 0x83cf0f3c, 0x155acbf6, 0x788736f5, 0xee12f23f, 0xa33cccf3, 0x35a90839, 0x5874f53a, 0xcee131f0, 0xc22888a2, 0x54bd4c68, 0x3960b16b, 0xaff575a1, 0xe2db4b6d, 0x744e8fa7, 0x199372a4, 0x8f06b66e, 0xd1fdae25, 0x47686aef, 0x2ab597ec, 0xbc205326, 0xf10e6dea, 0x679ba920, 0xa465423, 0x9cd390e9, 0x901a29bb, 0x68fed71, 0x6b521072, 0xfdc7d4b8, 0xb0e9ea74, 0x267c2ebe, 0x4ba1d3bd, 0xdd341777, 0x5232a119, 0xc4a765d3, 0xa97a98d0, 0x3fef5c1a, 0x72c162d6, 0xe454a61c, 0x89895b1f, 0x1f1c9fd5, 0x13d52687, 0x8540e24d, 0xe89d1f4e, 0x7e08db84, 0x3326e548, 0xa5b32182, 0xc86edc81, 0x5efb184b, 0x7598ec17, 0xe30d28dd, 0x8ed0d5de, 0x18451114, 0x556b2fd8, 0xc3feeb12, 0xae231611, 0x38b6d2db, 0x347f6b89, 0xa2eaaf43, 0xcf375240, 0x59a2968a, 0x148ca846, 0x82196c8c, 0xefc4918f, 0x79515545, 0xf657e32b, 0x60c227e1, 0xd1fdae2, 0x9b8a1e28, 0xd6a420e4, 0x4031e42e, 0x2dec192d, 0xbb79dde7, 0xb7b064b5, 0x2125a07f, 0x4cf85d7c, 0xda6d99b6, 0x9743a77a, 0x1d663b0, 0x6c0b9eb3, 0xfa9e5a79, 0xa4654232, 0x32f086f8, 0x5f2d7bfb, 0xc9b8bf31, 0x849681fd, 0x12034537, 0x7fdeb834, 0xe94b7cfe, 0xe582c5ac, 0x73170166, 0x1ecafc65, 0x885f38af, 0xc5710663, 0x53e4c2a9, 0x3e393faa, 0xa8acfb60, 0x27aa4d0e, 0xb13f89c4, 0xdce274c7, 0x4a77b00d, 0x7598ec1, 0x91cc4a0b, 0xfc11b708, 0x6a8473c2, 0x664dca90, 0xf0d80e5a, 0x9d05f359, 0xb903793, 0x46be095f, 0xd02bcd95, 0xbdf63096, 0x2b63f45c, 0xeb31d82e, 0x7da41ce4, 0x1079e1e7, 0x86ec252d, 0xcbc21be1, 0x5d57df2b, 0x308a2228, 0xa61fe6e2, 0xaad65fb0, 0x3c439b7a, 0x519e6679, 0xc70ba2b3, 0x8a259c7f, 0x1cb058b5, 0x716da5b6, 0xe7f8617c, 0x68fed712, 0xfe6b13d8, 0x93b6eedb, 0x5232a11, 0x480d14dd, 0xde98d017, 0xb3452d14, 0x25d0e9de, 0x2919508c, 0xbf8c9446, 0xd2516945, 0x44c4ad8f, 0x9ea9343, 0x9f7f5789, 0xf2a2aa8a, 0x64376e40, 0x3acc760b, 0xac59b2c1, 0xc1844fc2, 0x57118b08, 0x1a3fb5c4, 0x8caa710e, 0xe1778c0d, 0x77e248c7, 0x7b2bf195, 
		0xedbe355f, 0x8063c85c, 0x16f60c96, 0x5bd8325a, 0xcd4df690, 0xa0900b93, 0x3605cf59, 0xb9037937, 0x2f96bdfd, 0x424b40fe, 0xd4de8434, 0x99f0baf8, 0xf657e32, 0x62b88331, 0xf42d47fb, 0xf8e4fea9, 0x6e713a63, 0x3acc760, 0x953903aa, 0xd8173d66, 0x4e82f9ac, 0x235f04af, 0xb5cac065, 0x9ea93439, 0x83cf0f3, 0x65e10df0, 0xf374c93a, 0xbe5af7f6, 0x28cf333c, 0x4512ce3f, 0xd3870af5, 0xdf4eb3a7, 0x49db776d, 0x24068a6e, 0xb2934ea4, 0xffbd7068, 0x6928b4a2, 0x4f549a1, 0x92608d6b, 0x1d663b05, 0x8bf3ffcf, 0xe62e02cc, 0x70bbc606, 0x3d95f8ca, 0xab003c00, 0xc6ddc103, 0x504805c9, 0x5c81bc9b, 0xca147851, 0xa7c98552, 0x315c4198, 0x7c727f54, 0xeae7bb9e, 0x873a469d, 0x11af8257, 0x4f549a1c, 0xd9c15ed6, 0xb41ca3d5, 0x2289671f, 0x6fa759d3, 0xf9329d19, 0x94ef601a, 0x27aa4d0, 0xeb31d82, 0x9826d948, 0xf5fb244b, 0x636ee081, 0x2e40de4d, 0xb8d51a87, 0xd508e784, 0x439d234e, 0xcc9b9520, 0x5a0e51ea, 0x37d3ace9, 0xa1466823, 0xec6856ef, 0x7afd9225, 0x17206f26, 0x81b5abec, 0x8d7c12be, 0x1be9d674, 0x76342b77, 0xe0a1efbd, 0xad8fd171, 0x3b1a15bb, 0x56c7e8b8, 0xc0522c72};

}