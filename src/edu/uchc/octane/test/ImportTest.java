package edu.uchc.octane.test;

import ij.IJ;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.JFileChooser;

import edu.uchc.octane.TrajDataset;

public class ImportTest {
	public static void main(String[] args) throws IOException {
		JFileChooser fc = new JFileChooser();
		if (fc.showOpenDialog(IJ.getApplet()) == JFileChooser.APPROVE_OPTION) {
			TrajDataset dataset = TrajDataset.importDatasetFromText(fc.getSelectedFile());
			dataset.writeTrajectoriesToText(new OutputStreamWriter(System.out));
		}
	}
}
