package edu.uchc.octane.test;

import edu.uchc.octane.GaussianFitting;
import edu.uchc.octane.GaussianFittingM3;
import ij.process.ShortProcessor;

public class GaussianFittingTest {
	final static int size = 2;
	final static double sigma = .8;

	public static long test1() {
		GaussianFitting fittingModule = new GaussianFitting(false);
		
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
		
		fittingModule.setImageData(ip);
		long start = System.currentTimeMillis();
		fittingModule.fitGaussianAt(size , size , sigma, (int) (size));
		long duration = System.currentTimeMillis() - start;
		
		System.out.println(
				" X:" + xOffset + size + " | " + (fittingModule.getX() - size - 0.5) + 
				" Y:" + yOffset + size + " | " + (fittingModule.getY() - size - 0.5) +
				" H:" + fittingModule.getHeight());
		
		return duration;
	}

	public static void repeatTest1(int N) {
		long t = 0;
		for (int i = 0; i < N; i++) {
			t += test1();
		}
		System.out.println("Average time " + (double) t/N + "ms");
	}
	
	public static void main(String [] arg) {
		
		repeatTest1(100);
	}
}
		
