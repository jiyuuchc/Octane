//FILE:          PeakFinderDialog.java
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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.process.ImageProcessor;
import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.Button;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PeakFinderDialog extends GenericDialog {

	//private static final long serialVersionUID = 1L;
	private static final long serialVersionUID = -572636108979646529L;
	private PeakFinder finder_;
	private ImagePlus imp_;
	private String path_;
	ProcessStackThread processingThread_;

	class ProcessStackThread extends Thread {
		@Override
		public synchronized void run() {
			BufferedWriter writer = null;

			//String dir = workingDir_.getPath();
			assert (path_ != null);

			String fn = path_ + "analysis" + File.separator + "positions";
			// IJ.showMessage(fn);
			File file = new File(fn);
			int nFound = 0;
			int nMissed = 0;
			try {
				(new File(path_ + "analysis")).mkdirs();
				writer = new BufferedWriter(new FileWriter(file));
				ImageStack stack = imp_.getImageStack();
				for (int frame = 1; frame <= stack.getSize(); frame++) {
					IJ.showProgress(frame, stack.getSize());
					ImageProcessor ip = stack.getProcessor(frame);
					finder_.setImageProcessor(ip);
					nFound += finder_.findMaxima(imp_);
					nMissed += finder_.refineMaxima();
					finder_.saveMaxima(writer, frame);
				}
				writer.close();
			} catch (IOException e) {
				IJ.showMessage("IO error: " + e.getMessage());
			}
			IJ.log(imp_.getTitle() + "- Tested:" + nFound + " Missed:" + nMissed);
		}
	}

	public PeakFinderDialog(ImagePlus imp) {
		super(("SM Tracker:" + imp.getTitle()));
		imp_ = imp;
		finder_ = new PeakFinder();
		finder_.setRoi(imp_.getRoi());
		finder_.setImageProcessor(imp_.getProcessor());
		finder_.setThreshold(ImageProcessor.NO_THRESHOLD);

		FileInfo fi = imp_.getOriginalFileInfo();
		if (fi != null) {
			path_ = fi.directory + File.separator; 
		} else {
			IJ.showMessage("Can't find data location. Try to save the stack onto your hard drive first.");
			return;
		}

		addSlider("Noise Tolerance", 100.0, 5000.0, finder_.getTolerance());
		addDialogListener(new DialogListener() {

			public boolean dialogItemChanged(GenericDialog gd, AWTEvent ev) {
//				if (wasOKed()) {
//					setVisible(false);
//					processStack();
//				}
//				else {
					double tol = gd.getNextNumber();
					if (tol != finder_.getTolerance()) {
						finder_.setTolerance(tol);
						updateMaximum();
					}
//				}
				return true;
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// super.actionPerformed(e);
		setVisible(false);
		Button button = (Button) e.getSource(); // Hack, stupid gd does not expose any member
		if (button.getLabel() != "Cancel")
			processStack();
	}

	public void updateMaximum() {
		imp_.killRoi();
		finder_.findMaxima(imp_);
		//finder_.refineMaxima();
		finder_.markMaxima(imp_);
	}

	public void processStack() {
		imp_.killRoi();
		processingThread_ = new ProcessStackThread();
		processingThread_.start();
	}

	@Override
	public void showDialog() {
		updateMaximum();
		setModal(false);
		super.showDialog();
	}

	public void setImageProcessor(ImageProcessor ip) {
		if (isVisible()) {
			finder_.setImageProcessor(ip);
			updateMaximum();
		}
	}

	public boolean isProcessing() {
		return (processingThread_ != null && processingThread_.isAlive());
	}
}
