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

import java.awt.AWTEvent;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;
import java.util.prefs.Preferences;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.PointRoi;
import ij.process.ImageProcessor;

public class DeflationAnalysisDialog extends NonBlockingGenericDialog {

	ImagePlus imp_;
	DeflationAnalysis module_;
	Rectangle rect_;
	
	WindowAdapter windowListener_;
	ImageListener imageListener_;
	DialogListener dialogListener_;
	
	private SmNode[][] nodes_; 

	Preferences prefs_ = null;
	int kernelSize_;
	double sigma_;
	boolean zeroBg_;
	int deflationThreshold_;

	final static String KERNELSIZE_KEY = "kernelSize";
	final static String SIGMA_KEY = "sigma";
	final static String ZERO_BACKGROUND_KEY = "zeroBackground";
	final static String DEFLATION_THRESHOLD= "deflationThreshold";
	
	public DeflationAnalysisDialog(ImagePlus imp) {
		super("Set parameters:" + imp.getTitle());

		prefs_ = Preferences.userNodeForPackage(getClass());
		prefs_ = prefs_.node(getClass().getName());

		kernelSize_ = prefs_.getInt(KERNELSIZE_KEY, 2);
		sigma_ = prefs_.getDouble(SIGMA_KEY, 0.8);
		zeroBg_ = prefs_.getBoolean(ZERO_BACKGROUND_KEY, false);
		deflationThreshold_ = prefs_.getInt(DEFLATION_THRESHOLD, 100);

		imp_ = imp;
		module_ = new DeflationAnalysis();
		rect_ = imp.getProcessor().getRoi();
		setupDialog();

		windowListener_ = new WindowAdapter() {
			@Override
			public void windowIconified(WindowEvent e) {
					setVisible(false);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				if (isDisplayable()) {
					setVisible(true);
				}
			}

			@Override
			public void windowClosed(WindowEvent e) {
				dispose();
			}
		};
		imp_.getWindow().addWindowListener(windowListener_);

		imageListener_ = new ImageListener() {

			@Override
			public void imageClosed(ImagePlus imp) {
				if (imp == imp_) {
					dispose();
				}
			}

			@Override
			public void imageOpened(ImagePlus imp) {
			}

			@Override
			public void imageUpdated(ImagePlus imp) {
				if (imp == imp_) {
					updateResults();
				}
			}
		};
		ImagePlus.addImageListener(imageListener_);
	}

	void setupDialog() {
		addNumericField("Kernel Size (pixels)", kernelSize_, 0);
		addNumericField("PSF sigma (pixels)", sigma_, 2);
		addSlider("Intensity Threshold", 20, 40000.0, deflationThreshold_);
		addCheckbox("Zero Background", zeroBg_);

		Vector<Scrollbar> sliders = (Vector<Scrollbar>)getSliders();
		sliders.get(0).setUnitIncrement(20); // default was 1 

		dialogListener_ = new DialogListener() {
			@Override
			public boolean dialogItemChanged(GenericDialog dlg, AWTEvent evt) {
				if (dlg == null) {
					return true;
				}
				
				if (wasOKed()) {
					savePrefs();
					return true;
				}

				kernelSize_ = (int) getNextNumber();
				sigma_ = getNextNumber();
				deflationThreshold_ = (int) getNextNumber();
				zeroBg_ = (boolean) getNextBoolean();

				updateResults();
				return true;
			}
		};

		addDialogListener(dialogListener_);
	}

	public SmNode[][] processAllFrames() {
		imp_.killRoi();
		IJ.log("Deflation Analysis -- Searching for particles:");

		ImageStack stack = imp_.getImageStack();
		
		nodes_ = new SmNode[stack.getSize()][];
		
		int nFound = 0;
		for (int frame = 1; frame <= stack.getSize(); frame++) {
			if ((frame % 50) == 0) {
				IJ.log("Processed: "+ frame + "frames.");
			}
			
			IJ.showProgress(frame, stack.getSize());
			
			ImageProcessor ip = stack.getProcessor(frame);
			
			module_.process(ip, rect_, kernelSize_, sigma_, deflationThreshold_, zeroBg_);
			int nParticles = module_.reportNumParticles();
			if (nParticles > 0) {
				SmNode [] nodesInFrame = new SmNode[nParticles];
				nodes_[frame - 1] =  nodesInFrame;
				double [] x = module_.reportX();
				double [] y = module_.reportY();
				double [] e = module_.reportE();
				double [] h = module_.reportH();
				for (int i = 0; i < nParticles; i++) {
					nodesInFrame[i] = new SmNode(x[i], y[i], 0d, frame, (int) h[i], e[i]);
				}
			} else {
				nodes_[frame - 1] =  null;
			}
			nFound += nParticles;
		}
		IJ.log(imp_.getTitle() + "- Found " + nFound + " particles.");
		
		return nodes_;
	}

	void updateResults() {
		if (imp_ == null) {
			return;
		}

		module_.process(imp_.getProcessor(), rect_, kernelSize_, sigma_, deflationThreshold_, zeroBg_);

		int nParticles = module_.reportNumParticles();
		if (nParticles <= 0) {
			imp_.killRoi();
			return;
		}
		PointRoi roi;

		if (nParticles > 0) {
			double [] x = module_.reportX();
			double [] y = module_.reportY();
			int [] xi = new int[nParticles];
			int [] yi = new int[nParticles];
			for (int i = 0; i < nParticles; i ++ ) {
				xi[i] = (int) x[i];
				yi[i] = (int) y[i];
			}
			roi = new PointRoi(xi, yi, nParticles);
			
			imp_.setRoi(roi);
		} else {
			return;
		}
	}

	@Override
	public void showDialog(){
		super.showDialog();
	}
	
	@Override 
	public void dispose() {
		ImagePlus.removeImageListener(imageListener_);
		imageListener_ = null;
		
		imp_.getWindow().removeWindowListener(windowListener_);
		windowListener_ = null;
		
		super.dispose();
	}
	
	public SmNode[][] getSmNodes() {
		return nodes_;
	}
	
	public void savePrefs() {
		if (prefs_ == null) {
			return;
		}
		
		prefs_.putInt(KERNELSIZE_KEY, kernelSize_);
		prefs_.putDouble(SIGMA_KEY, sigma_);
		prefs_.putBoolean(ZERO_BACKGROUND_KEY, zeroBg_);
		prefs_.putInt(DEFLATION_THRESHOLD, deflationThreshold_);
	}

}
