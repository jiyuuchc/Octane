//FILE:          ParticleAnalysisDialog.java
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

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.PointRoi;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.Rectangle;

public abstract class ParticleAnalysisDialog extends NonBlockingGenericDialog {
	ImagePlus imp_;
	Rectangle rect_;
	
	ImageListener imageListener_;
	DialogListener dialogListener_;

	private SmNode[][] nodes_ = null;  
	
	Integer lastFrame_;
	Integer nFound_;
	
	public ParticleAnalysisDialog(ImagePlus imp, String title) {
		super(title);

		imp_ = imp;
		rect_ = imp.getProcessor().getRoi();

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
		
		dialogListener_ = new DialogListener() {
			@Override
			public boolean dialogItemChanged(GenericDialog dlg, AWTEvent evt) {
				if (dlg == null) {
					return true;
				}
				
				if (updateParameters() == true) {
					updateResults();
					return true;
				} else {
					return false;
				}
			}
		};

		addDialogListener(dialogListener_);
	}

	public SmNode[][] processAllFrames() {
		imp_.killRoi();
		IJ.log("Particle Analysis -- Searching for particles:");

		final ImageStack stack = imp_.getImageStack();
		
		nodes_ = new SmNode[stack.getSize()][];
		
		lastFrame_ = 0;
		nFound_ = 0;

		class ProcessThread extends Thread {

			public void run() {
				int curFrame = 1;
				do {
					synchronized(lastFrame_) {
						if (lastFrame_ < stack.getSize()) {
							lastFrame_ ++;
							curFrame = lastFrame_;
						} else {
							return;
						}
					}
					if ((curFrame % 50) == 0) {
						IJ.log("Processed: "+ curFrame + "frames.");
					}
					
					IJ.showProgress(curFrame, stack.getSize());
					
					ImageProcessor ip = stack.getProcessor(curFrame);
					
					ParticleAnalysis module = processCurrentFrame(ip);
					
					nodes_[curFrame - 1] = module.createSmNodes(curFrame);

					int nParticles = module.reportNumParticles();
					
					synchronized(nFound_) {
						nFound_ += nParticles;
					}

				} while(curFrame < stack.getSize());
			}
		}
		
		ProcessThread [] threads = new ProcessThread[GlobalPrefs.nThread_];
		
		for (int i = 0; i < GlobalPrefs.nThread_; i++ ) {
			threads[i] =  new ProcessThread();
			threads[i].start();
		}
		
		for (int i = 0; i < GlobalPrefs.nThread_; i++ ) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
			}
		}
		
		IJ.log(imp_.getTitle() + "- Found " + nFound_ + " particles.");
		
		return nodes_;
	}

	void updateResults() {
		if (imp_ == null) {
			return;
		}

		ParticleAnalysis module = processCurrentFrame(imp_.getProcessor());
		
		int nParticles = module.reportNumParticles();
		if (nParticles <= 0) {
			imp_.killRoi();
			return;
		}
		
		PointRoi roi;

		if (nParticles > 0) {
			double [] x = module.reportX();
			double [] y = module.reportY();
			int [] xi = new int[nParticles];
			int [] yi = new int[nParticles];
			for (int i = 0; i < nParticles; i ++ ) {
				xi[i] = (int) x[i];
				yi[i] = (int) y[i];
			}
			roi = new PointRoi(xi, yi, nParticles);
			
			imp_.setRoi(roi);
		} 
	}

	@Override 
	public void dispose() {
		if (imageListener_ != null) {
			ImagePlus.removeImageListener(imageListener_);
			imageListener_ = null;
		}	
		super.dispose();
	}
	
	public SmNode[][] getSmNodes() {
		return nodes_;
	}	
	
	abstract public ParticleAnalysis processCurrentFrame(ImageProcessor ip);
	abstract public boolean updateParameters();
}
