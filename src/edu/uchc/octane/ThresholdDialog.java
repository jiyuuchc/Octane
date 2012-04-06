//FILE:          ThresholdDialog.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 2/16/08
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
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
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
import ij.process.ImageProcessor;

/**
 * The dialog to set peak detection threshold.
 */
public class ThresholdDialog implements ImageListener {
	
	ImagePlus imp_;
	
	protected PeakFinder finder_;
	
	protected NonBlockingGenericDialog dlg_;

	private SmNode[][] nodes_; 
	
	/**
	 * Instantiates a new dialog.
	 *
	 * @param imp the ImageJ image
	 */
	public ThresholdDialog(ImagePlus imp) {
		imp_ = imp;

		finder_ = new PeakFinder();
		finder_.setRoi(imp_.getRoi());
		finder_.setImageProcessor(imp_.getProcessor());
		
		dlg_ = new NonBlockingGenericDialog("Set Threshold:" + imp.getTitle());
	}

	/**
	 * Open dialog.
	 *
	 * @return true, if OKed
	 */
	public boolean openDialog() {
		final String [] choices = {"None", "Polyfit Gaussian Weight","Gaussian Fit", "Zeor Background Gaussian"};
		dlg_.addChoice("Algrithm", choices, choices[Prefs.refiner_]);
		dlg_.addNumericField("Kernel Size", Prefs.kernelSize_, 0);
		dlg_.addNumericField("PSD sigma", Prefs.sigma_, 2);
		
		finder_.setTolerance(Prefs.peakTolerance_);
		dlg_.addSlider("Intensity Threshold", 0, 40000.0, finder_.getTolerance());

		finder_.setLaplaceTolerance(0);
		dlg_.addSlider("Laplace Threshold", 0, 100, finder_.getLaplaceTolerance()*10);

		Vector<Scrollbar> sliders = (Vector<Scrollbar>)dlg_.getSliders();
		//final Scrollbar slider = sliders.get(0);
		sliders.get(0).setUnitIncrement(20); // default was 1 
		
		dlg_.addDialogListener(new DialogListener() {
			@Override
			public boolean dialogItemChanged(GenericDialog gd, AWTEvent ev) {
				if (gd.wasOKed() || finder_ == null || ev == null || gd == null) {
					return true;
				}
				double tol = -1;
				double laplaceTol = -1;
				Prefs.refiner_ = dlg_.getNextChoiceIndex();
				Prefs.kernelSize_ = (int) dlg_.getNextNumber();
				Prefs.sigma_ = dlg_.getNextNumber();
				
				tol = gd.getNextNumber();
				laplaceTol = gd.getNextNumber() / 10.0;
				if (tol >= 0 && tol != finder_.getTolerance()) {
					finder_.setTolerance(tol);
					updateMaximum();
				}
				if (laplaceTol >= 0 && laplaceTol != finder_.getLaplaceTolerance() ) {
					finder_.setLaplaceTolerance(laplaceTol);
					updateMaximum();
				}
				return true;	
			}
		});

		imp_.getWindow().addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(WindowEvent e) {
					dlg_.setVisible(false);
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
				if (dlg_.isDisplayable())
					dlg_.setVisible(true);
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
				//dlg_.notify();
				dlg_.dispose();
			}
		});

		ImagePlus.addImageListener(this);
		updateMaximum();
		dlg_.showDialog();
		ImagePlus.removeImageListener(this);
		if (dlg_.wasOKed()) {
			Prefs.savePrefs();
			return processStack();
		} else {
			return false;
		}
	}

	protected void updateMaximum() {
		imp_.killRoi();
		finder_.findMaxima();
		imp_.setRoi(finder_.getMaximaAsROI());
	}

	protected boolean processStack() {
		imp_.killRoi();
		int nFound = 0;
		int nMissed = 0;
		IJ.log(imp_.getTitle() + ": Detecting particles ...");

		ImageStack stack = imp_.getImageStack();
		nodes_ = new SmNode[stack.getSize()][];
		for (int frame = 1; frame <= stack.getSize(); frame++) {
			if ((frame % 50) == 0) {
				IJ.log("Processed: "+ frame + "frames.");
			}
			IJ.showProgress(frame, stack.getSize());
			ImageProcessor ip = stack.getProcessor(frame);
			finder_.setImageProcessor(ip);
			nFound += finder_.findMaxima();
			nMissed += finder_.refineMaxima();
			//finder_.exportCurrentMaxima(writer, frame);
			nodes_[frame - 1] = finder_.getMaximaAsSMNodes(frame);
		}

		IJ.log(imp_.getTitle() + "- Tested:" + nFound + " Missed:" + nMissed);

		return true;
	}

	/**
	 * Sets the image data.
	 *
	 * @param ip the new image data
	 */
	public void setImageProcessor(ImageProcessor ip) {
		if (dlg_.isVisible()) {
			finder_.setImageProcessor(ip);
			updateMaximum();
		}
	}

	/**
	 * Gets the processed nodes.
	 *
	 * @return the processed nodes.
	 */
	public SmNode[][] getProcessedNodes() {
		return nodes_;
	}

	/* (non-Javadoc)
	 * @see ij.ImageListener#imageOpened(ij.ImagePlus)
	 */
	@Override
	public void imageOpened(ImagePlus imp) {
	}

	/* (non-Javadoc)
	 * @see ij.ImageListener#imageClosed(ij.ImagePlus)
	 */
	@Override
	public void imageClosed(ImagePlus imp) {
	}

	/* (non-Javadoc)
	 * @see ij.ImageListener#imageUpdated(ij.ImagePlus)
	 */
	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp == imp_) {
			if ( dlg_.isVisible() ) {
				finder_.setImageProcessor(imp.getProcessor());
				updateMaximum();				
			}
		}
	}

}
