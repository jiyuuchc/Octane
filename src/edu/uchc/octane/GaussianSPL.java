//FILE:          GaussianSPL.java
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

import java.util.Arrays;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.DifferentiableMultivariateRealFunction;
import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.apache.commons.math.analysis.MultivariateVectorialFunction;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.optimization.DifferentiableMultivariateRealOptimizer;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.direct.PowellOptimizer;

import ij.process.ImageProcessor;

/**
 * Subpixel refiner by Gaussian fit.
 */
public class GaussianSPL implements SubPixelLocalization, DifferentiableMultivariateRealFunction {
	
	private static final double defaultH_ = 200.0;
	double sigma2_ = 1.73;
	
	private int x0_,y0_;
	
	private double [] parameters_; 
	private double e_;
	private double bg_ = 0;
	private double [] gradients_;
	private double [] imageData_;
	private int width_ = 0;
	private int height_ = 0;
	
	protected boolean zeroBg_;
	
	/**
	 * Default constructor
	 */
	public GaussianSPL() {
		this(false);
	}
	
	/**
	 * Constructor.
	 *
	 * @param b whether the data is zero-background
	 */
	public GaussianSPL(boolean b) {
		zeroBg_ = b;
		if (!b) {
			parameters_ = new double[4];
		} else {
			parameters_ = new double[3];
		}
		parameters_[2] = defaultH_;
	
		gradients_ = new double[parameters_.length];
	}
	
	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#setImageData(ij.process.ImageProcessor)
	 */
	public void setImageData(ImageProcessor ip) {

		Object pixels = ip.getPixels();
		width_ = ip.getWidth();
		height_ = ip.getHeight();
		imageData_ = new double[width_ * height_];

		if (pixels instanceof byte[]) {
			byte[] b = (byte[])pixels;
			for (int i = 0; i < b.length; i++) {
				imageData_[i] = (double) (b[i] & 0xff);
			}
		} else if (pixels instanceof short[]) {
			short [] b = (short [])pixels;
			for (int i = 0; i < b.length; i++) {
				imageData_[i] = (double) (b[i] & 0xffff);
			}
		} else if (pixels instanceof float[]) {
			float [] b = (float [])pixels;
			for (int i = 0; i < b.length; i++) {
				imageData_[i] = (double) b[i];
			}
		} else if (pixels instanceof int[]) {
			int [] p = (int []) pixels;
			for (int i = 0; i < p.length; i++) {
				int r = (p[i]&0xff0000)>>16;
				int g = (p[i]&0xff00)>>8;
				int b = p[i]&0xff;
				imageData_[i] = 0.2126 * r + 0.7152 * g + 0.0722 * b;
			}
		}
		if (zeroBg_) {
			bg_ = 0;
		} else {
			bg_ = ip.getAutoThreshold();
		}
	}

	private double gauss(double x) {
		return Math.exp(- x*x/sigma2_);
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#refine(double, double)
	 */
	@Override
	public int refine(double x, double y) {
		//int w = 1 + 2 * Prefs.kernelSize_;
		sigma2_ = Prefs.sigma_ * Prefs.sigma_ * 2;
//		if (x < Prefs.kernelSize_) {
//			x0_ = Prefs.kernelSize_;
//		} else if (x >= ip_.getWidth() - Prefs.kernelSize_) {
//			x0_ = ip_.getWidth() - Prefs.kernelSize_ - 1;
//		} else { 
//			x0_ = (int) x;
//		}
		x0_ = (int) x;
		parameters_[0] = x0_ + .5 - x;

//		if (y < Prefs.kernelSize_) {
//			y0_ = Prefs.kernelSize_;
//		} else if (y >= ip_.getHeight() - Prefs.kernelSize_) {
//			y0_ = ip_.getHeight() - Prefs.kernelSize_ - 1;
//		} else { 
//			y0_ = (int) y;
//		}
		y0_ = (int) y;
		parameters_[1] = y0_ + .5 - y;

		try {
			fit();
		} catch (ConvergenceException e) {
			System.out.println("Gaussian fitting convergence error " + e.toString());
			return -1;
		} catch (Exception e) {
			System.err.println(e.getMessage() + e.toString());
			return -2;
		}

		// report
//		double hw = 0.5 + Prefs.kernelSize_;
//		for (int xi = - Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi++) {
//			for (int yi = - Prefs.kernelSize_; yi <= Prefs.kernelSize_; yi++) {
//				double xp = parameters_[0];
//				double yp = parameters_[1];
//				System.out.printf("%3d(%5f)\t", ip_.get(x0_ + xi, y0_ + yi), parameters_[2] * gauss(xp + xi) * gauss( yp + yi) + parameters_[3]);
//			}
//			System.out.println();
//		}
//		System.out.println();
		if (parameters_[2] < 0) {
			return -3;
		}

		return 0;
	}
	
	private double pixelValue(int x, int y) {
		return imageData_[x + y * width_];
	}
	
	void fit() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		DifferentiableMultivariateRealOptimizer gno = new PowellOptimizer();
		//gno.setMaxIterations(500);
		parameters_[2] = pixelValue(x0_ , y0_);
		if (!zeroBg_) {
			parameters_[3] = bg_;
		}
		RealPointValuePair vp = gno.optimize(this, GoalType.MINIMIZE, parameters_);
		parameters_ = vp.getPoint();
		
		double m = 0;
		double m2 = 0;
		for (int xi = - Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi++) {
			for (int yi = - Prefs.kernelSize_; yi <= Prefs.kernelSize_; yi++) {
				double v = pixelValue(x0_ + xi, y0_ + yi);
				m += v;
				m2 += v * v ;
			}
		}
		int nPixels = (1 + 2 * Prefs.kernelSize_)*(1 + 2 * Prefs.kernelSize_);
		m = m2 - m * m / nPixels; //variance of the grey values

		e_ = nPixels * Math.log(m/vp.getValue())  ; // a MLestimator. See Nature Methods 5,687 - 694 
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
		Arrays.fill(gradients_, 0);
		for (int xi = - Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi++) {
			for (int yi = - Prefs.kernelSize_; yi <= Prefs.kernelSize_; yi++) {
				double g = gauss(xp + xi)* gauss(yp + yi);
				double delta = bg + h*g - pixelValue(x0_ + xi , y0_ + yi);
				r += delta * delta;
				gradients_[0] += -4 * delta * h * g * (xp + xi)  / sigma2_ ;
				gradients_[1] += -4 * delta * h * g * (yp + yi)  / sigma2_ ; 
				gradients_[2] += 2 * delta * g;
				if (! zeroBg_) {
					gradients_[3] += 2 * delta;
				}
			}
		}
		return r;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.math.analysis.DifferentiableMultivariateRealFunction#partialDerivative(int)
	 */
	@Override
	public MultivariateRealFunction partialDerivative(int k) {
//		k_ = k;
//		return new MultivariateRealFunction() {
//			@Override
//			public double value(double[] point)
//					throws FunctionEvaluationException,
//					IllegalArgumentException {
//				return gradients_[k_];
//			}
//		};
		return null;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.math.analysis.DifferentiableMultivariateRealFunction#gradient()
	 */
	@Override
	public MultivariateVectorialFunction gradient() {
		return new MultivariateVectorialFunction() {
			@Override
			public double[] value(double[] point)
					throws FunctionEvaluationException,
					IllegalArgumentException {
				return gradients_;
			}
			
		};
	}

	public void deflate() {
		for (int xi = - Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi++) {
			for (int yi = - Prefs.kernelSize_; yi <= Prefs.kernelSize_; yi++) {
				double g = gauss(parameters_[0] + xi)* gauss(parameters_[1] + yi);
				imageData_[x0_ + xi + width_ * (y0_ + yi)] -= g * parameters_[2];
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getXOut()
	 */
	@Override
	public double getX() {
		return (x0_ + .5 - parameters_[0]) ;
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
	 * @see edu.uchc.octane.SubPixelRefiner#getConfidenceEstimator()
	 */
	@Override
	public double getError() {
		return e_ ;
	}
}
