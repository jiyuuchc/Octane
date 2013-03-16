//FILE:          GaussianFitting.java
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
public class GaussianFitting implements DifferentiableMultivariateRealFunction {
	
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
	
	private int windowSize_ = 2;

	protected boolean zeroBg_;
	
	/**
	 * Default constructor
	 */
	public GaussianFitting() {
		this(false);
	}
	
	/**
	 * Constructor.
	 *
	 * @param b whether the data is zero-background
	 */
	public GaussianFitting(boolean b) {
		zeroBg_ = b;
		if (!b) {
			parameters_ = new double[4];
		} else {
			parameters_ = new double[3];
		}
		parameters_[2] = defaultH_;
	
		gradients_ = new double[parameters_.length];
	}
	
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

	public int fitGaussianAt(double x, double y, double sigma, int size) {
		sigma2_ = sigma * sigma * 2;
		windowSize_ = size;
		x0_ = (int) x;
		y0_ = (int) y;
		parameters_[0] = x0_ + .5 - x;
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
		for (int xi = - windowSize_; xi <= windowSize_; xi++) {
			for (int yi = - windowSize_; yi <= windowSize_; yi++) {
				double v = pixelValue(x0_ + xi, y0_ + yi);
				m += v;
				m2 += v * v ;
			}
		}
		int nPixels = (1 + 2 * windowSize_)*(1 + 2 * windowSize_);
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
		for (int xi = - windowSize_; xi <= windowSize_; xi++) {
			for (int yi = - windowSize_; yi <= windowSize_; yi++) {
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
		for (int xi = - windowSize_; xi <= windowSize_; xi++) {
			for (int yi = - windowSize_; yi <= windowSize_; yi++) {
				double g = gauss(parameters_[0] + xi)* gauss(parameters_[1] + yi);
				imageData_[x0_ + xi + width_ * (y0_ + yi)] -= g * parameters_[2];
			}
		}
	}
	
	public double getX() {
		return (x0_ + .5 - parameters_[0]) ;
	}

	public double getY() {
		return (y0_ + .5 - parameters_[1]);
	}

	public double getHeight() {
		return parameters_[2];
	}

	public double getError() {
		return e_ ;
	}
}
