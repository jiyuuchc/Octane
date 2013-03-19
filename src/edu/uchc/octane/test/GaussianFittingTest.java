package edu.uchc.octane.test;

import java.lang.reflect.Method;

import org.apache.commons.math3.stat.StatUtils;

import edu.uchc.octane.BaseGaussianFitting;
import edu.uchc.octane.GaussianFitting2D;

import ij.process.ShortProcessor;

public class GaussianFittingTest {
	final static int size = 5;
	final static double sigma = 1;
	
	class ErrorCalculation {
		double[] d;
		int cnt;
		
		public ErrorCalculation(int N) {
			d = new double[N];
			cnt = 0;
		}
		
		public void addNumber(double v) {
			d[cnt++] = v;
		}
		
		public double getVariance() {
			return StatUtils.variance(d);
		}
	}

	BaseGaussianFitting module_;
	
	ErrorCalculation dX;
	ErrorCalculation dY;
	ErrorCalculation dZ;
	
	public Long test1() {
		
		double xOffset = (Math.random() - 0.5) ;
		double yOffset = (Math.random() - 0.5) ;
		
		
		ShortProcessor ip = TestDataGenerator.gaussianImage(
				size, // size 
				xOffset, 
				yOffset, 
				sigma * sigma * 2, 
				100, // intensity 
				50); // background

		TestDataGenerator.addShotNoise(ip, 1);

		long start = System.currentTimeMillis();
		module_.setImageData(ip);
		module_.preProcessBackground();
		module_.setupInitals(size , size , sigma, (int) (size));
		double [] p = module_.fit();

		long duration = System.currentTimeMillis() - start;

		System.out.println(
				" X:" + (xOffset - module_.getX() + size ) + 
				" Y:" + (yOffset - module_.getY() + size ) +
				" H:" + module_.getH());

		dX.addNumber(xOffset - module_.getX() + size);
		dY.addNumber(yOffset - module_.getY() + size);
		
		return duration;
	}
	

	public void repeatTest(int N, String testName) {
		long t = 0;

		dX = new ErrorCalculation(N);
		dY = new ErrorCalculation(N);
		dZ = new ErrorCalculation(N);

		for (int i = 0; i < N; i++) {
			Method method = null;
			try {
				method = getClass().getMethod(testName);
				t += (Long) (method.invoke(this));
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
		System.out.println("Average time " + (double) t/N + "ms");
		System.out.println("X error: " + dX.getVariance());
		System.out.println("Y error: " + dY.getVariance());
		System.out.println("Z error: " + dZ.getVariance());
	}
	
	public static void main(String [] arg) {
		GaussianFittingTest gft = new GaussianFittingTest();
		
		gft.module_ = new GaussianFitting2D();
		
		gft.repeatTest(100, "test1");
	}
}
		
