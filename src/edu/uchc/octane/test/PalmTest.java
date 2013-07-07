package edu.uchc.octane.test;

import java.io.File;
import java.io.IOException;

import edu.uchc.octane.Palm;
import edu.uchc.octane.PalmParameters;
import edu.uchc.octane.TrajDataset;

import ij.ImagePlus;

public class PalmTest {
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		
		ImagePlus imp = ij.IJ.openImage(args[0]);
		imp.show();
		
		File file = new File(args[0]+".dataset");
		TrajDataset dataset = TrajDataset.loadDataset(file);
		
		Palm palmModule = new Palm(dataset);
		
		int[] selected = new int[dataset.getSize()];
		
		for (int i = 0; i < selected.length; i++) {
		
			selected[i] = i;
		}
			
		if (PalmParameters.openDialog(dataset, true)) {
			
			palmModule.constructPalm(imp, selected);
		
		}		
	}
	
}
