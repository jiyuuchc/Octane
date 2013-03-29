//FILE:          PalmParameters.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/1/13
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
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//
package edu.uchc.octane;

import ij.gui.GenericDialog;

import java.util.prefs.Preferences;

/**
 * Parameters for PALM plot
 * @author Ji-Yu
 *
 */
public class PalmParameters {

	final private static String PALM_TYPE_KEY = "PalmType";
	final private static String PALM_SCALE_FACTOR_KEY = "PalmRatio";
	final private static String PALM_PSF_WIDTH_KEY = "PalmPSFWidth";
	final private static String RENDER_IN_COLOR_KEY = "RenderInColor";
	final private static String LUT_MAX_KEY = "LutMax";
	final private static String LUT_MIN_KEY = "LutMin";
	final private static String Z_BOTTOM_KEY = "ZBottom";
	final private static String Z_TOP_KEY = "ZTop";
	final private static String Z_SIGMA_KEY = "ZSigma";
	
	private static boolean bRenderStack_;
	
	private static Preferences prefs_ = GlobalPrefs.getRoot().node(PalmParameters.class.getName());
	
	private static int palmType_ = prefs_.getInt(PALM_TYPE_KEY, 0);
	private static boolean bRenderInColor_ = prefs_.getBoolean(RENDER_IN_COLOR_KEY, false);
	
	static double palmScaleFactor_ = prefs_.getDouble(PALM_SCALE_FACTOR_KEY, 10.0);
	static double palmPSFWidth_ = prefs_.getDouble(PALM_PSF_WIDTH_KEY, 0.1875);
	static double lutMax_ = prefs_.getDouble(LUT_MAX_KEY, 3.0);
	static double lutMin_ = prefs_.getDouble(LUT_MIN_KEY, 0);
	static double zBottom_ = prefs_.getDouble(Z_BOTTOM_KEY, 0);
	static double zTop_ = prefs_.getDouble(Z_TOP_KEY, 3);
	static double zSigma_ = prefs_.getDouble(Z_SIGMA_KEY, 1.6);
	
	private static Palm.PalmType [] typeList_ = {
			Palm.PalmType.AVERAGE,
			Palm.PalmType.HEAD,
			Palm.PalmType.TAIL,
			Palm.PalmType.ALLPOINTS,
			Palm.PalmType.TIMELAPSE
	};
	
	/**
	 * Opens a dialog to input parameters 
	 * @param b Whether this is to generate a 3D PALM stack
	 * @return True if user pressed OK
	 */
	static public boolean openDialog(boolean b) {
		if (prefs_ == null) {
			prefs_ = GlobalPrefs.getRoot().node(PalmParameters.class.getName());
		}

		bRenderStack_ = b;
		
		GenericDialog dlg = b ? new GenericDialog("Construct PALM Stack") : new GenericDialog("Construct PALM");
		
		String[] strPalmTypes = { "Average", "Head", "Tail", "All Points", "Movie"};
		String[] strPalmTypes2 = { "Average", "Head", "Tail", "All Points"};
		
		if (b) {
			if (palmType_ >= strPalmTypes2.length) {
				palmType_ = 0;
			}
			dlg.addChoice("PALM Type", strPalmTypes2, strPalmTypes[palmType_]);
		} else {
			dlg.addChoice("PALM Type", strPalmTypes, strPalmTypes[palmType_]);
		}
		
		dlg.addNumericField("Scale Factor", palmScaleFactor_, 0);
		dlg.addNumericField("PSF width", palmPSFWidth_, 3);
		
		dlg.addMessage("- Color Parameters -");
		dlg.addCheckbox("Render Z coordinates in pseudo-color", bRenderInColor_);
		dlg.addNumericField("Min Z value (pixel)", lutMin_, 3);
		dlg.addNumericField("Max Z value (pixel)", lutMax_, 3);

		if (b) {
			dlg.addMessage("- Stack Parameters -");
			dlg.addNumericField("Z of bottom slice (pixel)", zBottom_, 3);
			dlg.addNumericField("Z of top Slice (pixel)", zTop_, 3);
			dlg.addNumericField("PSF Sigma_Z (pixel)", zSigma_, 3);
		}
		
		dlg.showDialog();
		if (dlg.wasCanceled())
			return false;

		palmType_ = dlg.getNextChoiceIndex();
		palmScaleFactor_ = dlg.getNextNumber();
		palmPSFWidth_ = dlg.getNextNumber();
		bRenderInColor_ = dlg.getNextBoolean();
		lutMin_ = dlg.getNextNumber();
		lutMax_ = dlg.getNextNumber();
		
		if (lutMin_ >= lutMax_) {
			bRenderInColor_ = false;
		}
		
		if (b) {
			zBottom_ = dlg.getNextNumber();
			zTop_ = dlg.getNextNumber();
			zSigma_ = dlg.getNextNumber();
		}

		prefs_.putInt(PALM_TYPE_KEY, palmType_);
		prefs_.putBoolean(RENDER_IN_COLOR_KEY, bRenderInColor_);
		prefs_.putDouble(PALM_SCALE_FACTOR_KEY, palmScaleFactor_);
		prefs_.putDouble(PALM_PSF_WIDTH_KEY, palmPSFWidth_);
		prefs_.putDouble(LUT_MIN_KEY, lutMin_);
		prefs_.putDouble(LUT_MAX_KEY, lutMax_);
		prefs_.putDouble(Z_BOTTOM_KEY, zBottom_);
		prefs_.putDouble(Z_TOP_KEY, zTop_);
		prefs_.putDouble(Z_SIGMA_KEY, zSigma_);
		
		return true;
		
	}
	
	/**
	 * Get Palm type
	 * @return Palm Type specified by user
	 */
	static Palm.PalmType getPalmType() {
		return typeList_[palmType_];
	}
	
	/**
	 * Get whether to render in color
	 * @return Whether to render in color
	 */
	static boolean isRenderInColor() {
		return bRenderInColor_;
	}

	/**
	 * Get whether to render 3d PALM stack
	 * @return Whether to render 3d PALM stack
	 */
	static boolean isRenderStack() {
		return bRenderStack_;
	}

}
