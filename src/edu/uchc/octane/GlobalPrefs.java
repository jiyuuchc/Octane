//FILE:          GlobalPrefs.java
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

import ij.Prefs;
import ij.gui.GenericDialog;

import java.util.prefs.Preferences;

/**
 * The global preferences.
 */
public class GlobalPrefs {
	private static Preferences prefs_ = Preferences.userNodeForPackage(GlobalPrefs.class); 

	final static String PACKAGE_NAME = "Octane";
	final static String	VERSIONSTR = "1.6.0RC";
	
	final private static String NUM_THREAD_KEY = "numThread";
	final private static String DEFAULT_PIXEL_SIZE_KEY = "DeaultPixelSize";
	final private static String PARTICLE_ANALYSIS_MODE_KEY = "ParticleAnalysisMode";
	final private static String ASTIGMATISM_CALIBRATION_X_KEY = "astigmatismCalibrationX";
	final private static String ASTIGMATISM_CALIBRATION_Y_KEY = "astigmatismCalibrationY";
	final private static String ALTERNATING_OVERLAY_COLOR = "AlternatingOverlayColor";
	final private static String OVERLAY_STROKE_WIDTH = "OverlayStrokeWidth";
	
	public static int nThread_ = prefs_.getInt(NUM_THREAD_KEY , 4);
	public static double defaultPixelSize_ = Prefs.getDouble(DEFAULT_PIXEL_SIZE_KEY, 160);
	public static String particleAnalysisMode_ = Prefs.get(PARTICLE_ANALYSIS_MODE_KEY, "Accurate");
	public static String calibrationStrX_ = GlobalPrefs.getRoot().get(ASTIGMATISM_CALIBRATION_X_KEY, "0.8, 0, 0.18");
	public static String calibrationStrY_ = GlobalPrefs.getRoot().get(ASTIGMATISM_CALIBRATION_Y_KEY, "0.8, 6, 0.18");
	public static boolean alternatingOverlayColor_ = GlobalPrefs.getRoot().getBoolean(ALTERNATING_OVERLAY_COLOR, true);
	public static double overlayStrokeWidth_ = GlobalPrefs.getRoot().getDouble(OVERLAY_STROKE_WIDTH, 0.3);
	
	public static Preferences getRoot() {
		return prefs_;
	}

	/**
	 * Save preferences.
	 */
	public static void savePrefs() {
		
		prefs_.putInt(NUM_THREAD_KEY, nThread_);
		prefs_.putDouble(DEFAULT_PIXEL_SIZE_KEY, defaultPixelSize_);
		prefs_.put(PARTICLE_ANALYSIS_MODE_KEY, particleAnalysisMode_);
		prefs_.put(ASTIGMATISM_CALIBRATION_X_KEY, calibrationStrX_);
		prefs_.put(ASTIGMATISM_CALIBRATION_Y_KEY, calibrationStrY_);
	
	}
	
	/**
	 * Open global preference dialog
	 * 
	 *  @return  True if user pressed OK
	 */
	
	static public boolean openDialog(TrajDataset data, boolean b) {
		
		GenericDialog dlg = new GenericDialog("Octane Preferences"); 
		
		dlg.addNumericField("Numer of threads", nThread_, 0);
		dlg.addNumericField("Default Pixel Size", defaultPixelSize_, 2);
		
		dlg.addMessage("Overlay Options");
		dlg.addCheckbox("Alternating Trajectory Color", alternatingOverlayColor_);
		dlg.addNumericField("Overlay Stroke Width", overlayStrokeWidth_, 2);

		dlg.showDialog();
		
		if (dlg.wasCanceled()) {

			return false;
		}
		
		
		
		return true;
	}
}
