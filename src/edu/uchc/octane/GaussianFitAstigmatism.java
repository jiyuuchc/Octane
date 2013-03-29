//FILE:          GaussianFitAstigmatism.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/26/13
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

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

/**
 * This class implement 3D centroid localization based on the astigmatism method proposed by
 * Xiaowei Zhuang lab.   
 * @author Ji-Yu
 *
 */
public class GaussianFitAstigmatism extends BaseGaussianFit {

	//double [] parameters_;
	double sigma2_;

	double [] calibration_ = null;
	double p1_, p2_, p3_; 
	double z_, z0min_, z0max_;

	final static double errTol_ = 2.0;

	/* (non-Javadoc)
	 * @see edu.uchc.octane.BaseGaussianFit#fit()
	 */
	@Override
	public double[] fit() {
		double [] initParameters;

		if (bZeroBg_) {
			initParameters = new double[] {0, 0, pixelValue(0, 0) - bg_, sigma2_, sigma2_};
		} else {
			initParameters = new double[] {0, 0, pixelValue(0, 0) - bg_, sigma2_, sigma2_, bg_};
		}

		PowellOptimizer optimizer = new PowellOptimizer(1e-4, 1e-1);
	
		MultivariateFunction func = new MultivariateFunction() {
			@Override
			public double value(double[] point) {

				double bg = bZeroBg_ ? 0 : point[3]; 

				double v = 0;

				for (int xi = - windowSize_; xi < windowSize_; xi ++) {
					for (int yi = - windowSize_; yi < windowSize_; yi ++) {
						double delta = getValueExcludingBackground(xi, yi, point) + bg - pixelValue(xi, yi);
						v += delta * delta;
					}
				}
				return v;
			}
		};

		PointValuePair pvp;
		try {
			pvp = optimizer.optimize(
					new ObjectiveFunction(func),
					new InitialGuess(initParameters),
					new MaxEval(10000),
					GoalType.MINIMIZE);
		} catch (TooManyEvaluationsException e) {
			return null;
		}

		pvp_ = pvp;
		
//		double sigmax = pvp.getPoint()[3];
//		double sigmay = pvp.getPoint()[4];
//		if (calibration_ != null) {
//			double [] pn = new double[4];
//			pn[0] = p1_;
//			pn[1] = p2_;
//			pn[2] = p3_ + 2 * calibration_[2] * (calibration_[0] - sigmax) + 2 * calibration_[5] * (calibration_[3] - sigmay);
//			pn[3] = calibration_[1] * (calibration_[0] - sigmax) + calibration_[4] * (calibration_[3] - sigmay);
//			
//			Complex [] roots = new LaguerreSolver().solveAllComplex(pn, 0);
//			
//			int minIndex = -1;
//			double minV = Double.MAX_VALUE;
//			for (int i = 0; i < 3; i ++) {
//				double v;
//				if (FastMath.abs(roots[i].getImaginary()) < 1e-7) {
//					double z = roots[i].getReal(); 
//					double vx = calibration_[0] + calibration_[1] * z + calibration_[2] * z * z - sigmax;
//					double vy = calibration_[3] + calibration_[4] * z + calibration_[5] * z * z - sigmay;
//					v = vx * vx + vy * vy;
//					if (v < minV) {
//						minV = v;
//						minIndex = i;
//					}
//				}
//			}
//			
//			if (minV > errTol_) {
//				return null;
//			}
//
//			z_ = roots[minIndex].getReal();
//		}

		calculateZ();
		
		return pvp.getPoint();
	}

	/**
	 * calculate the Z coordinate based on astigmatism
	 */
	void calculateZ() {
		
		UnivariateFunction func = new UnivariateFunction() {
			@Override
			public double value(double z) { 
				double sigmax = pvp_.getPoint()[3];
				double sigmay = pvp_.getPoint()[4];
				double vx = calibration_[0] + calibration_[1] * z + calibration_[2] * z * z - sigmax;
				double vy = calibration_[3] + calibration_[4] * z + calibration_[5] * z * z - sigmay;
				return  vx * vx + vy * vy;				
			}
		};
		
		BrentOptimizer optimizer = new BrentOptimizer(1e-4, 1e-4);
		
		UnivariatePointValuePair upvp = optimizer.optimize(new UnivariateObjectiveFunction(func),
				GoalType.MINIMIZE,
				MaxEval.unlimited(),
				new SearchInterval(2 * z0min_ - z0max_, 2 * z0max_ - z0min_));

		if (upvp.getValue() > errTol_) {
			throw (new ConvergenceException());
		}
		
		z_ = upvp.getPoint();
	}
	
	/**
	 * Set initial value of the sigma
	 * @param sigma The sigma value of the Gaussian function
	 */
	public void setPreferredSigmaValue(double sigma) {
		sigma2_ = sigma * sigma * 2;
	}

	/**
	 * Specify the Z calibration
	 * The Xsigma(Z) and Ysigma(Z) are assumed to be 2nd order polynomial functions. Therefore 6 
	 * real numbers are needed to specify the calibration    
	 * @param p Six calibration parameters. First three for X and next three for Y. 
	 */
	public void setCalibration(double [] p) {
		if (p == null) {
			calibration_ = null;
			return;
		}

		if (p.length != 6) {
			throw (new IllegalArgumentException("Array unacceptable length"));
		}
		
		if (p[2] < 0 || p[5] < 5) {
			throw new IllegalArgumentException("2nd order coeff not positive");
		}
		
		calibration_ = p;
		
		p1_ = 2 * p[2] * p[2] + 2 * p[5] * p[5];
		p2_ = 3 * p[1] * p[2] + 3 * p[4] * p[5]; 
		p3_ = p[1] * p[1] + p[4] * p[4];
		
		z0min_ = p[1]/p[2]/2;
		z0max_ = p[4]/p[5]/2;
		if (z0min_ > z0max_) {
			double z0;
			z0 = z0min_;
			z0min_ = z0max_;
			z0max_ = z0;			
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.uchc.octane.BaseGaussianFit#getValueExcludingBackground(int, int, double[])
	 */
	@Override
	public double getValueExcludingBackground(int xi, int yi, double [] p) {
		double x = ( - p[0] + xi);
		double y = ( - p[1] + yi);
		
		return  FastMath.exp(- (x*x)/p[3]  -  (y*y)/p[4]) * p[2];
	}

	/* (non-Javadoc)
	 * @see edu.uchc.octane.BaseGaussianFit#getZ()
	 */
	@Override
	public double getZ() {
		return z_;
	}
}
