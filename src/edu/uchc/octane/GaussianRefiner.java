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

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.direct.NelderMead;

import ij.IJ;
import ij.process.ImageProcessor;

public class GaussianRefiner implements SubPixelRefiner, MultivariateRealFunction {
	
	static final int kernelSize_ = 3;
	static final double defaultH_ = 200.0;
	static final double sigma2_ = 1.73;
	
	static double [] gaussLookupTable_;
	
	static {
		gaussLookupTable_ = new double[(kernelSize_ * 2 + 1) * 100];
		//IJ.log("starting building index...");
		for (int i = 0 ; i < (kernelSize_ * 2 + 1) * 100; i ++ ) {
			gaussLookupTable_[i] = Math.exp(- (i * i) / (sigma2_ * 10000));
		}
		//IJ.log("Finishing building index...");
	}
	
	int blocks_;

	int x0_,y0_;
	ImageProcessor ip_;
	double [] parameters_; 
	double residue_;
	double bg_ = 1700.0;

	public GaussianRefiner(ImageProcessor ip){
		ip_ = ip;
		parameters_ = new double[4];
		parameters_[2] = defaultH_;
		bg_ = ip.getAutoThreshold();
	}

	double gauss(double x) {
		int x100 = (int) Math.round(Math.abs(x * 100)); 
		//return gaussLookupTable_[x100] * (x100 + 1 - x * 100) + gaussLookupTable_[x100+1] * (x * 100 - x100);
		return gaussLookupTable_[x100];
		//return Math.exp(x100*x100/10000);
	}

	@Override
	public int refine(double x, double y) {
		int w = 1 + 2 * kernelSize_;
		if (x < kernelSize_) {
			x0_ = kernelSize_;
			parameters_[0] = Math.asin(2 * (x - x0_) / w);
		} else if (x >= ip_.getWidth() - kernelSize_) {
			x0_ = ip_.getWidth() - kernelSize_ - 1;
			parameters_[0] = Math.asin(2 * (x - x0_) / w);
		} else { 
			x0_ = (int) x;
			parameters_[0] = 0;
		}

		if (y < kernelSize_) {
			y0_ = kernelSize_;
			parameters_[1] = Math.asin(2 * (y - y0_) / w);
		} else if (y >= ip_.getHeight() - kernelSize_) {
			y0_ = ip_.getHeight() - kernelSize_ - 1;
			parameters_[1] = Math.asin(2 * (y - y0_) / w);
		} else { 
			y0_ = (int) y;
			parameters_[1] = 0;
		}

		parameters_[3] = bg_;

		try {
			fitWithNelderMead();
		} /*catch (ConvergenceException e) {
			IJ.log(e.getMessage());
			return 0;
		}*/
		catch (Exception e) {
			return -1;
		}
		return 0;
	}
	
	void fitWithNelderMead() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		NelderMead nm = new NelderMead();

		RealPointValuePair vp = nm.optimize(this, GoalType.MINIMIZE, parameters_);
		parameters_ = vp.getPoint();
		residue_ = vp.getValue() / parameters_[2] / parameters_[2]; // normalized to H^2
	}

//	public int fitWithUncmin(double x, double y) {
//		FunctionOptimizer fo = new FunctionOptimizer();
//		
//		fo.setDimension(4);
//		fo.setInitialX(parameters_);
//		fo.setOptimizationMode(FunctionOptimizer.OptimizationMode.Function);
//		fo.optimizeFunction(this);
//		
//		if (fo.getTerminationCode() == FunctionOptimizer.TerminationCode.IterationLimit) {
//			//IJ.log("iterationlimit");
//			return -1;
//		}
//		if (fo.getTerminationCode() == FunctionOptimizer.TerminationCode.TooManyLargeStepsProbableUnboundFunction) {
//			//IJ.log("unbound");
//			return -2;
//		}
//
//		parameters_ = fo.getOptimum();
//		residue_ = fo.getFunctionAtOptimum();
//		return 0;
//	}

	@Override
	public double getXOut() {
		return (x0_ + 0.5 + Math.sin(parameters_[0]) * (kernelSize_ + 0.5));
	}

	@Override
	public double getYOut() {
		return (y0_ + 0.5 + Math.sin(parameters_[1]) * (kernelSize_ + 0.5));
	}

	@Override
	public double getHeightOut() {
		return parameters_[2];
	}

	@Override
	public double getResidue() {
		return residue_ / (2 * kernelSize_ + 1) / (2 * kernelSize_ + 1);
	}

//	@Override
//	public void setNumberOfProcessingBlocks(int number) {
//		blocks_ = number;		
//	}
//
//	@Override
//	public int getNumberOfProcessingBlocks() {
//		return 1;
//		//return blocks_;
//	}
//
//	@Override
//	public double evaluate(double[] p, int block) {
//		double f = 0;
//		double hw = 0.5 + kernelSize_;
//		double xp = Math.sin(p[0]) * hw;
//		double yp = Math.sin(p[1]) * hw;
//		double h = p[2];
//		double bg = p[3];
//		
//		for (int xi = - kernelSize_; xi <= kernelSize_; xi++) {
//			for (int yi = - kernelSize_; yi <= kernelSize_; yi++) {
//				double delta =  (double)(ip_.get(x0_ + xi , y0_ + yi))
//						- bg
//						- h * gauss(xp + xi) * gauss( yp + yi);
//				f += delta * delta;
//			}
//		}
//		return f;
//	}

	@Override
	public double value(double[] p) throws FunctionEvaluationException,
			IllegalArgumentException {

		double f = 0;
		double hw = 0.5 + kernelSize_;
		double xp = Math.sin(p[0]) * hw;
		double yp = Math.sin(p[1]) * hw;
		double h = p[2];
		double bg = p[3];
		if (bg < 0 || h < 0) {
			return 1e10;
		}
		for (int xi = - kernelSize_; xi <= kernelSize_; xi++) {
			for (int yi = - kernelSize_; yi <= kernelSize_; yi++) {
				double delta =  (double)(ip_.get(x0_ + xi , y0_ + yi))
						- bg
						- h * gauss(xp + xi) * gauss( yp + yi);
				f += delta * delta;
			}
		}

		return f;
	}
}
