package edu.uchc.octane.test;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import edu.uchc.octane.BaseGaussianFit;
import edu.uchc.octane.GaussianFit;
import edu.uchc.octane.GaussianFit3DSimple;

import ij.process.ShortProcessor;

public class GaussianFittingTest {
	
	final static int size = 2;
	final static double sigma = 1;
	final static int intensity = 100;
	final static int background = 0;
	
	final boolean preProcessBackground = false;
	
	final static int imgSize = 256;
	final static int nParticles = 300;
	
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
	
	Long test0(double xOffset, double yOffset){
		long start = System.nanoTime();

		module_.setupInitalValues(size, size , sigma, (int) (size));
		double [] p = module_.fit();
		
		if (p == null) {
			throw(new RuntimeException("fitting failed"));
		}

		long duration = System.nanoTime() - start;

		System.out.format(" X:%+6.4f\tY:%+6.4f\tZ:%6.4f\tE:%6.1f%n", 
				(xOffset - module_.getX() + size ), 
				(yOffset - module_.getY() + size ),
				module_.getZ(), module_.getE());

		dX.addNumber(xOffset - module_.getX() + size);
		dY.addNumber(yOffset - module_.getY() + size);
		dZ.addNumber(module_.getZ());
		return duration;
	}
	
	public Long test1(double xOffset, double yOffset) { //No noise

		ShortProcessor ip = TestDataGenerator.gaussianImage(
				size, // size 
				xOffset, 
				yOffset, 
				sigma * sigma * 2, 
				intensity, 
				background); // background
		
		module_.setImageData(ip, preProcessBackground);
		
		return test0(xOffset, yOffset);

	}
	

	public Long test2(double xOffset, double yOffset) { //with noise
		
		ShortProcessor ip = TestDataGenerator.gaussianImage(
				size, // size 
				xOffset, 
				yOffset, 
				sigma * sigma * 2, 
				intensity, // intensity 
				background); // background

		TestDataGenerator.addShotNoise(ip, 1);
		
		module_.setImageData(ip, preProcessBackground);
		
		return test0(xOffset, yOffset);
	}

	public void repeatTest(int N, String testName) {

		long t = 0;

		dX = new ErrorCalculation(N);
		dY = new ErrorCalculation(N);
		dZ = new ErrorCalculation(N);

		int nFailed = 0;
		for (int i = 0; i < N; i++) {
			
			double xOffset = (Math.random() - 0.5) ;
			double yOffset = (Math.random() - 0.5) ;

			Method method = null;
			try {
				method = getClass().getMethod(testName, double.class, double.class);
				t += (Long) (method.invoke(this, xOffset, yOffset));
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

	public void repeatTestNoneRandom(int N, String testName) {
		long t = 0;

		dX = new ErrorCalculation(N*N);
		dY = new ErrorCalculation(N*N);
		dZ = new ErrorCalculation(N*N);

		int nFailed = 0;
		for (double xOffset =  -0.5; xOffset < 0.5; xOffset += 1.0/N ) {
			for (double yOffset =  -0.5; yOffset < 0.5; yOffset += 1.0/N ) {

				Method method = null;
				try {
					method = getClass().getMethod(testName, double.class, double.class);
					t += (Long) (method.invoke(this, xOffset, yOffset));
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
		}
		System.out.println(testName + " excecuted " + (N*N) + " times. Failed " + nFailed + " Times");
		System.out.println("Average time " + (double) (t/ (N*N - nFailed)) + "ns");
		System.out.println("X error: " + dX.getVariance());
		System.out.println("Y error: " + dY.getVariance());
		System.out.println("Z error: " + dZ.getVariance());
	}

	public void setModule(BaseGaussianFit m) {
		module_ = m;
	}
	
	
	public void multipleParticleTest() {

		ShortProcessor ip = new ShortProcessor(imgSize, imgSize);

		long totalTime = 0;

		double [][] pos = TestDataGenerator.randomMultipleSpots(ip, nParticles, 2 * sigma * sigma, intensity, background);
		TestDataGenerator.addShotNoise(ip, 1);

		int N = pos[0].length;
		dX = new ErrorCalculation(N);
		dY = new ErrorCalculation(N);
		dZ = new ErrorCalculation(N);
		
		module_.setImageData(ip, preProcessBackground);

		for (int i = 0; i < N; i++) {
			long start = System.nanoTime();

			module_.setupInitalValues((int)(pos[0][i] +.5), (int)(pos[1][i]+.5), sigma, (int) (size));
			double [] p = module_.fit();
			long duration = System.nanoTime() - start;
			
			if (p == null) {
				System.out.println("Failed");
			} else {
				System.out.format(" X:%+6.4f\tY:%+6.4f\tZ:%6.4f\tE:%6.1f%n", 
						(pos[0][i] - module_.getX()), 
						(pos[1][i] - module_.getY()),
						module_.getZ(), module_.getE());

				dX.addNumber(pos[0][i] - module_.getX());
				dY.addNumber(pos[1][i] - module_.getY());
				dZ.addNumber(module_.getZ());
				totalTime += duration;
				
				module_.deflate();
			}
		}
		
		System.out.println("Average time: " + (totalTime / N) + " ms");
	}
	
	public static void main(String [] arg) {
		
		final GaussianFittingTest gft = new GaussianFittingTest();
		
		GaussianFit3DSimple m = new GaussianFit3DSimple();
		m.setCalibrationValues(new double[]{0, 1, 0.0000000009});

		gft.setModule(m);
		
		//gft.repeatTestNoneRandom(10, "test1");
		gft.multipleParticleTest();
	}
}
		
