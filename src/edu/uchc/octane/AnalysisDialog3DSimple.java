//FILE:          AnalysisDialog3DSimple.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/20/13
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

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Setting up parameters for simple 3D particle analysis.
 * This class use GaussianFit3DSimple to calculate Z coordinates of the particles
 * 
 * @author Ji-Yu
 *
 */
public class AnalysisDialog3DSimple extends AnalysisDialog2D {

	private String calibrationStr_ = null; 
	private double [] c_ = new double[3];

	final private static String CALIBRATION_KEY = "calibrationString";

	/**
	 * constructor
	 * @param imp The Image to be analyzed
	 */
	public AnalysisDialog3DSimple(ImagePlus imp) {
		super(imp);
		
		setTitle("Simple 3D analysis");
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.AnalysisDialog2D#setupDialog()
	 */
	@Override
	void setupDialog() {
		super.setupDialog();
		
		calibrationStr_ = prefs_.get(CALIBRATION_KEY, "0.8, 0, 0.18");
		addStringField("Z calibration: ", calibrationStr_);
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.AnalysisDialog2D#savePrefs()
	 */
	@Override
	public void savePrefs() {
		if (prefs_ == null) {
			return;
		}
		
		super.savePrefs();
		
		prefs_.put(CALIBRATION_KEY, calibrationStr_);
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.AnalysisDialog2D#processCurrentFrame(ij.process.ImageProcessor)
	 */
	@Override
	public WatershedAnalysis processCurrentFrame(ImageProcessor ip) throws InterruptedException {
		
		WatershedAnalysis module = new WatershedAnalysis();
		
		GaussianFit3DSimple fittingModule = new GaussianFit3DSimple();
		
		fittingModule.setCalibrationValues(c_);
		
		module.setGaussianFitModule(fittingModule);
		
		module.setGaussianFitParameters(kernelSize_, sigma_, preProcessBackground_, true);
		module.process(ip, rect_, watershedThreshold_, watershedNoise_);

		return module;
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.AnalysisDialog2D#updateParameters()
	 */
	@Override
	public boolean updateParameters() {
		if (!super.updateParameters()) {
			return false;
		}
		
		calibrationStr_ = this.getNextString();
		
		return getCalibrationValues(calibrationStr_);
	}

	/**
	 * Analyze the calibration string input from the dialog
	 * @param str
	 * @return
	 */
	boolean getCalibrationValues(String str) {
		String [] substrs = str.split(",");
		if (substrs.length != 3) {
			return false;
		}
		
		for (int i = 0; i < 3; i++) {
			try {
				c_[i] = Double.parseDouble(substrs[i]);
			} catch (NumberFormatException e) {
				return false;
			}
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
