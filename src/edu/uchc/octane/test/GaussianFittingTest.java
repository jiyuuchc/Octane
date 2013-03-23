package edu.uchc.octane.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.BaseGaussianFit;
import edu.uchc.octane.GaussianFit;

import ij.process.ShortProcessor;

public class GaussianFittingTest {

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
			return FastMath.sqrt(StatUtils.variance(d));
		}
	}

	BaseGaussianFit module_;
	
	ErrorCalculation dX;
	ErrorCalculation dY;
	ErrorCalculation dZ;
	
	public Long test1() {

		final int size = 2;
		final double sigma = 1;
	
		setModule(new GaussianFit());
		
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

		long start = System.nanoTime();
		module_.setImageData(ip, false);
		//module_.preProcessBackground();
		module_.setupInitalValues(size , size , sigma, (int) (size));
		double [] p = module_.fit();

		long duration = System.nanoTime() - start;

		System.out.println(
				" X:" + (xOffset - module_.getX() + size ) + 
				" Y:" + (yOffset - module_.getY() + size ) +
				" H:" + module_.getH());

		dX.addNumber(xOffset - module_.getX() + size);
		dY.addNumber(yOffset - module_.getY() + size);
		dZ.addNumber(FastMath.sqrt(p[p.length-1]/2) - sigma);
		return duration;
	}
	

	public void repeatTest(int N, String testName) {
		long t = 0;

		dX = new ErrorCalculation(N);
		dY = new ErrorCalculation(N);
		dZ = new ErrorCalculation(N);

		int nFailed = 0;
		for (int i = 0; i < N; i++) {
			Method method = null;
			try {
				method = getClass().getMethod(testName);
				t += (Long) (method.invoke(this));
			} catch (InvocationTargetException e) {
				// usually means that the fitting failed.
				System.out.print("Test throw an exception " + e.getCause().toString());
				System.out.println(" Test failed");
				nFailed ++;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e.toString());
			}
		}
		System.out.println(testName + " excecuted " + N + " times. Failed " + nFailed + " Times");
		System.out.println("Average time " + (double) (t/ (N - nFailed)) + "ns");
		System.out.println("X error: " + dX.getVariance());
		System.out.println("Y error: " + dY.getVariance());
		System.out.println("Z error: " + dZ.getVariance());
	}

	public void setModule(BaseGaussianFit m) {
		module_ = m;
	}
	
	public static void main(String [] arg) {
		final GaussianFittingTest gft = new GaussianFittingTest();
		
		gft.repeatTest(100, "test1");
	}
}
		
