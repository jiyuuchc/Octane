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
		dlg.addMessage("- Tracking -");
		dlg.addNumericField("Max Displacement", Prefs.trackerMaxDsp_, 1);
		dlg.addNumericField("Max Blinking", (double)Prefs.trackerMaxBlinking_, 0);
		dlg.addNumericField("Confidence Threshold", Prefs.confidenceThreshold_, 0);

		dlg.showDialog();
		if (dlg.wasCanceled())
			return;
		Prefs.trackerMaxDsp_ = dlg.getNextNumber();
		Prefs.trackerMaxBlinking_ = (int) dlg.getNextNumber();
		Prefs.confidenceThreshold_ = dlg.getNextNumber();
	}
}
