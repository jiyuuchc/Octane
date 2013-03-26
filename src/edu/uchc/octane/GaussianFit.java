package edu.uchc.octane;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;

public class GaussianFit extends BaseGaussianFit {

	//double [] parameters_;
	boolean floatingSigma_ = false;
	
	double sigma2_;

//	public void setZeroBackground(boolean b) {
//		bZeroBg_ = b;
//	}

	public void setFloatingSigma(boolean b) {
		floatingSigma_ = b;
	}
	
	public void setPreferredSigmaValue(double sigma) {
		sigma2_ = sigma * sigma * 2;
	}
	
	@Override
	public double [] fit() {
		
		double [] initParameters;
		
		if (floatingSigma_) {
			if (bZeroBg_) {
				initParameters = new double[] {0, 0, pixelValue(0, 0) - bg_, sigma2_};
			} else {
				initParameters = new double[] {0, 0, pixelValue(0, 0) - bg_, bg_, sigma2_};
			}			
		} else {
			if (bZeroBg_) {
				initParameters = new double[] {0, 0, pixelValue(0, 0) - bg_};
			} else {
				initParameters = new double[] {0, 0, pixelValue(0, 0) - bg_, bg_};
			}
		}

		PowellOptimizer optimizer = new PowellOptimizer(1e-4, 1e-1);
		
		MultivariateFunction func = new MultivariateFunction() {
			@Override
			public double value(double[] point) {
				
				//initParameters = point;
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

		return pvp.getPoint();
	}

	@Override
	public double getValueExcludingBackground(int xi, int yi, double [] p) {
		double x = ( - p[0] + xi);
		double y = ( - p[1] + yi);
		
		return  FastMath.exp(- (x*x + y*y) / (floatingSigma_ ? p[p.length - 1]:sigma2_)) * p[2];
	}
}
