Java MEF writer
Written by Dan Crepeau, 3D Medical Imaging Inc., in conjunction with Mayo Systems Electrophysiology Lab

Usage governed by Apache 2.0 License, see license file or source headers
Example code in Tester.java

Class can be imported into Matlab and used as follows:

javaaddpath('/Volumes/asan/Source/java/DanMefWriter/version-1.5/MEF_writer.jar')
ts = 946729497000000:2000:946729497000000+10000*2000;
plot(ts)
data = round(1000*sin((ts-946729497000000)/100000));
plot(data)
mw = edu.mayo.msel.mefwriter.MefWriter('test.mef', 1.0, 500, 10000)
mw.writeData(data, ts, 10000)
mw.close


This example creates a sinusoidal signal of 10000 samples in length and writes the signal to a MEF file
