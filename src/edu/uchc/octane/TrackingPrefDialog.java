//FILE:          TrackingPrefDialog.java
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

import java.util.prefs.Preferences;

import ij.gui.GenericDialog;

/**
 * The Preferences dialog .
 */
public class TrackingPrefDialog {

	final static String MAX_BLINKING_KEY = "trackerMaxBlinking";
	final static String MAX_DISPLACEMENT_KEY = "trackerMaxDsp";
	final static String ERROR_THRESHOLD_KEY = "errorThreshold";
	
	public static double trackerMaxDsp_ = 5;
	public static int trackerMaxBlinking_ = 0;
	public static double errorThreshold_ = 0;

	private static Preferences prefs_ = null;

	/**
	 * Open dialog.
	 */
	static public void openDialog() {
		if (prefs_ == null) {
			prefs_ = Preferences.systemNodeForPackage(TrackingPrefDialog.class);
			prefs_ = prefs_.node(TrackingPrefDialog.class.getName());
		}
		
		GenericDialog dlg = new GenericDialog("Tracking Options");
		dlg.addMessage("- Tracking -");
		dlg.addNumericField("Max Displacement", trackerMaxDsp_, 1);
		dlg.addNumericField("Max Blinking", (double)trackerMaxBlinking_, 0);
		dlg.addNumericField("Confidence Threshold", errorThreshold_, 0);

		dlg.showDialog();
		if (dlg.wasCanceled())
			return;
		
		trackerMaxDsp_ = dlg.getNextNumber();
		trackerMaxBlinking_ = (int) dlg.getNextNumber();
		errorThreshold_ = dlg.getNextNumber();
		
		prefs_.putInt(MAX_BLINKING_KEY,trackerMaxBlinking_);
		prefs_.putDouble(MAX_DISPLACEMENT_KEY, trackerMaxDsp_);
		prefs_.putDouble(ERROR_THRESHOLD_KEY , errorThreshold_);
	}
}
