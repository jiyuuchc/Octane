//FILE:          PrefDialog.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 2/16/08
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES./**
//
package edu.uchc.octane;

import ij.gui.GenericDialog;

/**
 * The Preferences dialog .
 */
public class PrefDialog {
	
	/**
	 * Open dialog.
	 */
	static public void openDialog() {
		GenericDialog dlg = new GenericDialog("Options");
		dlg.addMessage("- Subpixel Fitting -");
		String [] choices = {"Polyfit Gaussian Weight","Gaussian Fit", "Zeor Background Gaussian"};
		dlg.addChoice("Algrithm", choices, choices[Prefs.refiner_]);
		dlg.addNumericField("Kernel Size", Prefs.kernelSize_, 0);
		dlg.addNumericField("PSD sigma", Prefs.sigma_, 2);

		dlg.addMessage("- Tracking -");
		dlg.addNumericField("Max Displacement", Prefs.trackerMaxDsp_, 1);
		dlg.addNumericField("Max Blinking", (double)Prefs.trackerMaxBlinking_, 0);
		dlg.addNumericField("Residue Threshold", Prefs.residueThreshold_, 1);

		dlg.addMessage("- Analysis -");
		dlg.addNumericField("PALM Ratio", Prefs.palmScaleFactor_, 1);
		dlg.addNumericField("PALM PSD sigma", Prefs.palmPSDWidth_, 3);
		dlg.addNumericField("PALM Threshold", Prefs.palmThreshold_, 1);		
		dlg.addNumericField("Histogram Bins", Prefs.histogramBins_, 0);

		dlg.showDialog();
		if (dlg.wasCanceled())
			return;
		Prefs.refiner_ = dlg.getNextChoiceIndex();
		Prefs.kernelSize_ = (int) dlg.getNextNumber();
		Prefs.sigma_ = dlg.getNextNumber();
		Prefs.trackerMaxDsp_ = dlg.getNextNumber();
		Prefs.trackerMaxBlinking_ = (int) dlg.getNextNumber();
		Prefs.residueThreshold_ = dlg.getNextNumber();
		Prefs.palmScaleFactor_ = dlg.getNextNumber();
		Prefs.palmPSDWidth_ = dlg.getNextNumber();
		Prefs.palmThreshold_ = dlg.getNextNumber();
		Prefs.histogramBins_ = (int) dlg.getNextNumber();
		Prefs.savePrefs();
	}
}
