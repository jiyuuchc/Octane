package edu.uchc.octane.test;

import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import edu.uchc.octane.SmNode;
import edu.uchc.octane.ThresholdDialog;
import edu.uchc.octane.TrajDataset;


public class TrackerTest {
	final static String p = "testdata"; // test data
	final static String path = p + "/cell1.avi";
	
	public static void main(String [] args) throws IOException {
		ImagePlus imp;
		imp = ij.IJ.openImage(path);

		imp.show();

		ThresholdDialog dlg = new ThresholdDialog(imp);
		if (dlg.openDialog() == true) {
			TrajDataset d= TrajDataset.createDatasetFromNodes(dlg.getProcessedNodes());
			d.writeTrajectoriesToText(new OutputStreamWriter(System.out));
		}
		imp.close();
	}
}
