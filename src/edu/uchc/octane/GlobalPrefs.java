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

import java.util.prefs.Preferences;

/**
 * The global preferences.
 */
public class GlobalPrefs {
	private static Preferences prefs_ = Preferences.userNodeForPackage(GlobalPrefs.class); 

	final static String PACKAGE_NAME = "Octane";
	
	final private static String SHOW_OVERLAY_KEY = "ShowOverlay";
	final private static String HISTOGRAM_BINS_KEY = "histogramBins";
	final private static String COMPENSATE_DRIFT_KEY = "compensateDrift";
	final private static String NUM_THREAD_KEY = "numThread";
	final private static String MSD_DELAY_KEY = "MsdDelay";
	final private static String NOTES_SCRIPT_KEY = "NotesScript";
	final private static String DEFAULT_PIXEL_SIZE_KEY = "deaultPixelSize";
	
	public static boolean showOverlay_ = prefs_.getBoolean(SHOW_OVERLAY_KEY, false);
	public static boolean compensateDrift_ = prefs_.getBoolean(COMPENSATE_DRIFT_KEY, false); 
	public static int histogramBins_ = prefs_.getInt(HISTOGRAM_BINS_KEY , 20);
	public static int nThread_ = prefs_.getInt(NUM_THREAD_KEY , 4);
	public static int msdDelay_ = prefs_.getInt(MSD_DELAY_KEY, 4);
	public static String notesScript_ = prefs_.get(NOTES_SCRIPT_KEY, "");
	public static double defaultPixelSize_ = Prefs.getDouble(DEFAULT_PIXEL_SIZE_KEY, 160);
	
	public static Preferences getRoot() {
		return prefs_;
	}

	/**
	 * Save preferences.
	 */
	public static void savePrefs() {
		prefs_.putBoolean(SHOW_OVERLAY_KEY, showOverlay_);
		prefs_.putBoolean(COMPENSATE_DRIFT_KEY, compensateDrift_);
		prefs_.putInt(HISTOGRAM_BINS_KEY, histogramBins_);
		prefs_.putInt(NUM_THREAD_KEY, nThread_);
		prefs_.putInt(MSD_DELAY_KEY, msdDelay_);
		prefs_.put(NOTES_SCRIPT_KEY, notesScript_);
		prefs_.putDouble(DEFAULT_PIXEL_SIZE_KEY, defaultPixelSize_);
	}
}
