//FILE:          GaussianRefiner.java
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
import org.apache.commons.math.analysis.DifferentiableMultivariateVectorialFunction;
import org.apache.commons.math.analysis.MultivariateMatrixFunction;
import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.apache.commons.math.analysis.MultivariateVectorialFunction;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.optimization.DifferentiableMultivariateRealOptimizer;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.VectorialPointValuePair;
import org.apache.commons.math.optimization.direct.PowellOptimizer;
import org.apache.commons.math.optimization.general.ConjugateGradientFormula;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math.optimization.general.NonLinearConjugateGradientOptimizer;

import ij.process.ImageProcessor;

/**
 * Subpixel refiner by Gaussian fit.
 */
public class GaussianResolver implements SubPixelResolver, DifferentiableMultivariateRealFunction {
	
	static final int kernelSize_ = 3;
	
	static final double defaultH_ = 200.0;
	
	static final double sigma2_ = 1.73;
	
	//static double [] gaussLookupTable_;
	
//	static {
//		gaussLookupTable_ = new double[(kernelSize_ * 2 + 1) * 100];
//		//IJ.log("starting building index...");
//		for (int i = 0 ; i < (kernelSize_ * 2 + 1) * 100; i ++ ) {
//			gaussLookupTable_[i] = Math.exp(- (i * i) / (sigma2_ * 10000));
//		}
//		//IJ.log("Finishing building index...");
//	}
	
	int x0_,y0_;
	
	ImageProcessor ip_;
	
	double [] parameters_; 
	
	double residue_;
	
	double bg_ = 1700.0;

	double [] gradients_;
	
	
	int k_;

	boolean zeroBg_;
	
	double x_in, y_in; 
	
	/**
	 * Default constructor assuming nonzero background.
	 */
	public GaussianResolver() {
		this(false);
	}
	
	/**
	 * Constructor.
	 *
	 * @param b whether the data is zero-background
	 */
	public GaussianResolver(boolean b) {
		zeroBg_ = b;
		if (!b) {
			parameters_ = new double[4];
		} else {
			parameters_ = new double[3];
		}
		parameters_[2] = defaultH_;
		//j_ = new Jacobian();
		
		gradients_ = new double[parameters_.length];
		//weight_ = new double[(kernelSize_ * 2 + 1)*(kernelSize_ * 2 + 1)];
		//Arrays.fill(weight_,1.0);
	}
	
	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#setImageData(ij.process.ImageProcessor)
	 */
	public void setImageData(ImageProcessor ip){
		ip_ = ip;
		bg_ = ip.getAutoThreshold();
	}

	double gauss(double x) {
//		int x100 = (int) Math.round(Math.abs(x * 100)); 
//		//return gaussLookupTable_[x100] * (x100 + 1 - x * 100) + gaussLookupTable_[x100+1] * (x * 100 - x100);
//		return gaussLookupTable_[x100];
		return Math.exp(- x*x/sigma2_);
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#refine(double, double)
	 */
	@Override
	public int refine(double x, double y) {
		//int w = 1 + 2 * kernelSize_;
		if (x < kernelSize_) {
			x0_ = kernelSize_;
		} else if (x >= ip_.getWidth() - kernelSize_) {
			x0_ = ip_.getWidth() - kernelSize_ - 1;
		} else { 
			x0_ = (int) x;
		}
		parameters_[0] = x0_ + .5 - x;

		if (y < kernelSize_) {
			y0_ = kernelSize_;
		} else if (y >= ip_.getHeight() - kernelSize_) {
			y0_ = ip_.getHeight() - kernelSize_ - 1;
		} else { 
			y0_ = (int) y;
		}
		parameters_[1] = y0_ + .5 - y;

		x_in = parameters_[0];
		y_in = parameters_[1];

		try {
			fit();
		} catch (ConvergenceException e) {
			//System.err.println(e.toString());
			return -1;
		} catch (Exception e) {
//			System.out.println(e.getMessage() + e.toString());
			return -2;
		}

		// report
//		double hw = 0.5 + kernelSize_;
//		for (int xi = - kernelSize_; xi <= kernelSize_; xi++) {
//			for (int yi = - kernelSize_; yi <= kernelSize_; yi++) {
//				double xp = Math.sin(parameters_[0]) * hw;
//				double yp = Math.sin(parameters_[1]) * hw;
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
	
	void fit() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		DifferentiableMultivariateRealOptimizer gno = new PowellOptimizer();
		//gno.setMaxIterations(500);
		parameters_[2] = ip_.get(x0_, y0_) - bg_;
		if (!zeroBg_) {
			parameters_[3] = bg_;
		}
		RealPointValuePair vp = gno.optimize(this, GoalType.MINIMIZE, parameters_);
		parameters_ = vp.getPoint(); 
		residue_ = vp.getValue() / parameters_[2] / parameters_[2] ; // normalized to H^2
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getXOut()
	 */
	@Override
	public double getXOut() {
		return (x0_ + .5 - parameters_[0]) ;
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getYOut()
	 */
	@Override
	public double getYOut() {
		return (y0_ + .5 - parameters_[1]);
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getHeightOut()
	 */
	@Override
	public double getHeightOut() {
		return parameters_[2];
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#getResidue()
	 */
	@Override
	public double getResidue() {
		return residue_ ;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.math.analysis.MultivariateRealFunction#value(double[])
	 */
	@Override
	public double value(double[] p) throws FunctionEvaluationException,IllegalArgumentException {
		//double hw = 0.5 + kernelSize_;
		double xp = p[0];
		double yp = p[1];
		double h = p[2];
		double bg = zeroBg_?0:p[3];
		
		double r = 0;
		Arrays.fill(gradients_, 0);
		for (int xi = - kernelSize_; xi <= kernelSize_; xi++) {
			for (int yi = - kernelSize_; yi <= kernelSize_; yi++) {
				double g = gauss(xp + xi)* gauss(yp + yi);
				double delta = bg + h*g - ip_.get(x0_ + xi , y0_ + yi);
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
}
