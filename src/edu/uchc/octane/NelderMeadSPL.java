//FILE:          NelderMeadResolver.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/30/11
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

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.direct.NelderMead;

import ij.process.ImageProcessor;

/**
 * A refiner using NelderMead fitting algrithm.
 */
public class NelderMeadSPL implements SubPixelLocalization, MultivariateRealFunction {
		
	//private static final double defaultH_ = 200.0;
	private double sigma2_ = 1.73;

	//int blocks_;
	private int x0_,y0_;
	
	ImageProcessor ip_;
	
	private double [] parameters_; 
	private double residue_;
	private double bg_ ;
	
	protected boolean zeroBg_;
	
	/**
	 * Default constructor.
	 */
	public NelderMeadSPL() {
		this(false);
	}
	
	/**
	 * Constructor.
	 *
	 * @param b Is zero-background
	 */
	public NelderMeadSPL(boolean b) {
		zeroBg_ = b;
		if (b) 
			parameters_ = new double[3];
		else
			parameters_ = new double[4];
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#setImageData(ij.process.ImageProcessor)
	 */
	public void setImageData(ImageProcessor ip){
		ip_ = ip;
		bg_ = ip.getAutoThreshold();		
	}

	double gauss(double x) {
		return Math.exp(- x*x/sigma2_);
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#refine(double, double)
	 */
	@Override
	public int refine(double x, double y) {
		//int w = 1 + 2 * Prefs.kernelSize_;
		sigma2_ = 2 * Prefs.sigma_ * Prefs.sigma_;
		if (x < Prefs.kernelSize_) {
			x0_ = Prefs.kernelSize_;
		} else if (x >= ip_.getWidth() - Prefs.kernelSize_) {
			x0_ = ip_.getWidth() - Prefs.kernelSize_ - 1;
		} else { 
			x0_ = (int) x;
		}
		parameters_[0] = x0_ + .5 - x;

		if (y < Prefs.kernelSize_) {
			y0_ = Prefs.kernelSize_;
		} else if (y >= ip_.getHeight() - Prefs.kernelSize_) {
			y0_ = ip_.getHeight() - Prefs.kernelSize_ - 1;
		} else { 
			y0_ = (int) y;
		}
		parameters_[1] = y0_ + .5 - y;

		if (! zeroBg_)
			parameters_[3] = bg_;

		try {
			fit();
		} catch (ConvergenceException e) {
			return -1;
		} catch (Exception e) {
//			System.out.println(e.getMessage() + e.toString());
			return -2;
		}

		// report
//		double hw = 0.5 + Prefs.kernelSize_;
//		for (int xi = - Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi++) {
//			for (int yi = - Prefs.kernelSize_; yi <= Prefs.kernelSize_; yi++) {
//				double xp = Math.sin(parameters_[0]) * hw;
//				double yp = Math.sin(parameters_[1]) * hw;
//				System.out.printf("%3d(%5f)\t", ip_.get(x0_ + xi, y0_ + yi), parameters_[2] * gauss(xp + xi) * gauss( yp + yi) + parameters_[3]);
//			}
//			System.out.println();
//		}
//		System.out.println();
		
		return 0;
	}
	
	void fit() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		NelderMead nm = new NelderMead();
		parameters_[2] = ip_.get(x0_, y0_) - bg_;

		RealPointValuePair vp = nm.optimize(this, GoalType.MINIMIZE, parameters_);
		parameters_ = vp.getPoint();
		//residue_ = vp.getValue(); // normalized to H^2
		
		double m = 0;
		double m2 = 0;
		for (int xi = - Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi++) {
			for (int yi = - Prefs.kernelSize_; yi <= Prefs.kernelSize_; yi++) {
				m += ip_.get(x0_+xi, y0_+yi);
				m2 += (ip_.get(x0_+xi, y0_+yi))*(ip_.get(x0_+xi, y0_+yi));
			}
		}
		int nPixels = (1 + 2 * Prefs.kernelSize_)*(1 + 2 * Prefs.kernelSize_);
		m = m2 - m * m / nPixels; //variance of the grey values
		residue_ = nPixels * Math.log(m/vp.getValue());
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getXOut()
	 */
	@Override
	public double getX() {
		return (x0_ + .5 - parameters_[0]);
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getYOut()
	 */
	@Override
	public double getY() {
		return (y0_ + .5 - parameters_[1]);
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getHeightOut()
	 */
	@Override
	public double getHeight() {
		return parameters_[2];
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getResidue()
	 */
	@Override
	public double getConfidenceEstimator() {
		
		return residue_ ;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.math.analysis.MultivariateRealFunction#value(double[])
	 */
	@Override
	public double value(double[] p) throws FunctionEvaluationException,IllegalArgumentException {
		double xp = p[0];
		double yp = p[1];
		double h = p[2];
		double bg = zeroBg_?0:p[3];
		double r = 0;
		if (h < 0) {
			return 1e10;
		}
		
		for (int xi = - Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi++) {
			for (int yi = - Prefs.kernelSize_; yi <= Prefs.kernelSize_; yi++) {
				double g = gauss(xp + xi)* gauss(yp + yi);
				double v = (double)(ip_.get(x0_ + xi , y0_ + yi)) - bg - h*g;
				r += v * v;
			}
		}

		return r;
	}
}
