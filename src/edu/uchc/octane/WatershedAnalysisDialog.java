package edu.uchc.octane;

import java.awt.AWTEvent;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.util.Vector;
import java.util.prefs.Preferences;

import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;

public class WatershedAnalysisDialog extends ParticleAnalysisDialog {

	DialogListener dialogListener_;
	
	static Preferences prefs_ = null;

	int kernelSize_;
	double sigma_;
	boolean zeroBg_;
	int watershedThreshold_;
	int watershedNoise_;

	final static String KERNELSIZE_KEY = "kernelSize";
	final static String SIGMA_KEY = "sigma";
	final static String ZERO_BACKGROUND_KEY = "zeroBackground";
	final static String WATERSHED_THRESHOLD_KEY = "threshold";
	final static String WATERSHED_NOISE_KEY = "noise";
	
	public WatershedAnalysisDialog(ImagePlus imp) {
		super(imp, "Watershed parameters:" + imp.getTitle());

		if (prefs_ == null) {
			prefs_ = GlobalPrefs.getRoot().node(WatershedAnalysisDialog.class.getName());
		}
		kernelSize_ = prefs_.getInt(KERNELSIZE_KEY, 2);
		sigma_ = prefs_.getDouble(SIGMA_KEY, 0.8);
		zeroBg_ = prefs_.getBoolean(ZERO_BACKGROUND_KEY, false);
		watershedThreshold_ = prefs_.getInt(WATERSHED_THRESHOLD_KEY, 100);
		watershedNoise_ = prefs_.getInt(WATERSHED_NOISE_KEY, 100);
		
		setupDialog();

	}

	void setupDialog() {
		
		addNumericField("Kernel Size (pixels)", kernelSize_, 0);
		addNumericField("PSF sigma (pixels)", sigma_, 2);
		addCheckbox("Zero Background", zeroBg_);
		
		addSlider("Intensity Threshold", 20, 40000.0, watershedThreshold_);
		addSlider("Noise Threshold", 1, 5000.0, watershedNoise_);
		

		Vector<Scrollbar> sliders = (Vector<Scrollbar>)getSliders();
		sliders.get(0).setUnitIncrement(20); // default was 1
		sliders.get(1).setUnitIncrement(20); // default was 1

		dialogListener_ = new DialogListener() {
			@Override
			public boolean dialogItemChanged(GenericDialog dlg, AWTEvent evt) {
				if (dlg == null) {
					return true;
				}

				kernelSize_ = (int) getNextNumber();
				sigma_ = getNextNumber();
				zeroBg_ = (boolean) getNextBoolean();
				
				watershedThreshold_ = (int) getNextNumber();
				watershedNoise_ = (int) getNextNumber();

				updateResults();

				return true;
			}
		};

		addDialogListener(dialogListener_);
	}

	public void savePrefs() {
		if (prefs_ == null) {
			return;
		}
		
		prefs_.putInt(KERNELSIZE_KEY, kernelSize_);
		prefs_.putDouble(SIGMA_KEY, sigma_);
		prefs_.putBoolean(ZERO_BACKGROUND_KEY, zeroBg_);
		prefs_.putInt(WATERSHED_THRESHOLD_KEY, watershedThreshold_);
		prefs_.putInt(WATERSHED_NOISE_KEY, watershedNoise_);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		
		if (wasOKed()) {
			savePrefs();
		}
	}
	
	@Override
	public WatershedAnalysis processCurrentFrame(ImageProcessor ip) {
		
		WatershedAnalysis module = new WatershedAnalysis();  
		module.setGaussianFitParameters(kernelSize_, sigma_, zeroBg_, true);
		module.process(ip, rect_, watershedThreshold_, watershedNoise_);
		return module;
	}

}
