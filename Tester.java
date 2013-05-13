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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class Tester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		new Tester();
	}
	
	public Tester() {
		//RunTest1();
		//RunTest2();
		//RunTest3();
		//RunTest4();
		RunTest5();
	}
	
	private void RunTest1() {
		System.out.println("Beginning test 1");
		
		// create new MEF file
		MEFWriter writer = new MEFWriter("/users/dan/Documents/workspace/output-test.mef", 
				1.0, 		/* one second per block */
				1000.0, 	/* 1000 Hz sampling frequency */ 
				100000); 	/* 1/10 second gap is discontinuity */
		
		// set MEF header fields
		writer.setInstitution("Mayo Clinic, Rochester, MN, USA");
		writer.setUnencryptedTextField("not entered");
		writer.setFirstName("Mickey");
		writer.setSecondName("M.");
		writer.setThirdName("Mouse");
		writer.setSubjectID("1-234-567");
		writer.setChannelName("channel name");
		writer.setLowFrequencyFilterSetting(100);
		writer.setHighFrequencyFilterSetting(9000);
		writer.setNotchFrequencyFilterSetting(500);
		writer.setVoltageConversionFactor(4);
		writer.setAcquisitionSystem("Neuralynx");
		writer.setChannelComments("channel comments");
		writer.setStudyComments("study comments");
		writer.setPhysicalChannelNumber(5);
		writer.setGMToffset((float)-6.0); //central standard time
		writer.setAnonymizedSubjectName("John Doe");
		// the following 2 statements are only necessary if you want multiple MEF files to
		// have the same session unique ID.  Otherwise a random ID is auto-generated for each file.
		byte[] random_bytes = writer.get8RandomBytes();
		writer.setSessionUniqueIdentifier(random_bytes);
		
		// add data samples to MEF file
		int[] samps = new int[10];
		long[] timestamps = new long[10];		
		
		samps[0] = -100;
		samps[1] = 1000;
		samps[2] = 1004;
		timestamps[0] = 1359315962000000L;  // Jan 27, 2013 19:46:02 GMT
		timestamps[1] = 1359315962001000L;
		timestamps[2] = 1359315962002000L;		
		writer.writeData(samps, timestamps, 3);
		
		samps[0] = 1010;
		samps[1] = 1014;
		timestamps[0] = 1359315962003000L;
		timestamps[1] = 1359315962004000L;
		writer.writeData(samps, timestamps, 2);
		
		// done adding samples, so close file and finalize header
		writer.close();
		
		System.out.println("Done with test 1");
	}
	
	private void RunTest2() {
		System.out.println("Beginning test 2");
		
		// create new MEF file
		MEFWriter writer = new MEFWriter("/users/dan/Documents/workspace/output-test-csc.mef", 
				1.0, 		/* one second per block */
				30303.0, 	/* 1000 Hz sampling frequency */ 
				100000); 	/* 1/10 second gap is discontinuity */
		
		// set MEF header fields
		writer.setInstitution("Mayo Clinic, Rochester, MN, USA");
		writer.setUnencryptedTextField("not entered");
		writer.setFirstName("Mickey");
		writer.setSecondName("M.");
		writer.setThirdName("Mouse");
		writer.setSubjectID("1-234-567");
		writer.setChannelName("channel name");
		writer.setLowFrequencyFilterSetting(100);
		writer.setHighFrequencyFilterSetting(9000);
		writer.setNotchFrequencyFilterSetting(500);
		writer.setVoltageConversionFactor(1);
		writer.setAcquisitionSystem("Neuralynx");
		writer.setChannelComments("channel comments");
		writer.setStudyComments("study comments");
		writer.setPhysicalChannelNumber(5);
		writer.setGMToffset((float)-6.0); //central standard time
		writer.setAnonymizedSubjectName("John Doe");
		// the following 2 statements are only necessary if you want multiple MEF files to
		// have the same session unique ID.  Otherwise a random ID is auto-generated for each file.
		byte[] random_bytes = writer.get8RandomBytes();
		writer.setSessionUniqueIdentifier(random_bytes);
		
		// add data samples to MEF file
		int[] samps = new int[512];
		long[] timestamps = new long[512];		
		
		// this info is in header, but hardcoded for simplicity
		long timestamp_basis = 1182549196491000L;  //Jun 22 2007 21:53:16 GMT
		double sampling_frequency = 30303;
		
		FileInputStream in_file = null;
		try {
			in_file = new FileInputStream("/users/dan/Documents/workspace/CSC3.Ncs");
		} catch (FileNotFoundException e) {
			System.out.println("Can't find .Ncs file");
		}
		
		int data_read = 1;
		int bytes_read;
		
		byte[] header_buffer = new byte[16384];
		byte[] buffer = new byte[1044];
		
		long sample_counter= 0;
		
		try {
			// skip header
			in_file.read(header_buffer);
			// read data blocks
			while (data_read == 1)
			{
				bytes_read = in_file.read(buffer);
				if (bytes_read < 1044)
					data_read = 0;
				if (data_read == 1)
				{
					// read timestamp
					timestamps[0] = 0;
					for (int i=0;i<8;i++)
					   timestamps[0] += ((long) buffer[i] & 0xffL) << (8 * i);
					timestamps[0] += timestamp_basis;
					
					//System.out.println(timestamps[0]);
					// extrapolate all timestamps in block
					for (int i=0;i<512;i++)
						timestamps[i] = timestamps[0] + (i * 33);  // each timestamp is 33 microsec, or 1/30303 seconds
					
					// read all samples from block
					// next 20 bytes in block are junk
					int offset = 20;
					for (int i=0;i<512;i++)
					{
						samps[i] = 0;
						for (int j=0;j<2;j++)
							samps[i] += ((int) buffer[j+offset] & 0xff) << (8 * j);
						// convert to signed
						if (samps[i] >= 32768)
							samps[i] -= 65536;
						
						offset += 2;
						//System.out.println(samps[i]);
					}
					
					sample_counter += 512;
					writer.writeData(samps, timestamps, 512);
				}	
			}
			
			System.out.println(sample_counter + " samples read from CSC file.");

		} catch (IOException e) {
			System.out.println("Can't read .Ncs file");
		}
		
		// done adding samples, so close file and finalize header
		writer.close();
		
		System.out.println("Done with test 2");
	}
	
	private void RunTest3() {
		System.out.println("Beginning test 3");

		// create new MEF file
		MEFWriter writer = new MEFWriter("/users/dan/Documents/workspace/output-test-18bit.mef", 
				1.0, 		/* one second per block */
				32000.0, 	/* 32k Hz sampling frequency */ 
				100000); 	/* 1/10 second gap is discontinuity */

		// set MEF header fields
		writer.setInstitution("Mayo Clinic, Rochester, MN, USA");
		writer.setUnencryptedTextField("not entered");
		writer.setFirstName("Mickey");
		writer.setSecondName("M.");
		writer.setThirdName("Mouse");
		writer.setSubjectID("1-234-567");
		writer.setChannelName("channel name");
		writer.setLowFrequencyFilterSetting(100);
		writer.setHighFrequencyFilterSetting(9000);
		writer.setNotchFrequencyFilterSetting(500);
		writer.setVoltageConversionFactor(4);
		writer.setAcquisitionSystem("Neuralynx");
		writer.setChannelComments("channel comments");
		writer.setStudyComments("study comments");
		writer.setPhysicalChannelNumber(5);
		writer.setGMToffset((float) -6.0); // central standard time
		writer.setAnonymizedSubjectName("John Doe");
		// the following 2 statements are only necessary if you want multiple
		// MEF files to
		// have the same session unique ID. Otherwise a random ID is
		// auto-generated for each file.
		byte[] random_bytes = writer.get8RandomBytes();
		writer.setSessionUniqueIdentifier(random_bytes);

		// add data samples to MEF file
		int[] samps = new int[10];
		long[] timestamps = new long[10];

		long timestamp_basis = 1359315962000000L; // Jan 27, 2013 19:46:02 GMT
		// 18-bit sample max and min
		int sample_min = -131072;
		int sample_max = 131071;

		try {
			PrintWriter out = new PrintWriter("/users/dan/Documents/workspace/output-test-18bit.txt");

			for (long l = 0; l < 100000000L; l++) {
				samps[0] = sample_min + (int) (Math.random() * ((sample_max - sample_min) + 1));
				// System.out.println(samps[0]);
				timestamps[0] = timestamp_basis + (long) (l * 31.25); // 31.25 microsec samples = 32k Hz
				writer.writeData(samps, timestamps, 1);
				out.println(samps[0]);
			}

			out.close();

		} catch (FileNotFoundException e) {
		}

		// done adding samples, so close file and finalize header
		writer.close();

		System.out.println("Done with test 3");
	}
	
	private void RunTest4() {
		System.out.println("Beginning test 4");

		// create new MEF file
		MEFWriter writer = new MEFWriter("/users/dan/Documents/workspace/output-test-18bit.mef", 
				1.0, 		/* one second per block */
				32000.0, 	/* 32k Hz sampling frequency */ 
				100000); 	/* 1/10 second gap is discontinuity */

		// set MEF header fields
		writer.setInstitution("Mayo Clinic, Rochester, MN, USA");
		writer.setUnencryptedTextField("not entered");
		writer.setFirstName("Mickey");
		writer.setSecondName("M.");
		writer.setThirdName("Mouse");
		writer.setSubjectID("1-234-567");
		writer.setChannelName("channel name");
		writer.setLowFrequencyFilterSetting(100);
		writer.setHighFrequencyFilterSetting(9000);
		writer.setNotchFrequencyFilterSetting(500);
		writer.setVoltageConversionFactor(4);
		writer.setAcquisitionSystem("Neuralynx");
		writer.setChannelComments("channel comments");
		writer.setStudyComments("study comments");
		writer.setPhysicalChannelNumber(5);
		writer.setGMToffset((float) -6.0); // central standard time
		writer.setAnonymizedSubjectName("John Doe");
		// the following 2 statements are only necessary if you want multiple
		// MEF files to
		// have the same session unique ID. Otherwise a random ID is
		// auto-generated for each file.
		byte[] random_bytes = writer.get8RandomBytes();
		writer.setSessionUniqueIdentifier(random_bytes);

		// add data samples to MEF file
		int[] samps = new int[10];
		long[] timestamps = new long[10];

		long timestamp_basis = 1359315962000000L; // Jan 27, 2013 19:46:02 GMT
		// 18-bit sample max and min
		int sample_min = -131072;
		int sample_max = 131071;
		samps[0] = 0;

		try {
			PrintWriter out = new PrintWriter("/users/dan/Documents/workspace/output-test-18bit.txt");

			for (long l = 0; l < 100000000L; l++) {
				//samps[0] = sample_min + (int) (Math.random() * ((sample_max - sample_min) + 1));
				if (samps[0] == 131071)
					samps[0] = -131072;
				else
					samps[0]++;
				// System.out.println(samps[0]);
				timestamps[0] = timestamp_basis + (long) (l * 31.25); // 31.25 microsec samples = 32k Hz
				writer.writeData(samps, timestamps, 1);
				out.println(samps[0]);
			}

			out.close();

		} catch (FileNotFoundException e) {
		}

		// done adding samples, so close file and finalize header
		writer.close();

		System.out.println("Done with test 4");
	}
	
	private void RunTest5() {
		System.out.println("Beginning test 5");
		
		// create new MEF file
		MEFWriter writer = new MEFWriter("/users/dan/Documents/workspace/output-test.mef", 
				1.0, 		/* one second per block */
				1000.0, 	/* 1000 Hz sampling frequency */ 
				100000); 	/* 1/10 second gap is discontinuity */
		
		// set MEF header fields
		/*writer.setInstitution("Mayo Clinic, Rochester, MN, USA");
		writer.setUnencryptedTextField("not entered");
		writer.setFirstName("Mickey");
		writer.setSecondName("M.");
		writer.setThirdName("Mouse");
		writer.setSubjectID("1-234-567");
		writer.setChannelName("channel name");
		writer.setLowFrequencyFilterSetting(100);
		writer.setHighFrequencyFilterSetting(9000);
		writer.setNotchFrequencyFilterSetting(500);
		writer.setVoltageConversionFactor(1);
		writer.setAcquisitionSystem("Neuralynx");
		writer.setChannelComments("channel comments");
		writer.setStudyComments("study comments");
		writer.setPhysicalChannelNumber(5);
		writer.setGMToffset((float)-6.0); //central standard time
		writer.setAnonymizedSubjectName("John Doe");
		// the following 2 statements are only necessary if you want multiple MEF files to
		// have the same session unique ID.  Otherwise a random ID is auto-generated for each file.
		byte[] random_bytes = writer.get8RandomBytes();
		writer.setSessionUniqueIdentifier(random_bytes);*/
		
		// add data samples to MEF file
		int[] samps = new int[10];
		long timestamp;		
		
		samps[0] = -100;
		samps[1] = 1000;
		samps[2] = 1004;
		timestamp = 1359315962000000L;  // Jan 27, 2013 19:46:02 GMT	
		writer.writeDataBlock(samps, timestamp, 3, false);
		
		samps[0] = 1010;
		samps[1] = 1014;
		timestamp = 1359315962003000L;
		writer.writeDataBlock(samps, timestamp, 2, false);
		
		samps[0] = 2000;
		timestamp = 1359315963000000L;
		writer.writeDataBlock(samps, timestamp, 1, true);
		
		// done adding samples, so close file and finalize header
		writer.close();
		
		System.out.println("Done with test 5");
	}

}
