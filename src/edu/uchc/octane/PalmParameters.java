package edu.uchc.octane;

import ij.gui.GenericDialog;

import java.util.prefs.Preferences;

public class PalmParameters {
	final static String PALM_TYPE_KEY = "PalmType";
	final static String PALM_SCALE_FACTOR_KEY = "PalmRatio";
	final static String PALM_PSF_WIDTH_KEY = "PalmPSFWidth";
	final static String Z_RENDER_KEY = "ZRender";
	final static String Z_MAX_KEY = "ZMax";
	final static String Z_MIN_KEY = "ZMin";
	
	private static Preferences prefs_ = GlobalPrefs.getRoot().node(PalmParameters.class.getName());
	
	private static int palmType_ = prefs_.getInt(PALM_TYPE_KEY, 0);
	private static boolean zRenderMode_ = prefs_.getBoolean(Z_RENDER_KEY, false);
	
	static double palmScaleFactor_ = prefs_.getDouble(PALM_SCALE_FACTOR_KEY, 10.0);
	static double palmPSFWidth_ = prefs_.getDouble(PALM_PSF_WIDTH_KEY, 0.1875);
	static double zMax_ = prefs_.getDouble(Z_MAX_KEY, 3.0);
	static double zMin_ = prefs_.getDouble(Z_MIN_KEY, 0);
	
	private static Palm.PalmType [] typeList_ = {
			Palm.PalmType.AVERAGE,
			Palm.PalmType.HEAD,
			Palm.PalmType.TAIL,
			Palm.PalmType.ALLPOINTS,
			Palm.PalmType.TIMELAPSE
	};
	
	static public boolean openDialog() {
		if (prefs_ == null) {
			prefs_ = GlobalPrefs.getRoot().node(PalmParameters.class.getName());
		}

		GenericDialog dlg = new GenericDialog("Construct PALM");
		
		String[] strPalmTypes = { "Average", "Head", "Tail", "All Points", "Movie"};
		
		dlg.addChoice("PALM Type", strPalmTypes, strPalmTypes[palmType_]);
		dlg.addNumericField("Scale Factor", palmScaleFactor_, 0);
		dlg.addNumericField("PSF width", palmPSFWidth_, 3);
		
		dlg.addCheckbox("Render Z coordinates in pseudo-color", zRenderMode_);
		dlg.addNumericField("Min Z value (pixel)", zMin_, 3);
		dlg.addNumericField("Max Z value (pixel)", zMax_, 3);

		dlg.showDialog();
		if (dlg.wasCanceled())
			return false;

		palmType_ = dlg.getNextChoiceIndex();
		palmScaleFactor_ = dlg.getNextNumber();
		palmPSFWidth_ = dlg.getNextNumber();
		zRenderMode_ = dlg.getNextBoolean();
		zMin_ = dlg.getNextNumber();
		zMax_ = dlg.getNextNumber();
		
		if (zMin_ >= zMax_) {
			zRenderMode_ = false;
		}
		
		prefs_.putInt(PALM_TYPE_KEY, palmType_);
		prefs_.putBoolean(Z_RENDER_KEY, zRenderMode_);
		prefs_.putDouble(PALM_SCALE_FACTOR_KEY, palmScaleFactor_);
		prefs_.putDouble(PALM_PSF_WIDTH_KEY, palmPSFWidth_);
		prefs_.putDouble(Z_MIN_KEY, zMin_);
		prefs_.putDouble(Z_MAX_KEY, zMax_);

		return true;
		
	}
	
	static Palm.PalmType getPalmType() {
		return typeList_[palmType_];
	}

	static Palm.ZRenderMode getZRenderMode() {
		return zRenderMode_ ? Palm.ZRenderMode.COLOR : Palm.ZRenderMode.NONE;
	}
}
