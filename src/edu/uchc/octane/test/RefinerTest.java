package edu.uchc.octane.test;

import java.io.IOException;
import java.io.OutputStreamWriter;

import edu.uchc.octane.GaussianRefiner;
import edu.uchc.octane.PFGWRefiner2;
import edu.uchc.octane.PeakFinder;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class RefinerTest {
	final static String p = "C:\\Users\\Ji-Yu\\workspace\\Octane\\testdata\\sh2"; // test data
	final static String path = p + "\\sh2_eos_egf_70min_0.avi";
	
	public static void main(String[] args) throws IOException {

		ImagePlus imp;
		imp = ij.IJ.openImage(path);
		//imp.show();

		PeakFinder finder = new PeakFinder();
		finder.setTolerance(30);
		finder.setRefiner(new PFGWRefiner2(imp.getProcessor()));

		ImageStack stack = imp.getImageStack();

		OutputStreamWriter writer = new OutputStreamWriter(System.out);
		for (int frame = 1; frame <= stack.getSize(); frame++) {
			ImageProcessor ip = stack.getProcessor(frame);
			finder.setImageProcessor(ip);
			finder.findMaxima();
			finder.refineMaxima();
			finder.exportCurrentMaxima(writer,frame);
		}
	}
}
