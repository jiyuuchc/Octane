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

	public DeflationAnalysisDialog(ImagePlus imp) {
		super("Set parameters:" + imp.getTitle());

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
		addNumericField("Kernel Size (pixels)", Prefs.kernelSize_, 0);
		addNumericField("PSF sigma (pixels)", Prefs.sigma_, 2);
		addSlider("Intensity Threshold", 20, 40000.0, Prefs.deflationThreshold_);
		addCheckbox("Zero Background", Prefs.zeroBackground_);

		Vector<Scrollbar> sliders = (Vector<Scrollbar>)getSliders();
		sliders.get(0).setUnitIncrement(20); // default was 1 

		dialogListener_ = new DialogListener() {
			@Override
			public boolean dialogItemChanged(GenericDialog dlg, AWTEvent evt) {
				if (dlg == null) {
					return true;
				}
				
				Prefs.kernelSize_ = (int) getNextNumber();
				Prefs.sigma_ = getNextNumber();
				Prefs.deflationThreshold_ = (int) getNextNumber();
				Prefs.zeroBackground_ = (boolean) getNextBoolean();

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
			
			module_.process(ip, rect_, Prefs.kernelSize_, Prefs.sigma_, Prefs.deflationThreshold_, Prefs.zeroBackground_);
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

		module_.process(imp_.getProcessor(), rect_, Prefs.kernelSize_, Prefs.sigma_, Prefs.deflationThreshold_, Prefs.zeroBackground_);

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

}
