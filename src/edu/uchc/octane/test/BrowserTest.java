package edu.uchc.octane.test;

import java.io.File;
import java.io.IOException;

import ij.ImagePlus;
import edu.uchc.octane.Browser;
import edu.uchc.octane.ThresholdDialog;

public class BrowserTest {
	public static ImagePlus imp;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String p= "C:\\Users\\Ji-Yu\\workspace\\Octane\\testdata\\sh2";
		imp = ij.IJ.openImage(p+"\\sh2_eos_egf_70min_0.avi");
		imp.show();
		//TrajDataset data = new TrajDataset(p);
		File file = new File(p + File.separator + imp.getTitle()+".dataset");
		if (file.exists()) {
			Browser b = new Browser(imp);
			b.setup();
		} else {
			ThresholdDialog finderDlg = new ThresholdDialog(imp);
			if (finderDlg.openDialog() == true) {
				Browser b= new Browser(imp);
				b.setup(finderDlg.getProcessedNodes());
			} 
		}
	}
}
