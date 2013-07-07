//FILE:          ParticleAnalysisDialog2D.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/16/13
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
 * Setting up parameters for 2D particle analysis using watershed/Gaussian fitting
 * @author Ji-Yu
 *
 */
public class ParticleAnalysisDialog2D extends ParticleAnalysisDialogBase {

	static Preferences prefs_ = null;

	int kernelSize_;
	double sigma_;
	boolean preProcessBackground_;
	int watershedThreshold_;
	int watershedNoise_;
	double resolution_;

	final private static String IMAGE_RESOLUTION = "imageResolution";
	final private static String ZERO_BACKGROUND_KEY = "zeroBackground";
	final private static String WATERSHED_THRESHOLD_KEY = "threshold";
	final private static String WATERSHED_NOISE_KEY = "noise";
	
	/**
	 * Constructor
	 * @param imp The image to be analyzed
	 */
	public ParticleAnalysisDialog2D(ImagePlus imp) {
		super(imp, "Watershed parameters:" + imp.getTitle());
	}

	/**
	 * Set up input fields of the dialog 
	 */
	@Override
	void setupDialog() { 

		if (prefs_ == null) {
			prefs_ = GlobalPrefs.getRoot().node(this.getClass().getName());
		}

		resolution_ =  prefs_.getDouble(IMAGE_RESOLUTION, 300);
		preProcessBackground_ = prefs_.getBoolean(ZERO_BACKGROUND_KEY, false);
		watershedThreshold_ = prefs_.getInt(WATERSHED_THRESHOLD_KEY, 100);
		watershedNoise_ = prefs_.getInt(WATERSHED_NOISE_KEY, 100);

		addNumericField("Pixel Size (nm)", pixelSize_, 0);
		addNumericField("Image Resolution (FWHM) (nm)", resolution_, 1);
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
		
		prefs_.putDouble(IMAGE_RESOLUTION, resolution_);
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
	public void processCurrentFrame(ImageProcessor ip, ParticleAnalysis module) throws InterruptedException {

		GaussianFit2D fittingModule = new GaussianFit2D();
		fittingModule.setWindowSize(kernelSize_);
		fittingModule.setPreprocessBackground(preProcessBackground_);
		fittingModule.setDeflation(true);
		fittingModule.setPreferredSigmaValue(sigma_);
		
		module.setGaussianFitModule(fittingModule);

		module.process(ip, rect_, watershedThreshold_, watershedNoise_);

	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.ParticleAnalysisDialog#updateParameters()
	 */
	@Override
	public boolean updateParameters() {
		
		pixelSize_ = getNextNumber();
		resolution_ = getNextNumber();
		
		sigma_ = resolution_ / 2.355 / pixelSize_;		
		
		if (sigma_ <=0 ) {
			return false;
		}
		
		kernelSize_ = (int) Math.round(sigma_ * 2.5);
		
		preProcessBackground_ = (boolean) getNextBoolean();
		
		watershedThreshold_ = (int) getNextNumber();
		watershedNoise_ = (int) getNextNumber();
		
		return true;
	}
}
