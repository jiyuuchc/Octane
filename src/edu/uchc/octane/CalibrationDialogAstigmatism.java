//FILE:          CalibrationDialogAstigmatism.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 7/6/13
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

import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.fitting.PolynomialFitter;
import org.apache.commons.math3.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.ImageProcessor;

public class CalibrationDialogAstigmatism extends ParticleAnalysisDialogBase {
	
	double [] calibration_;
	
	int watershedNoise_;
	double sliceSpacing_;
	double resolution_;
	
	/**
	 * Constructor
	 * @param imp The image to be analyzed
	 */
	public CalibrationDialogAstigmatism(ImagePlus imp) {
		
		super(imp, "Astigmatism calibration parameters:" + imp.getTitle());
		
		calibration_ = new double[6];
	}
	
	@Override
	void setupDialog() {

		addNumericField("Pixel Size (nm)", 160, 0);
		addNumericField("Image resolution (nm)", 300, 0);
		addNumericField("Slice Spacing (nm)", 100, 0);

		addSlider("Noise Threshold", 1, 5000.0, 500);
		Vector<Scrollbar> sliders = (Vector<Scrollbar>)getSliders();
		sliders.get(0).setUnitIncrement(20); // default was 1
		
	}


	@Override
	public boolean updateParameters() {
		
		pixelSize_ = getNextNumber();
		resolution_ = getNextNumber();
		sliceSpacing_ = getNextNumber();
		
		if (sliceSpacing_ <= 0 || pixelSize_ <=0 || resolution_ <= 0) {
			return false;
		}

		watershedNoise_ = (int) getNextNumber();
		
		return true;
	}
	
	@Override
	public void processCurrentFrame(ImageProcessor ip, ParticleAnalysis module) throws InterruptedException {
		
		module.process(ip, rect_, 0, watershedNoise_);

	}

	/* (non-Javadoc)
	 * @see ij.gui.NonBlockingGenericDialog#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		if (wasOKed()) {
			
			Thread thread = new Thread() {
				
				@Override
				public void run() {
					
					doCalibration();

				}
			};
			
			thread.start();
		}
	} 
	
	void doCalibration() {
		
		final ImageStack stack = imp_.getImageStack();

		GaussianFitAstigmatism fittingModule = new GaussianFitAstigmatism();

		double sigma = resolution_ / pixelSize_ / 2.355;		
		int kernelSize = (int) Math.round(sigma * 2.5);
		
		fittingModule.setWindowSize(kernelSize);
		fittingModule.setPreprocessBackground(false);
		fittingModule.setDeflation(true);
		fittingModule.setPreferredSigmaValue(sigma);
		fittingModule.setCalibration(calibration_);

		double [] sigmax = new double[stack.getSize()];
		double [] sigmay = new double[stack.getSize()];

		// 	calculate average sigmaX and sigmaY for each slice
		for (int curFrame = 1; curFrame <= stack.getSize(); curFrame ++) {

			ImageProcessor ip = stack.getProcessor(curFrame);
			fittingModule.setImageData(ip);

			IJ.showProgress(curFrame, stack.getSize());
			
			ParticleAnalysis analysisModule = new ParticleAnalysis();

			try {
				analysisModule.process(ip, rect_, 0, watershedNoise_);
			} catch(InterruptedException e) {
				assert false;
			}

			int nParticles = analysisModule.reportNumParticles();

			if (nParticles <= 0) {
				IJ.error("No particles detected in frame: " + curFrame + "!");
				return;
			}

			double [] x = analysisModule.reportX();
			double [] y = analysisModule.reportY();

			int nFailed = 0;
			double sx = 0;
			double sy = 0;

			for (int i = 0; i < nParticles; i++) {

				fittingModule.setInitialCoordinates((int)x[i], (int)y[i]);
				
				if (fittingModule.fit() == null) {

					nFailed ++;

				} else {
				
					sx += fittingModule.getSigmaX();
					sy += fittingModule.getSigmaY();
				}
			}
			
			if (nParticles > nFailed) {
			
				sigmax[curFrame - 1] = sx / (nParticles - nFailed);
				sigmay[curFrame - 1] = sy / (nParticles - nFailed);
			
			} else {

				IJ.error("No particles detected in frame: " + curFrame + "!");
				return;
			
			}
		}
		
		//fitting sigmaX and sigmaY to parabolic functions
		
		double [] px;
		double [] py;
		
		try {
			
			px = parabolicFit(sigmax);
			py = parabolicFit(sigmay);
			
		} catch (MathIllegalStateException e) {
			
			IJ.error("Result cannot be modeled with parabolic funcitons.");
			return; 
		}
		
		px[0] -= px[1] * px[1] / 4 / px[2];  // a - b^2/4c
		py[0] -= py[1] * py[1] / 4 / py[2];
		
		px[1] = - px[1] / 2 / px[2]; // - b/2c
		py[1] = - py[1] / 2 / py[2];
		
		if (px[0] <= 0 || py[0] <= 0 || px[2] <= 0 || py[2] <= 0) {
			
			IJ.error("Parabolic fit resulting in illegal parameters");
			return;

		}
		
		//Display and save fitting results
		double [] z = new double[sigmax.length];
		double [] vx = new double[sigmax.length];
		double [] vy = new double[sigmax.length];
		
		for (int i = 0; i < sigmax.length; i ++) {
			
			z[i] = (i - sigmax.length / 2) * sliceSpacing_;
			vx[i] = parabolicValue(px, z[i]);
			vy[i] = parabolicValue(py, z[i]);
		}
		
		Plot plotWinX = new Plot("Astigmatism Calibration X", "Z (nm)", "Sigma", z, vx);
		Plot plotWinY = new Plot("Astigmatism Calibration Y", "Z (nm)", "Sigma", z, vy);
		plotWinX.addPoints(z, sigmax, Plot.BOX);
		plotWinY.addPoints(z, sigmay, Plot.BOX);

		plotWinX.show();
		plotWinY.show();		
		
		GlobalPrefs.calibrationStrX_ = String.format("%.3f, %.3f, %.3f", px[0], px[1], px[2]);
		GlobalPrefs.calibrationStrY_ = String.format("%.3f, %.3f, %.3f", py[0], py[1], py[2]);
		GlobalPrefs.savePrefs();
		
		IJ.log("Calibration results accepted.");
	}
	
	double [] parabolicFit(double [] sigma) {
		
		PolynomialFitter fitter = new PolynomialFitter(new LevenbergMarquardtOptimizer());
		
		for (int i = 0; i < sigma.length; i ++) {
			
			fitter.addObservedPoint((i - sigma.length / 2) * sliceSpacing_, sigma[i] );
		
		}
		
		double [] guess = new double[3];
		guess[0] = sigma[(int)(sigma.length  / 2)];
		guess[2] = (sigma[0] - guess[0]) / (sliceSpacing_ * sliceSpacing_ * sigma.length * sigma.length / 4);
		
		double [] results = fitter.fit(guess);
		
		return results;
	}
	
	double parabolicValue(double [] coeff, double x) {
		
		assert (coeff.length == 3);
		
		return coeff[0] + (x - coeff[1]) * (x - coeff[1]) * coeff[2];
	}
}
