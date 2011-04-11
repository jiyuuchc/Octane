package edu.uchc.octane.test;

import java.io.File;
import ij.ImagePlus;
import edu.uchc.octane.Browser;
import edu.uchc.octane.ThresholdDialog;

public class BrowserTest {
	public static ImagePlus imp;
	
	public static void main(String[] args) {

		String p= "C:\\Users\\Ji-Yu\\workspace\\Octane\\testdata\\eosactin-DIV8_1";
		imp = ij.IJ.openImage(p+"\\eosactin.tif");
		imp.show();
		//TrajDataset data = new TrajDataset(p);
		File file = new File(p + File.separator + "analysis" + File.separator + "dataset");
		if (file.exists()) {
			Browser b = new Browser(imp);
			b.setVisible(true);
			//b.constructMobilityMap();
		} else {
			ThresholdDialog finderDlg = new ThresholdDialog(imp);
			if (finderDlg.openDialog() == true) {
				Browser b = new Browser(imp);
				b.setVisible(true);
			} 
		}
	}
}
