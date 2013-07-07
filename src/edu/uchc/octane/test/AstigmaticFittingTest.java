package edu.uchc.octane.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;
import edu.uchc.octane.GaussianFitAstigmatism;
import ij.process.ShortProcessor;

public class AstigmaticFittingTest {

	final static int size = 3;
	final static double sigma = 1;
	final static int intensity = 100;
	final static int background = 0;
	
	double sigmax, sigmay;

	GaussianFitAstigmatism module_;

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
		
		public double getMean() {
			return StatUtils.mean(d);
		}
	}

	
	static double square(double x) {
		return x * x;
	}
	
	Long test0(double xOffset, double yOffset){

		long start = System.nanoTime();

		double [] p = module_.fit();

		if (p == null) {
		
			throw(new RuntimeException("fitting failed"));
		
		}

		long duration = System.nanoTime() - start;

		System.out.format(" X:%+6.4f\tY:%+6.4f\tSX:%6.4f\tSY:%6.4f%n", 
				(xOffset - module_.getX() + size ), 
				(yOffset - module_.getY() + size ),
				FastMath.sqrt(module_.getSigmaX()/2), 
				FastMath.sqrt(module_.getSigmaY()/2)
				);

		return duration;
	}

	public Long test1(double xOffset, double yOffset) { //No noise

		sigmax = sigma * sigma * 2;
		sigmay = sigma * sigma * 2;

		ShortProcessor ip = TestDataGenerator.astigmaticGaussianImage(
				size, // size 
				xOffset, 
				yOffset, 
				sigmax,
				sigmay,
				intensity, 
				background); // background

		module_.setImageData(ip);

		return test0(xOffset, yOffset);
	}

	public Long test2(double xOffset, double yOffset) { //with noise

		sigmax = sigma * sigma * 2;
		sigmay = sigma * sigma * 2;

		ShortProcessor ip = TestDataGenerator.astigmaticGaussianImage(
				size, // size 
				xOffset, 
				yOffset, 
				sigmax, 
				sigmay,
				intensity, // intensity 
				background); // background

		TestDataGenerator.addShotNoise(ip, 1);

		module_.setImageData(ip);

		return test0(xOffset, yOffset);
	}
	
	public Long test3(double xOffset, double yOffset) {

		double z = Math.random();
		double sigma2 = sigma * sigma * 2;
		sigmax = sigma2 * square( 1 + z * z);
		sigmay = sigma2 * square( 1 + (1-z) * (1-z));
		
		ShortProcessor ip = TestDataGenerator.astigmaticGaussianImage(
				size, // size 
				xOffset, 
				yOffset, 
				sigmax, 
				sigmay,
				intensity, // intensity 
				background); // background

		TestDataGenerator.addShotNoise(ip, 1);

		module_.setImageData(ip);

		return test0(xOffset, yOffset);

	}

	public void repeatTest(int N, String testName) {

		long t = 0;

		ErrorCalculation dX = new ErrorCalculation(N);
		ErrorCalculation dY = new ErrorCalculation(N);
		ErrorCalculation dSX = new ErrorCalculation(N);
		ErrorCalculation dSY = new ErrorCalculation(N);

		int nFailed = 0;
		for (int i = 0; i < N; i++) {

			double xOffset = (Math.random() - 0.5) ;
			double yOffset = (Math.random() - 0.5) ;

			Method method = null;

			try {
			
				method = getClass().getMethod(testName, double.class, double.class);
			
				t += (Long) (method.invoke(this, xOffset, yOffset));

				dX.addNumber(xOffset - module_.getX() + size);
				dY.addNumber(yOffset - module_.getY() + size);
				dSX.addNumber(FastMath.sqrt((module_.getSigmaX()/2)) - FastMath.sqrt(sigmax/2));
				dSY.addNumber(FastMath.sqrt((module_.getSigmaY()/2)) - FastMath.sqrt(sigmay/2));
				
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
		System.out.println("X error: " + dX.getMean() + " +/- " + dX.getVariance());
		System.out.println("Y error: " + dY.getMean() + " +/- " + dY.getVariance());
		System.out.println("SX error: " + dSX.getMean() + " +/- " + dSX.getVariance());
		System.out.println("SY error: " + dSY.getMean() + " +/- " + dSY.getVariance());

	}

	// this test calculate Z value
	public void test4(int N) {

		long t = 0;

		ErrorCalculation dX = new ErrorCalculation(N);
		ErrorCalculation dY = new ErrorCalculation(N);
		ErrorCalculation dZ = new ErrorCalculation(N);

		double [] c = {1.0, 0, 1.0, 1.0, 1.0, 1.0};
		module_.setCalibration(c);
		
		int nFailed = 0;
		for (int i = 0; i < N; i++) {

			double xOffset = (Math.random() - 0.5) ;
			double yOffset = (Math.random() - 0.5) ;


			double z = Math.random();
			double sigma2 = sigma * sigma * 2;
			sigmax = sigma2 * square( 1 + z * z);
			sigmay = sigma2 * square( 1 + (1-z) * (1-z));
			
			ShortProcessor ip = TestDataGenerator.astigmaticGaussianImage(
					size, // size 
					xOffset, 
					yOffset, 
					sigmax, 
					sigmay,
					intensity, // intensity 
					background); // background

			TestDataGenerator.addShotNoise(ip, 1);

			module_.setImageData(ip);
			try {
				t += test0(xOffset, yOffset);
				
				dX.addNumber(xOffset - module_.getX() + size);
				dY.addNumber(yOffset - module_.getY() + size);
				dZ.addNumber(module_.getZ() - z);
				
			} catch (RuntimeException e) {

				// usually means that the fitting failed.
				System.out.println("Test failed");
				nFailed ++;

			} catch (Exception e) {
				
				e.printStackTrace();
				System.out.println(e.toString());
			
			}
		}
		
		System.out.println("Test 4"+ " excecuted " + N + " times. Failed " + nFailed + " Times");
		System.out.println("Average time " + (double) (t/ (N - nFailed)) + "ns");
		System.out.println("X error: " + dX.getMean() + " +/- " + dX.getVariance());
		System.out.println("Y error: " + dY.getMean() + " +/- " + dY.getVariance());
		System.out.println("Z error: " + dZ.getMean() + " +/- " + dZ.getVariance());

	}

	public void setModule(GaussianFitAstigmatism m) {
		module_ = m;
	}

	public static void main(String [] arg) {

		final AstigmaticFittingTest aft = new AstigmaticFittingTest();

		GaussianFitAstigmatism m = new GaussianFitAstigmatism();
		
		aft.setModule(m);
		
		m.setPreprocessBackground(true);
		m.setPreferredSigmaValue(sigma * sigma * 2);
		m.setWindowSize(size);
		m.setInitialCoordinates(size, size);
		
		m.setCalibration(null);
		
		System.out.println("Test 1: Single point, random position, Fixed width, no noise");
		aft.repeatTest(100, "test1");
		
		System.out.println("Test 2: Single point, random position, Fixed width, with shot noise");
		aft.repeatTest(100, "test2");
		
		System.out.println("Test 3: Single point, random position, variable width, with shot noise");
		aft.repeatTest(100, "test3");

		System.out.println("Test 4: Single point, random position, variable Z, with shot noise");
		aft.test4(100);
	}
}
