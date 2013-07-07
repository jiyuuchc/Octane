//FILE:          ParticleAnalysisDialogAstigmatism.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 7/6/13
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

import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.util.Vector;
import java.util.prefs.Preferences;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Setting up parameters for 3D particle analysis using astigmatism fitting
 * @author Ji-Yu
 *
 */
public class ParticleAnalysisDialogAstigmatism extends ParticleAnalysisDialogBase {
	static Preferences prefs_ = null;

	int kernelSize_;
	double sigma_;

	boolean preProcessBackground_;
	int watershedThreshold_;
	int watershedNoise_;

	double [] calibration_;

	final private static String ZERO_BACKGROUND_KEY = "zeroBackground";
	final private static String WATERSHED_THRESHOLD_KEY = "threshold";
	final private static String WATERSHED_NOISE_KEY = "noise";
	final private static String ASTIGMATISM_CALIBRATION_X_KEY = "astigmatismCalibrationX";
	final private static String ASTIGMATISM_CALIBRATION_Y_KEY = "astigmatismCalibrationY";

	/**
	 * Constructor
	 * @param imp The image to be analyzed
	 */
	public ParticleAnalysisDialogAstigmatism(ImagePlus imp) {

		super(imp, "Astigmatism analysis parameters:" + imp.getTitle());
		calibration_ = new double[6];

		String calibrationStr;
		calibrationStr = prefs_.get(ASTIGMATISM_CALIBRATION_X_KEY, "0.8, 0, 0.18");
		String [] substrs = calibrationStr.split(",");
		for (int i = 0; i < 3; i++) {
			calibration_[i] = Double.parseDouble(substrs[i]); 
		}

		calibrationStr = prefs_.get(ASTIGMATISM_CALIBRATION_Y_KEY, "0.8, 0, 0.18");
		substrs = calibrationStr.split(",");
		for (int i = 0; i < 3; i++) {
			calibration_[i + 3] = Double.parseDouble(substrs[i]); 
		}

		sigma_ = calibration_[0] - calibration_[1] * calibration_[1] / 4 / calibration_[2];		

		kernelSize_ = (int) Math.round(sigma_ * 2.5);
	}

	/**
	 * Set up input fields of the dialog 
	 */
	@Override
	void setupDialog() { 

		if (prefs_ == null) {
			prefs_ = GlobalPrefs.getRoot().node(this.getClass().getName());
		}

		preProcessBackground_ = prefs_.getBoolean(ZERO_BACKGROUND_KEY, false);
		watershedThreshold_ = prefs_.getInt(WATERSHED_THRESHOLD_KEY, 100);
		watershedNoise_ = prefs_.getInt(WATERSHED_NOISE_KEY, 100);
		
		addNumericField("Pixel Size (nm)", pixelSize_, 0);
		addCheckbox("Preprocess background", preProcessBackground_);
		
		addSlider("Intensity Threshold", 20, 40000.0, watershedThreshold_);
		addSlider("Noise Threshold", 1, 5000.0, watershedNoise_);
		
		Vector<Scrollbar> sliders = (Vector<Scrollbar>)getSliders();
		sliders.get(0).setUnitIncrement(20); // default was 1
		sliders.get(1).setUnitIncrement(20); // default was 1
	}

	/**
	 * Save parameters to persistent store
	 */
	public void savePrefs() {

		if (prefs_ == null) {
			return;
		}
		
		prefs_.putBoolean(ZERO_BACKGROUND_KEY, preProcessBackground_);
		prefs_.putInt(WATERSHED_THRESHOLD_KEY, watershedThreshold_);
		prefs_.putInt(WATERSHED_NOISE_KEY, watershedNoise_);
	}

	/* (non-Javadoc)
	 * @see ij.gui.NonBlockingGenericDialog#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		
		if (wasOKed()) {
			savePrefs();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.uchc.octane.ParticleAnalysisDialog#processCurrentFrame(ij.process.ImageProcessor)
	 */
	@Override
	public void processCurrentFrame(ImageProcessor ip, ParticleAnalysis analysisModule) throws InterruptedException {

		if (bProcessingAll_) {
			GaussianFitAstigmatism fittingModule = new GaussianFitAstigmatism();
			fittingModule.setWindowSize(kernelSize_);
			fittingModule.setPreprocessBackground(preProcessBackground_);
			fittingModule.setDeflation(true);
			fittingModule.setPreferredSigmaValue(sigma_);
			fittingModule.setCalibration(calibration_);

			analysisModule.setGaussianFitModule(fittingModule);
		} else {
			analysisModule.setGaussianFitModule(null);
		}
		analysisModule.process(ip, rect_, watershedThreshold_, watershedNoise_);

	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.ParticleAnalysisDialog#updateParameters()
	 */
	@Override
	public boolean updateParameters() {
		
		pixelSize_ = getNextNumber();
		
		preProcessBackground_ = (boolean) getNextBoolean();
	
		watershedThreshold_ = (int) getNextNumber();
		watershedNoise_ = (int) getNextNumber();
		
		return true;
	}
}
