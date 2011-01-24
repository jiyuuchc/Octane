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
//import ij.io.FileInfo;
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
	private PeakFinder finder_ = null;
	private ImagePlus imp_ = null;
	private String path_ = null;
	private boolean wasOKed_ = false;
	
	public PeakFinderDialog(ImagePlus imp) {
		super("Set Threshold:" + imp.getTitle());
		imp_ = imp;
		finder_ = new PeakFinder();
		finder_.setRoi(imp_.getRoi());
		finder_.setImageProcessor(imp_.getProcessor());
		finder_.setThreshold(ImageProcessor.NO_THRESHOLD);

		addSlider("Threshold", 1000.0, 40000.0, finder_.getTolerance());
		addDialogListener(new DialogListener() {

			public boolean dialogItemChanged(GenericDialog gd, AWTEvent ev) {
				double tol = gd.getNextNumber();
				if (tol != finder_.getTolerance()) {
					finder_.setTolerance(tol);
					updateMaximum();
				}
				return true;
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Button [] buttons = getButtons();
		setVisible(false);
		if (e.getSource() == buttons[0]) { //OK button
			processStack();
			wasOKed_ = true; 
		}
	}

	@Override
	public boolean wasOKed() {
		return wasOKed_;
	}
	
	public void updateMaximum() {
		imp_.killRoi();
		finder_.findMaxima(imp_);
		finder_.markMaxima(imp_);
	}

	public void processStack() {
		Thread thread = new Thread() {
			public void run() {
				imp_.killRoi();
				BufferedWriter writer = null;
				
				FileInfo fi = imp_.getOriginalFileInfo();
				path_ = fi.directory;
				String fn = path_ + "analysis" + File.separator + "positions";
				File file = new File(fn);
				int nFound = 0;
				int nMissed = 0;
				IJ.log(imp_.getTitle() + ": Processing");
				try {
					(new File(path_ + "analysis")).mkdirs();
					writer = new BufferedWriter(new FileWriter(file));
					ImageStack stack = imp_.getImageStack();
					for (int frame = 1; frame <= stack.getSize(); frame++) {
						if ((frame % 50) == 0) {
							IJ.log("Processed: "+ frame + "frames.");
						}
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
				dispose();
			}
		};
		
		thread.start();
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
	
	public ImagePlus getImp(){
		return imp_;
	}
	
}
