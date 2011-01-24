package edu.uchc.octane.test;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import ij.ImagePlus;
import edu.uchc.octane.Browser;
//import edu.uchc.octane.TrajDataset;
import edu.uchc.octane.PeakFinderDialog;

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
			b.constructMobilityMap();
		} else {
			PeakFinderDialog finderDlg_ = new PeakFinderDialog(imp);
			finderDlg_.showDialog();
			finderDlg_.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					PeakFinderDialog d = (PeakFinderDialog) e.getSource(); 
					if (d.wasOKed()) {
						Browser b = new Browser(imp);
						b.setVisible(true);
					}
					d.removeWindowListener(this);
					super.windowClosed(e);
				}
		
			});
		}
	}
}
