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
import org.apache.commons.math.analysis.DifferentiableMultivariateVectorialFunction;
import org.apache.commons.math.analysis.MultivariateMatrixFunction;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.optimization.VectorialPointValuePair;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;

import ij.process.ImageProcessor;

public class GaussianRefiner implements SubPixelRefiner, DifferentiableMultivariateVectorialFunction {
	
	static final int kernelSize_ = 2;
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
	
	int blocks_;

	int x0_,y0_;
	ImageProcessor ip_;
	double [] parameters_; 
	double residue_;
	double bg_ = 1700.0;

	double [][] gradients_;
	double [] weight_;
	Jacobian j_; 

	class Jacobian implements MultivariateMatrixFunction {

		@Override
		public double[][] value(double[] p)
				throws FunctionEvaluationException, IllegalArgumentException {
			double hw = 0.5 + kernelSize_;
			double xp = Math.sin(p[0]) * hw;
			double yp = Math.sin(p[1]) * hw;
			double h = p[2];
			int cnt = 0;
			for (int xi = - kernelSize_; xi <= kernelSize_; xi++) {
				for (int yi = - kernelSize_; yi <= kernelSize_; yi++) {
					double g = gauss(xp + xi)* gauss(yp + yi);
					gradients_[cnt][0] = -2 * h * g * (xp + xi) * hw / sigma2_ * Math.cos(p[0]);
					gradients_[cnt][1] = -2 * h * g * (yp + yi) * hw / sigma2_ * Math.cos(p[1]); 
					gradients_[cnt][2] = g;
					gradients_[cnt][3] = 1;
					cnt ++;
				}
			}

			
//			if (p[2] < 0) {
//				for (int i = 0; i < gradients_.length; i++) {
//					gradients_[i][2] = 1e10;
//				}
//			}
//			if (p[3] < 0) {
//				for (int i = 0; i < gradients_.length; i++) {
//					gradients_[i][3] = 1e10;
//				}
//			}
			return gradients_;
		}
		
	}

	public GaussianRefiner(ImageProcessor ip){
		ip_ = ip;
		parameters_ = new double[4];
		parameters_[2] = defaultH_;
		bg_ = ip.getAutoThreshold();
		j_ = new Jacobian();
		
		gradients_ = new double[(kernelSize_ * 2 + 1)*(kernelSize_ * 2 + 1)][];
		for ( int i = 0 ; i < gradients_.length; i ++) {
			gradients_[i] = new double[4];
		}
		
		weight_ = new double[(kernelSize_ * 2 + 1)*(kernelSize_ * 2 + 1)];
		Arrays.fill(weight_,1.0);
	}

	double gauss(double x) {
//		int x100 = (int) Math.round(Math.abs(x * 100)); 
//		//return gaussLookupTable_[x100] * (x100 + 1 - x * 100) + gaussLookupTable_[x100+1] * (x * 100 - x100);
//		return gaussLookupTable_[x100];
		return Math.exp(- x*x/sigma2_);
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
			fit();
		} catch (ConvergenceException e) {
			System.out.println("Convergence error");
		} catch (Exception e) {
			System.out.println(e.getMessage() + e.toString());
			return -1;
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
		
		return 0;
	}
	
	void fit() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		GaussNewtonOptimizer gno = new GaussNewtonOptimizer(false);
		int cnt = 0;
		double target[] = new double[(kernelSize_ * 2 + 1)*(kernelSize_ * 2 + 1)];
		for (int xi = - kernelSize_; xi <= kernelSize_; xi++) {
			for (int yi = - kernelSize_; yi <= kernelSize_; yi++) {
				target[cnt++] =  (double)(ip_.get(x0_ + xi , y0_ + yi));
			}
		}
		parameters_[2] = ip_.get(x0_, y0_) - bg_;
		VectorialPointValuePair vp = gno.optimize(this, target, weight_, parameters_);
		parameters_ = vp.getPoint(); 
		residue_ = gno.getRMS() / parameters_[2]; // normalized to H^2
	}

//	void fitWithNelderMead() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
//		NelderMead nm = new NelderMead();
//
//		RealPointValuePair vp = nm.optimize(this, GoalType.MINIMIZE, parameters_);
//		parameters_ = vp.getPoint();
//		residue_ = vp.getValue() / parameters_[2] / parameters_[2]; // normalized to H^2
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
		return residue_ ;
	}

	@Override
	public double[] value(double[] p) throws FunctionEvaluationException,IllegalArgumentException {
		double hw = 0.5 + kernelSize_;
		double xp = Math.sin(p[0]) * hw;
		double yp = Math.sin(p[1]) * hw;
		double h = p[2];
		double bg = p[3];
		double [] r = new double[(kernelSize_* 2 + 1) * (kernelSize_* 2 + 1)];
//		if (bg < 0 || h < 0) {
//			Arrays.fill(r, 1e10);
//			return r;
//		}
		
		//Arrays.fill(gradients_, 0.0);
		
		int cnt = 0;
		for (int xi = - kernelSize_; xi <= kernelSize_; xi++) {
			for (int yi = - kernelSize_; yi <= kernelSize_; yi++) {
				double g = gauss(xp + xi)* gauss(yp + yi);
				r[cnt] = bg + h*g;
				cnt ++;
			}
		}

		return r;
	}

	@Override
	public MultivariateMatrixFunction jacobian() {
		return j_;
	}
}
