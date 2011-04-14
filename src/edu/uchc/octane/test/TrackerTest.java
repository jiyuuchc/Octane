package edu.uchc.octane.test;

import ij.ImagePlus;
import java.io.File;
import java.io.IOException;

import edu.uchc.octane.ThresholdDialog;
import edu.uchc.octane.TrajDataset;


public class TrackerTest {
	public static void main(String [] args) throws IOException {
		ImagePlus imp;
		String p= "C:\\Users\\Ji-Yu\\workspace\\Octane\\testdata\\sh2";

		imp = ij.IJ.openImage(p+"\\sh2_eos_egf_70min_0.avi");

		imp.show();

		ThresholdDialog finderDlg = new ThresholdDialog(imp);
		if (finderDlg.openDialog() == true) {
			TrajDataset d= new TrajDataset();
			d.setNodes(finderDlg.getProcessedNodes());
			d.buildTrajectoriesFromNodes();
			d.writeTrajectoriesToText(new File(p + "\\trajs"));
		}
	}
}
