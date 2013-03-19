package edu.uchc.octane;

import org.apache.commons.math.util.FastMath;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;

public class GaussianFitting2D extends BaseGaussianFitting {

	double [] parameters_;
	double v_;
	boolean zeroBg_ = false;
	boolean floatingSigma_ = false;

	public void setZeroBackground(boolean b) {
		zeroBg_ = b;
		bg_ = 0;
	}
	
	public void setFloatingSigma(boolean b) {
		floatingSigma_ = b;
	}
	
	@Override
	public double [] fit() {
		
		if (floatingSigma_) {
			if (zeroBg_) {
				parameters_ = new double[] {0, 0, pixelValue(0, 0) - bg_, sigma2_};
			} else {
				parameters_ = new double[] {0, 0, pixelValue(0, 0) - bg_, bg_, sigma2_};
			}			
		} else {
			if (zeroBg_) {
				parameters_ = new double[] {0, 0, pixelValue(0, 0) - bg_};
			} else {
				parameters_ = new double[] {0, 0, pixelValue(0, 0) - bg_, bg_};
			}
		}

		PowellOptimizer optimizer = new PowellOptimizer(1e-4, 1e-1);
		
		MultivariateFunction func = new MultivariateFunction() {
			@Override
			public double value(double[] point) {
				
				parameters_ = point;
				double bg = zeroBg_ ? 0 : point[3]; 
					
				double v = 0;
				
				for (int xi = - windowSize_; xi < windowSize_; xi ++) {
					for (int yi = - windowSize_; yi < windowSize_; yi ++) {
						double delta = getValueExcludingBackground(xi, yi) + bg - pixelValue(xi, yi);
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
					new InitialGuess(parameters_),
					new MaxEval(1000),
					GoalType.MINIMIZE);
		} catch (TooManyEvaluationsException e) {
			return null;
		}

		parameters_ = pvp.getPoint();
		v_ = pvp.getValue();
		
		return parameters_;
	}

	@Override
	public double getValueExcludingBackground(int xi, int yi) {
		double x = ( - parameters_[0] + xi);
		double y = ( - parameters_[1] + yi);
		
		return  FastMath.exp(- (x*x + y*y) / (floatingSigma_ ? parameters_[parameters_.length - 1]:sigma2_)) * parameters_[2];
	}

	@Override
	public double getX() {
		return parameters_[0] + x0_;
	}

	@Override
	public double getY() {
		return parameters_[1] + y0_;
	}

	@Override
	public double getZ() {
		return 0;
	}

	@Override
	public double getH() {
		return parameters_[2];
	}

	@Override
	public double getE() {
		double m = 0;
		double m2 = 0;
		for (int xi = - windowSize_; xi <= windowSize_; xi++) {
			for (int yi = - windowSize_; yi <= windowSize_; yi++) {
				double v = pixelValue(xi, yi);
				m += v;
				m2 += v * v ;
			}
		}

		int nPixels = (1 + 2 * windowSize_)*(1 + 2 * windowSize_);
		m = m2 - m * m / nPixels; //variance of the grey values

		return nPixels * FastMath.log(m / v_);
	}

	@Override
	public void preProcessBackground() {
		super.preProcessBackground();
		setZeroBackground(true);
	}
}
