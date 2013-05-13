/*
# Copyright 2013, Mayo Foundation, Rochester MN. All rights reserved
# Written by Ben Brinkmann, Matt Stead, Dan Crepeau, Vince Vasoli, and Mark Bower
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

import java.io.PrintWriter;
import java.io.Serializable;

public class RED {
	HeaderOfRED header;
	int BLOCK_HEADER_BYTES = 287;
	long CARRY_CHECK = 0x7F800000;
	long TOP_VALUE = 0x80000000;
	long TOP_VALUE_M_1 = 0x7FFFFFFF;
	long BOTTOM_VALUE = 0x800000;
	long BOTTOM_VALUE_M_1 = 0x7FFFFF;
	long SHIFT_BITS = 23;
	long EXTRA_BITS = 7;
	int FILLER_BYTE = 0x55;
	long range;
	long low_bound;
	int[] diff_buffer = null;
	int[] in_buffer = null;
	long[] cnts = null;
	long[] cum_cnts = null;
	byte[] encrypted_bytes = null;
	int[] output_int = null;
	int currentSizeOfByteArray;
	int buffer_p;
	int bytes_p;
	int in_byte;
	boolean printAllFlag = false;
	PrintWriter pw;

	static final long[] crc_tab32 = { 0x0, 0x09695c4ca, 0xfb4839c9, 0x6dddfd03,
			0x20f3c3cf, 0xb6660705, 0xdbbbfa06, 0x4d2e3ecc, 0x41e7879e,
			0xd7724354, 0xbaafbe57, 0x2c3a7a9d, 0x61144451, 0xf781809b,
			0x9a5c7d98, 0xcc9b952, 0x83cf0f3c, 0x155acbf6, 0x788736f5,
			0xee12f23f, 0xa33cccf3, 0x35a90839, 0x5874f53a, 0xcee131f0,
			0xc22888a2, 0x54bd4c68, 0x3960b16b, 0xaff575a1, 0xe2db4b6d,
			0x744e8fa7, 0x199372a4, 0x8f06b66e, 0xd1fdae25, 0x47686aef,
			0x2ab597ec, 0xbc205326, 0xf10e6dea, 0x679ba920, 0xa465423,
			0x9cd390e9, 0x901a29bb, 0x68fed71, 0x6b521072, 0xfdc7d4b8,
			0xb0e9ea74, 0x267c2ebe, 0x4ba1d3bd, 0xdd341777, 0x5232a119,
			0xc4a765d3, 0xa97a98d0, 0x3fef5c1a, 0x72c162d6, 0xe454a61c,
			0x89895b1f, 0x1f1c9fd5, 0x13d52687, 0x8540e24d, 0xe89d1f4e,
			0x7e08db84, 0x3326e548, 0xa5b32182, 0xc86edc81, 0x5efb184b,
			0x7598ec17, 0xe30d28dd, 0x8ed0d5de, 0x18451114, 0x556b2fd8,
			0xc3feeb12, 0xae231611, 0x38b6d2db, 0x347f6b89, 0xa2eaaf43,
			0xcf375240, 0x59a2968a, 0x148ca846, 0x82196c8c, 0xefc4918f,
			0x79515545, 0xf657e32b, 0x60c227e1, 0xd1fdae2, 0x9b8a1e28,
			0xd6a420e4, 0x4031e42e, 0x2dec192d, 0xbb79dde7, 0xb7b064b5,
			0x2125a07f, 0x4cf85d7c, 0xda6d99b6, 0x9743a77a, 0x1d663b0,
			0x6c0b9eb3, 0xfa9e5a79, 0xa4654232, 0x32f086f8, 0x5f2d7bfb,
			0xc9b8bf31, 0x849681fd, 0x12034537, 0x7fdeb834, 0xe94b7cfe,
			0xe582c5ac, 0x73170166, 0x1ecafc65, 0x885f38af, 0xc5710663,
			0x53e4c2a9, 0x3e393faa, 0xa8acfb60, 0x27aa4d0e, 0xb13f89c4,
			0xdce274c7, 0x4a77b00d, 0x7598ec1, 0x91cc4a0b, 0xfc11b708,
			0x6a8473c2, 0x664dca90, 0xf0d80e5a, 0x9d05f359, 0xb903793,
			0x46be095f, 0xd02bcd95, 0xbdf63096, 0x2b63f45c, 0xeb31d82e,
			0x7da41ce4, 0x1079e1e7, 0x86ec252d, 0xcbc21be1, 0x5d57df2b,
			0x308a2228, 0xa61fe6e2, 0xaad65fb0, 0x3c439b7a, 0x519e6679,
			0xc70ba2b3, 0x8a259c7f, 0x1cb058b5, 0x716da5b6, 0xe7f8617c,
			0x68fed712, 0xfe6b13d8, 0x93b6eedb, 0x5232a11, 0x480d14dd,
			0xde98d017, 0xb3452d14, 0x25d0e9de, 0x2919508c, 0xbf8c9446,
			0xd2516945, 0x44c4ad8f, 0x9ea9343, 0x9f7f5789, 0xf2a2aa8a,
			0x64376e40, 0x3acc760b, 0xac59b2c1, 0xc1844fc2, 0x57118b08,
			0x1a3fb5c4, 0x8caa710e, 0xe1778c0d, 0x77e248c7, 0x7b2bf195,
			0xedbe355f, 0x8063c85c, 0x16f60c96, 0x5bd8325a, 0xcd4df690,
			0xa0900b93, 0x3605cf59, 0xb9037937, 0x2f96bdfd, 0x424b40fe,
			0xd4de8434, 0x99f0baf8, 0xf657e32, 0x62b88331, 0xf42d47fb,
			0xf8e4fea9, 0x6e713a63, 0x3acc760, 0x953903aa, 0xd8173d66,
			0x4e82f9ac, 0x235f04af, 0xb5cac065, 0x9ea93439, 0x83cf0f3,
			0x65e10df0, 0xf374c93a, 0xbe5af7f6, 0x28cf333c, 0x4512ce3f,
			0xd3870af5, 0xdf4eb3a7, 0x49db776d, 0x24068a6e, 0xb2934ea4,
			0xffbd7068, 0x6928b4a2, 0x4f549a1, 0x92608d6b, 0x1d663b05,
			0x8bf3ffcf, 0xe62e02cc, 0x70bbc606, 0x3d95f8ca, 0xab003c00,
			0xc6ddc103, 0x504805c9, 0x5c81bc9b, 0xca147851, 0xa7c98552,
			0x315c4198, 0x7c727f54, 0xeae7bb9e, 0x873a469d, 0x11af8257,
			0x4f549a1c, 0xd9c15ed6, 0xb41ca3d5, 0x2289671f, 0x6fa759d3,
			0xf9329d19, 0x94ef601a, 0x27aa4d0, 0xeb31d82, 0x9826d948,
			0xf5fb244b, 0x636ee081, 0x2e40de4d, 0xb8d51a87, 0xd508e784,
			0x439d234e, 0xcc9b9520, 0x5a0e51ea, 0x37d3ace9, 0xa1466823,
			0xec6856ef, 0x7afd9225, 0x17206f26, 0x81b5abec, 0x8d7c12be,
			0x1be9d674, 0x76342b77, 0xe0a1efbd, 0xad8fd171, 0x3b1a15bb,
			0x56c7e8b8, 0xc0522c72 };

	public RED() {
		header = new HeaderOfRED();
	}
	
	private int CastAsUI1(int symbol)
	{
		if (symbol < 0)
			return symbol + 256;
		return symbol;
	}

	public int compress_block(int[] in_buffer, byte[] out_buffer,
			int num_entries, long uUTC_time, int discontinuity) {
		if (diff_buffer == null)
			diff_buffer = new int[num_entries * 4];

		if (diff_buffer.length < (num_entries * 4))
			diff_buffer = new int[num_entries * 4];
		
		if ( cnts == null ) 
			cnts = new long[256];

		/*** generate differences ***/
		int si1_p1 = 0;
		int si1_p2 = 0;

		// first entry is full value (3 bytes)
		diff_buffer[si1_p1++] = in_buffer[si1_p2] & 0xFF;
		diff_buffer[si1_p1++] = (in_buffer[si1_p2] & 0xFF00) >> 8;
		diff_buffer[si1_p1++] = (in_buffer[si1_p2] & 0xFF0000) >> 16;

		int min_data_value = in_buffer[0];
		int max_data_value = in_buffer[0];

		int diff;
		for (int i = 1; i < num_entries; i++) {
			diff = in_buffer[i] - in_buffer[i - 1];
			if (in_buffer[i] > max_data_value)
				max_data_value = in_buffer[i];
			else if (in_buffer[i] < min_data_value)
				min_data_value = in_buffer[i];
			if (diff > 127 || diff < -127) { // for little endian input
				si1_p2 = i;
				diff_buffer[si1_p1++] = 128; // -128, hardcoded the conversion
				diff_buffer[si1_p1++] = in_buffer[si1_p2] & 0xFF;
				diff_buffer[si1_p1++] = (in_buffer[si1_p2] & 0xFF00) >> 8;
				diff_buffer[si1_p1++] = (in_buffer[si1_p2] & 0xFF0000) >> 16;
			} else
				diff_buffer[si1_p1++] = diff;
		}
		long diff_cnts = si1_p1;

		/*** generate statistics ***/
		for (int i = 0; i < 256; i++)
			cnts[i] = 0;
		int ui1_p1 = 0;
		long[] cnts = new long[256];
		for (long i = diff_cnts; (i-- > 0);)
			++cnts[CastAsUI1(diff_buffer[ui1_p1++])];

		long max_cnt = 0;
		for (int i = 0; i < 256; ++i)
			if (cnts[i] > max_cnt)
				max_cnt = cnts[i];

		// TBD the following code needs to be verified
		double stats_scale;
		if (max_cnt > 255) {
			stats_scale = 254.999 / max_cnt;
			for (int i = 0; i < 256; ++i)
				cnts[i] = (long) Math.ceil(cnts[i] * stats_scale);
		}

		long[] cum_cnts = new long[256];
		cum_cnts[0] = 0;
		for (int i = 0; i < 255; ++i)
			cum_cnts[i + 1] = cnts[i] + cum_cnts[i];
		long scaled_tot_cnts = cnts[255] + cum_cnts[255];

		/*** range encode ***/
		RANGE_STATS stats = new RANGE_STATS(out_buffer);
		ui1_p1 = 0;
		for (long i = diff_cnts; (i-- > 0); ++ui1_p1)
			stats.encode_symbol(CastAsUI1(diff_buffer[ui1_p1]), cnts[CastAsUI1(diff_buffer[ui1_p1])],
					cum_cnts[CastAsUI1(diff_buffer[ui1_p1])], scaled_tot_cnts);
		stats.done_encoding();

		// ensure 8-byte alignment for next block
		long comp_len = stats.get_obp();
		long extra_bytes = 8 - comp_len % 8;

		int temp_obp = stats.get_obp();
		if (extra_bytes < 8) {
			for (long i = 0; i < extra_bytes; i++)
				out_buffer[temp_obp++] = (byte) (FILLER_BYTE);
		}

		/*** write the packet & packet header ***/
		/*
		 * 4 byte checksum, 8 byte time value, 4 byte compressed byte count, 4
		 * byte difference count,
		 */
		/*
		 * 4 byte sample count, 3 byte data maximum, 3 byte data minimum, 256
		 * byte model counts
		 */

		ui1_p1 = 0;

		// fill checksum with zero as a placeholder
		for (int i = 0; i < 4; i++)
			out_buffer[ui1_p1++] = 0;

		long comp_block_len = temp_obp - BLOCK_HEADER_BYTES;

		Pack4Bytes(out_buffer, ui1_p1, comp_block_len);
		ui1_p1 += 4;

		Pack8Bytes(out_buffer, ui1_p1, uUTC_time);
		ui1_p1 += 8;

		Pack4Bytes(out_buffer, ui1_p1, diff_cnts);
		ui1_p1 += 4;

		Pack4Bytes(out_buffer, ui1_p1, num_entries);
		ui1_p1 += 4;

		Pack3Bytes(out_buffer, ui1_p1, max_data_value); // encode max and min
														// values as si3
		ui1_p1 += 3;

		Pack3Bytes(out_buffer, ui1_p1, min_data_value); // encode max and min
														// values as si3
		ui1_p1 += 3;

		out_buffer[ui1_p1++] = (byte) (discontinuity & 0xFF);

		// ehbp = ui1_p1;

		for (int i = 0; i < 256; ++i)
			out_buffer[ui1_p1++] = (byte) (cnts[i] & 0xFF);

		/*
		 * if (data_encryption==MEF_TRUE) { if (key==NULL) { fprintf(stderr,
		 * "[%s] Error: Null Encryption Key with encrypted block header\n",
		 * __FUNCTION__); return(-1); } else AES_encryptWithKey(ehbp, ehbp,
		 * key); //expanded key }
		 */

		byte[] checksum = new byte[4];
		CalcCRCchecksum(checksum, out_buffer, comp_block_len
				+ BLOCK_HEADER_BYTES);

		for (int i = 0; i < 4; i++)
			out_buffer[i] = checksum[i];

		header.SetMaxValue(max_data_value);
		header.SetMinValue(min_data_value);

		return (int) (comp_block_len + BLOCK_HEADER_BYTES);
	}

	public class HeaderOfRED implements Serializable {
		int max_value;
		int min_value;

		public HeaderOfRED() {
		}

		void SetMaxValue(int mv) {
			max_value = mv;
		}

		int GetMaxValue() {
			return max_value;
		}

		void SetMinValue(int mv) {
			min_value = mv;
		}

		int GetMinValue() {
			return min_value;
		}
	}

	private class RANGE_STATS {

		long low_bound;
		long range;
		int out_byte;
		long underflow_bytes;
		int ob_p;
		byte[] out_buffer;

		public RANGE_STATS(byte[] buffer) {
			low_bound = 0;
			out_byte = 0;
			underflow_bytes = 0;
			// range = TOP_VALUE;
			range = TOP_VALUE_M_1;
			range++;
			ob_p = BLOCK_HEADER_BYTES;
			out_buffer = buffer;
		}

		public int get_obp() {
			return ob_p;
		}

		private void enc_normalize() {
			while (range <= BOTTOM_VALUE) {
				if (low_bound < CARRY_CHECK) { // no carry possible => output
					out_buffer[ob_p++] = (byte) (out_byte & 0xFF);
					for (; (underflow_bytes != 0); underflow_bytes--)
						out_buffer[ob_p++] = (byte) (0xFF);
					out_byte = (int) ((low_bound >> SHIFT_BITS) & 0xFF);
				} else if ((low_bound & TOP_VALUE) != 0) { // carry now, no
															// future carry
					out_buffer[ob_p++] = (byte) ((out_byte + 1) & 0xFF);
					for (; (underflow_bytes != 0); underflow_bytes--)
						out_buffer[ob_p++] = 0;
					out_byte = (int) ((low_bound >> SHIFT_BITS) & 0xFF);
				} else
					// pass on a potential carry
					underflow_bytes++;
				range <<= 8;
				low_bound = (low_bound << 8) & TOP_VALUE_M_1;
			}

			return;
		}

		public void encode_symbol(int symbol, long symbol_cnts,
				long cnts_lt_symbol, long tot_cnts) {
			long r, tmp;

			enc_normalize();
			low_bound += (tmp = (r = range / tot_cnts) * cnts_lt_symbol);
			if (symbol < 0xFF) // not last symbol
				range = r * symbol_cnts;
			else
				// last symbol
				range -= tmp; // special case improves compression
			// at expense of speed
			return;
		}

		public void done_encoding() {
			enc_normalize();

			long tmp = low_bound;
			tmp = (tmp >> SHIFT_BITS) + 1;
			if (tmp > 0xFF) {
				out_buffer[ob_p++] = (byte) ((out_byte + 1) & 0xFF);
				for (; (underflow_bytes != 0); underflow_bytes--)
					out_buffer[ob_p++] = 0;
			} else {
				out_buffer[ob_p++] = (byte) (out_byte & 0xFF);
				for (; (underflow_bytes != 0); underflow_bytes--)
					out_buffer[ob_p++] = (byte) (0xFF);
			}
			out_buffer[ob_p++] = (byte) (tmp & 0xFF);
			out_buffer[ob_p++] = 0;
			out_buffer[ob_p++] = 0;
			out_buffer[ob_p++] = 0;

			return;
		}
	}

	private void Pack3Bytes(byte[] out_buffer, int index, long value) {
		out_buffer[index] = (byte) (value & 0xFF);
		out_buffer[index + 1] = (byte) ((value & 0xFF00) >> 8);
		out_buffer[index + 2] = (byte) ((value & 0xFF0000) >> 16);
	}

	private void Pack4Bytes(byte[] out_buffer, int index, long value) {
		out_buffer[index] = (byte) (value & 0xFF);
		out_buffer[index + 1] = (byte) ((value & 0xFF00) >> 8);
		out_buffer[index + 2] = (byte) ((value & 0xFF0000) >> 16);
		out_buffer[index + 3] = (byte) ((value & 0xFF000000) >> 24);
	}

	private void Pack8Bytes(byte[] out_buffer, int index, long value) {
		out_buffer[index] = (byte) (value & 0xFF);
		out_buffer[index + 1] = (byte) ((value & 0xFF00) >> 8);
		out_buffer[index + 2] = (byte) ((value & 0xFF0000) >> 16);
		out_buffer[index + 3] = (byte) ((value & 0xFF000000) >> 24);
		value = value >> 32;
		out_buffer[index + 4] = (byte) (value & 0xFF);
		out_buffer[index + 5] = (byte) ((value & 0xFF00) >> 8);
		out_buffer[index + 6] = (byte) ((value & 0xFF0000) >> 16);
		out_buffer[index + 7] = (byte) ((value & 0xFF000000) >> 24);
	}

	private void CalcCRCchecksum(byte[] checksum, byte[] out_buffer,
			long num_bytes) {
		checksum[0] = (byte) 0xFF;
		checksum[1] = (byte) 0xFF;
		checksum[2] = (byte) 0xFF;
		checksum[3] = (byte) 0xFF;

		byte[] new_checksum = new byte[4];
		for (int i = 4; i < num_bytes; i++) {
			update_crc_32(checksum, out_buffer[i], new_checksum);
			for (int j = 0; j < 4; j++)
				checksum[j] = new_checksum[j];
		}
	}

	private void update_crc_32(byte[] checksum, int c, byte[] new_checksum) {
		int tmp = checksum[0] ^ (c & 0xFF);
		if (tmp < 0)
			tmp += 256;

		new_checksum[0] = (byte) (checksum[1] ^ ((byte) (crc_tab32[tmp] & 0xFF)));
		new_checksum[1] = (byte) (checksum[2] ^ ((byte) ((crc_tab32[tmp] >> 8) & 0xFF)));
		new_checksum[2] = (byte) (checksum[3] ^ ((byte) ((crc_tab32[tmp] >> 16) & 0xFF)));
		new_checksum[3] = (byte) ((crc_tab32[tmp] >> 24) & 0xFF);
	}

	public HeaderOfRED GetHeaderOfRED() {
		return header;
	}
}