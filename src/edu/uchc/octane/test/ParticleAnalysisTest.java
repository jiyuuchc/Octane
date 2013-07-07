package edu.uchc.octane.test;

import java.awt.Rectangle;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import edu.uchc.octane.GaussianFit2D;
import edu.uchc.octane.GaussianFit3DSimple;
import edu.uchc.octane.ParticleAnalysis;

public class ParticleAnalysisTest {
	
	static final int nRepeats = 10;

	//simulated data parameters
	static final int nParticles = 100;
	static final int imgSize = 256;
	static final int peakIntensity = 100;
	static final int bgIntensity = 10;
	static final double sigma2 = 2;

	//fitting parameters
	static final double sigma = 1;
	static final int kernelSize = 2;
	
	static ImagePlus showImg(String title, ImageProcessor ip) {
		ImagePlus imp = new ImagePlus(title, ip);
		imp.show();
		imp.getCanvas().zoomIn(0, 0);
		imp.getCanvas().zoomIn(0, 0);
		return imp;
	}
	
	public static void watershedTest1(ParticleAnalysis module) throws InterruptedException {
		ShortProcessor ip = new ShortProcessor(imgSize, imgSize);
		TestDataGenerator.randomMultipleSpots(ip, nParticles, sigma2, peakIntensity, bgIntensity);
		TestDataGenerator.addShotNoise(ip, 1);
		
		ImagePlus imp = showImg("Test Image", ip);
		showImg("Test Image", ip);

		GaussianFit2D fittingModule = new GaussianFit2D();
		fittingModule.setWindowSize(kernelSize);
		fittingModule.setPreprocessBackground(false);
		fittingModule.setDeflation(true);
		fittingModule.setPreferredSigmaValue(sigma);
		
		module.setGaussianFitModule(fittingModule);

		Rectangle mask = new Rectangle(0, 0, imgSize, imgSize);
		
		long start = System.currentTimeMillis();
		module.process(ip, mask, bgIntensity, peakIntensity / 6);
		long duration = System.currentTimeMillis() - start;

		imp.setRoi(module.createPointRoi());
		
		System.out.println("Time lapsed: " + duration + " ms. Detected particles: " + module.reportNumParticles());

	}
	
	public static void watershedTest2(ParticleAnalysis module) throws InterruptedException {
		ShortProcessor ip = new ShortProcessor(imgSize, imgSize);
		long totalTime = 0;
		int n = 0;
		for (int i = 0; i < nRepeats; i ++) {

			TestDataGenerator.randomMultipleSpots(ip, nParticles, sigma2, peakIntensity, bgIntensity);
			TestDataGenerator.addShotNoise(ip, 1);

			GaussianFit2D fittingModule = new GaussianFit2D();
			fittingModule.setWindowSize(kernelSize);
			fittingModule.setPreprocessBackground(false);
			fittingModule.setDeflation(true);
			fittingModule.setPreferredSigmaValue(sigma);
			
			module.setGaussianFitModule(fittingModule);
			
			Rectangle mask = new Rectangle(0, 0, imgSize, imgSize);

			long start = System.currentTimeMillis();
			module.process(ip, mask, bgIntensity, peakIntensity / 6);
			long duration = System.currentTimeMillis() - start;

			n += module.reportNumParticles();
			//System.out.println("Time lapsed: " + duration + " ms. Detected particles: " + module.reportNumParticles());
			totalTime += duration;
		}
		
		System.out.println("Average time: " + (totalTime / nRepeats) + " ms");		
		System.out.println("Average detection%: " + (n * 100.0 / nParticles / nRepeats) + "%");
		
	}

	public static void watershedTest3(ParticleAnalysis module) throws InterruptedException {
		ShortProcessor ip = new ShortProcessor(imgSize, imgSize);
		TestDataGenerator.randomMultipleSpots(ip, nParticles, sigma2, peakIntensity, bgIntensity);
		TestDataGenerator.addShotNoise(ip, 1);

		ImagePlus imp = showImg("Test Image", ip);
		showImg("Test Image", ip);

		GaussianFit3DSimple fittingModule = new GaussianFit3DSimple();
		fittingModule.setCalibrationValues(new double[] {1, 0, 0.3});

		fittingModule.setWindowSize(kernelSize);
		fittingModule.setPreprocessBackground(true);
		fittingModule.setDeflation(true);
		fittingModule.setPreferredSigmaValue(sigma);
		
		module.setGaussianFitModule(fittingModule);
		
		Rectangle mask = new Rectangle(0, 0, imgSize, imgSize);
		
		long start = System.currentTimeMillis();
		module.process(ip, mask, bgIntensity, peakIntensity / 6);
		long duration = System.currentTimeMillis() - start;

		imp.setRoi(module.createPointRoi());

		double [] x = module.reportX();
		double [] y = module.reportY();
		double [] z = module.reportZ();
		double [] e = module.reportE();
		for (int i = 0; i < module.reportNumParticles(); i++) {
			System.out.format("%d - X: %6.2f\tY: %6.2f \tZ: %6.2f\tE: %6.1f%n", i, x[i],y[i],z[i],e[i]);
		}
		
		System.out.println("Time lapsed: " + duration + " ms.");
		System.out.println("Detected " + module.reportNumParticles() + " out of " + nParticles + " particles: ");
		
		System.out.println("Mean Z value: " + StatUtils.mean(z, 0, module.reportNumParticles()));
		System.out.println("Std Z: " + FastMath.sqrt(StatUtils.variance(z, 0, module.reportNumParticles())));
	}
	
	public static void main(String[] args){
		
		// WaterShed test
		ParticleAnalysis module = new ParticleAnalysis();
		
		try {
			watershedTest3(module);
		} catch (InterruptedException e) {
//			e.printStackTrace();
		}
		
		//watershedTest2(module);

	}
}
