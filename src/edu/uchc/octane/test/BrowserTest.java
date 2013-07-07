package edu.uchc.octane.test;

import java.io.File;
import java.io.IOException;

import ij.ImagePlus;

import edu.uchc.octane.OctaneWindowControl;
import edu.uchc.octane.ParticleAnalysisDialogBase;
import edu.uchc.octane.ParticleAnalysisDialog2D;
import edu.uchc.octane.SmNode;
import edu.uchc.octane.TrackingParameters;
import edu.uchc.octane.TrajDataset;

public class BrowserTest {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		ImagePlus imp = ij.IJ.openImage(args[0]);
		imp.show();
		//TrajDataset data = new TrajDataset(p);
		File file = new File(args[0]+".dataset");
		if (file.exists()) {
			OctaneWindowControl b = new OctaneWindowControl(imp);
			b.setup();
		} else {
			ParticleAnalysisDialogBase dlg = new ParticleAnalysisDialog2D(imp);
			dlg.showDialog();
			if (dlg.wasOKed()) {
				SmNode [][] nodes = dlg.processAllFrames();
				
				if ( TrackingParameters.openDialog() ) { //wasOKed ?

					TrajDataset data = TrajDataset.createDatasetFromNodes(nodes);
					OctaneWindowControl ctlr = new OctaneWindowControl(imp);
					ctlr.setup(data);
				}
			}
		}
	}
}
