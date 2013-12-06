package edu.uchc.octane.test;

import edu.uchc.octane.CalibrationDialogAstigmatism;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ShortProcessor;

public class CalibrationTest {

	final static int size = 100;
	final static double sigmamin = 1;
	final static int intensity = 1000;
	final static int background = 100;  
	final static int noise = 10;  
	
	final static double zxmin = -500;
	final static double zymin = 500;
	final static double za = 4e-6;
	
	final static double zl = -1000;
	final static double zh = 1000;
	
	
	ImagePlus createCalibrationStack() {
		
		ImageStack stack = new ImageStack(size * 2 + 1, size * 2 + 1);

		for (double z = zl; z <= zh; z+= 100) {
			double sigmax = sigmamin + (z - zxmin) * (z - zxmin) * za;
			double sigmay = sigmamin + (z - zymin) * (z - zymin) * za;

			ShortProcessor ip = TestDataGenerator.astigmaticGaussianImage(
					size, // size 
					0, 
					0, 
					2 * sigmax * sigmax,
					2 * sigmay * sigmay,
					intensity, 
					background); // background
			
			TestDataGenerator.addNoise(ip, noise);
			stack.addSlice(ip);
		}

		
		ImagePlus imp = new ImagePlus("calibration", stack);
		return imp;
	}
	
	
	void doTest() {
		ImagePlus imp = createCalibrationStack();
		imp.show();
		
		CalibrationDialogAstigmatism dlg = new CalibrationDialogAstigmatism(imp);
		
		dlg.showDialog();
	}
	
	public static void main(String [] arg) {

		final CalibrationTest ct = new CalibrationTest();
		
		ct.doTest();		
	}
}

