package edu.uchc.octane.test;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import ij.ImagePlus;
import edu.uchc.octane.Browser;
//import edu.uchc.octane.TrajDataset;

public class BrowserTest {

	public static void main(String[] args) {
		ImagePlus imp;
		String p= "C:\\Users\\Ji-Yu\\workspace\\Octane\\testdata\\eosactin-DIV8_1";
		imp = ij.IJ.openImage(p+"\\eosactin.tif");
		imp.show();
		//TrajDataset data = new TrajDataset(p);
		Browser b = new Browser(imp);
//		b.addWindowListener(new WindowAdapter() {
//			public void windowClosing(WindowEvent e) {
//				System.exit(0);
//			}
//		});
		b.setVisible(true);
	}
}
