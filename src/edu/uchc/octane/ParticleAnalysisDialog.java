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
import ij.gui.NonBlockingGenericDialog;
import ij.gui.PointRoi;
import ij.process.ImageProcessor;

import java.awt.Rectangle;

public abstract class ParticleAnalysisDialog extends NonBlockingGenericDialog {
	ImagePlus imp_;
	Rectangle rect_;
	
	ImageListener imageListener_;
	
	ParticleAnalysis module_ = null;
	private SmNode[][] nodes_ = null;  
	
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
	}

	public SmNode[][] processAllFrames() {
		imp_.killRoi();
		IJ.log("Particle Analysis -- Searching for particles:");

		ImageStack stack = imp_.getImageStack();
		
		nodes_ = new SmNode[stack.getSize()][];
		
		int nFound = 0;
		for (int frame = 1; frame <= stack.getSize(); frame++) {
			if ((frame % 50) == 0) {
				IJ.log("Processed: "+ frame + "frames.");
			}
			
			IJ.showProgress(frame, stack.getSize());
			
			ImageProcessor ip = stack.getProcessor(frame);
			
			processCurrentFrame(ip);
			
			nodes_[frame - 1] = module_.createSmNodes(frame);
			int nParticles = module_.reportNumParticles();
			nFound += nParticles;
		}
		IJ.log(imp_.getTitle() + "- Found " + nFound + " particles.");
		
		return nodes_;
	}

	void updateResults() {
		if (imp_ == null) {
			return;
		}

		processCurrentFrame(imp_.getProcessor());
		
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
	
	abstract public void processCurrentFrame(ImageProcessor ip);
}
