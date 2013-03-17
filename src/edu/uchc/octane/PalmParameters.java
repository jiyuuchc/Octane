package edu.uchc.octane;

import ij.gui.GenericDialog;

import java.util.prefs.Preferences;

public class PalmParameters {
	final static String PALM_TYPE_KEY = "PalmType";
	final static String PALM_SCALE_FACTOR_KEY = "PalmRatio";
	final static String PALM_PSF_WIDTH_KEY = "PalmPSFWidth";
	
	private static Preferences prefs_ = GlobalPrefs.getRoot().node(PalmParameters.class.getName());
	public static int palmType_ = prefs_.getInt(PALM_TYPE_KEY, 1);
	public static double palmScaleFactor_ = prefs_.getDouble(PALM_SCALE_FACTOR_KEY, 10.0);
	public static double palmPSFWidth_ = prefs_.getDouble(PALM_PSF_WIDTH_KEY, 0.1875);
	
	private static Palm.PalmType [] typeList_ = {
			Palm.PalmType.AVERAGE,
			Palm.PalmType.HEAD,
			Palm.PalmType.TAIL,
			Palm.PalmType.ALLPOINTS,
			Palm.PalmType.STACK
	};

	static public boolean openDialog() {
		if (prefs_ == null) {
			prefs_ = GlobalPrefs.getRoot().node(PalmParameters.class.getName());
		}

		GenericDialog dlg = new GenericDialog("Construct PALM");
		String[] items = { "Average", "Head", "Tail", "All Points", "Stack"};
		dlg.addChoice("PALM Type", items, items[palmType_]);
		dlg.addNumericField("Scale Factor", palmScaleFactor_, 0);
		dlg.addNumericField("PSF width", palmPSFWidth_, 3);

		dlg.showDialog();
		if (dlg.wasCanceled())
			return false;

		palmType_ = dlg.getNextChoiceIndex();
		palmScaleFactor_ = dlg.getNextNumber();
		palmPSFWidth_ = dlg.getNextNumber();
		
		prefs_.putInt(PALM_TYPE_KEY, palmType_);
		prefs_.putDouble(PALM_SCALE_FACTOR_KEY, palmScaleFactor_);
		prefs_.putDouble(PALM_PSF_WIDTH_KEY, palmPSFWidth_);

		return true;
		
	}
	
	static Palm.PalmType getPalmType() {
		return typeList_[palmType_];
	}

}
