package edu.uchc.octane;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class AnalysisDialog3DSimple extends AnalysisDialog2D {

	String calibrationStr_ = null; 
	double [] c_ = new double[3];

	final static String CALIBRATION_KEY = "calibrationString";

	public AnalysisDialog3DSimple(ImagePlus imp) {
		super(imp);
		
		setTitle("Simple 3D analysis");
	}

	@Override
	void setupDialog() {
		super.setupDialog();
		
		calibrationStr_ = prefs_.get(CALIBRATION_KEY, "0.8, 0, 0.18");
		addStringField("Z calibration: ", calibrationStr_);
	}

	public void savePrefs() {
		if (prefs_ == null) {
			return;
		}
		
		super.savePrefs();
		
		prefs_.put(CALIBRATION_KEY, calibrationStr_);
	}

	@Override
	public WatershedAnalysis processCurrentFrame(ImageProcessor ip) {
		
		WatershedAnalysis module = new WatershedAnalysis();
		
		GaussianFit3DSimple fittingModule = new GaussianFit3DSimple();
		
		fittingModule.setCalibrationValues(c_);
		
		module.setGaussianFitModule(fittingModule);
		
		module.setGaussianFitParameters(kernelSize_, sigma_, zeroBg_, true);
		module.process(ip, rect_, watershedThreshold_, watershedNoise_);

		return module;
	}

	@Override
	public boolean updateParameters() {
		super.updateParameters();
		
		calibrationStr_ = this.getNextString();
		
		return getCalibrationValues(calibrationStr_);
	}

	boolean getCalibrationValues(String str) {
		String [] substrs = str.split(",");
		if (substrs.length != 3) {
			return false;
		}
		
		for (int i = 0; i < 3; i++) {
			c_[i] = Double.parseDouble(substrs[i]);
		}
		
		GaussianFit3DSimple fittingModule = new GaussianFit3DSimple();

		try {  // test if the parameters were legit
			fittingModule.setCalibrationValues(c_) ;
		} catch(IllegalArgumentException e) {
			return false;
		}
		return true;
	}
}
