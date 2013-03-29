//FILE:          DeflationAnalysisDialog.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/30/13
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

import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.util.Vector;
import java.util.prefs.Preferences;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Dialog to specify parameters for DeflationAnalysis 
 * @author Ji-Yu
 *
 */
public class DeflationAnalysisDialog extends ParticleAnalysisDialog {

	static Preferences prefs_ = null;
	int kernelSize_;
	double sigma_;
	boolean zeroBg_;
	int deflationThreshold_;

	final private static String KERNELSIZE_KEY = "kernelSize";
	final private static String SIGMA_KEY = "sigma";
	final private static String ZERO_BACKGROUND_KEY = "zeroBackground";
	final private static String DEFLATION_THRESHOLD= "deflationThreshold";
	
	/**
	 * Constructor
	 * @param imp The data to be analyzed
	 */
	public DeflationAnalysisDialog(ImagePlus imp) {
		super(imp, "Deflation parameters:" + imp.getTitle());

		if ( prefs_ == null ) {
			prefs_ = GlobalPrefs.getRoot().node(DeflationAnalysisDialog.class.getName());
		}
		kernelSize_ = prefs_.getInt(KERNELSIZE_KEY, 2);
		sigma_ = prefs_.getDouble(SIGMA_KEY, 0.8);
		zeroBg_ = prefs_.getBoolean(ZERO_BACKGROUND_KEY, false);
		deflationThreshold_ = prefs_.getInt(DEFLATION_THRESHOLD, 100);
		
		setupDialog();

	}

	/**
	 * Set up input fields of the dialog
	 */
	void setupDialog() {
		addNumericField("Kernel Size (pixels)", kernelSize_, 0);
		addNumericField("PSF sigma (pixels)", sigma_, 2);
		addSlider("Intensity Threshold", 20, 40000.0, deflationThreshold_);
		addCheckbox("Zero Background", zeroBg_);

		Vector<Scrollbar> sliders = (Vector<Scrollbar>)getSliders();
		sliders.get(0).setUnitIncrement(20); // default was 1 
	}

	/**
	 * Save parameters to persistent store
	 */
	public void savePrefs() {
		if (prefs_ == null) {
			return;
		}
		
		prefs_.putInt(KERNELSIZE_KEY, kernelSize_);
		prefs_.putDouble(SIGMA_KEY, sigma_);
		prefs_.putBoolean(ZERO_BACKGROUND_KEY, zeroBg_);
		prefs_.putInt(DEFLATION_THRESHOLD, deflationThreshold_);
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
	public DeflationAnalysis processCurrentFrame(ImageProcessor ip) {
		
		DeflationAnalysis module = new DeflationAnalysis();  
		
		module.process(ip, rect_, kernelSize_, sigma_, deflationThreshold_, zeroBg_);
		
		return module;
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.ParticleAnalysisDialog#updateParameters()
	 */
	@Override
	public boolean updateParameters() {
		kernelSize_ = (int) getNextNumber();
		sigma_ = getNextNumber();
		deflationThreshold_ = (int) getNextNumber();
		zeroBg_ = (boolean) getNextBoolean();
		
		return true;
		
	}
}
