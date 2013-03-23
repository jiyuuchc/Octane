package edu.uchc.octane.test;

import java.awt.Rectangle;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import edu.uchc.octane.GaussianFit;
import edu.uchc.octane.GaussianFit3DSimple;
import edu.uchc.octane.WatershedAnalysis;

public class ParticleAnalysisTest {
	
	static final int nRepeats = 10;

	//simulated data parameters
	static final int nParticles = 300;
	static final int imgSize = 256;
	static final int peakIntensity = 100;
	static final int bgIntensity = 40;
	static final double sigma2 = 2 * 0.8 * 0.8;

	//fitting parameters
	static final double sigma = 0.8;
	static final int kernelSize = 2;
	
	static ImagePlus showImg(String title, ImageProcessor ip) {
		ImagePlus imp = new ImagePlus(title, ip);
		imp.show();
		imp.getCanvas().zoomIn(0, 0);
		imp.getCanvas().zoomIn(0, 0);
		return imp;
	}
	
	public static void watershedTest1(WatershedAnalysis module) {
		ShortProcessor ip = new ShortProcessor(imgSize, imgSize);
		TestDataGenerator.randomMultipleSpots(ip, nParticles, sigma2, peakIntensity, bgIntensity);
		TestDataGenerator.addShotNoise(ip, 1);
		
		ImagePlus imp = showImg("Test Image", ip);
		showImg("Test Image", ip);

		module.setGaussianFitModule(new GaussianFit());
		module.setGaussianFitParameters(kernelSize, sigma, false, true);
		Rectangle mask = new Rectangle(0, 0, imgSize, imgSize);
		
		long start = System.currentTimeMillis();
		module.process(ip, mask, bgIntensity, peakIntensity / 6);
		long duration = System.currentTimeMillis() - start;

		imp.setRoi(module.createPointRoi());
		// n += module.reportNumParticles();
		System.out.println("Time lapsed: " + duration + " ms. Detected particles: " + module.reportNumParticles());
		// totalTime += duration;
	}
	
	public static void watershedTest2(WatershedAnalysis module) {
		ShortProcessor ip = new ShortProcessor(imgSize, imgSize);
		long totalTime = 0;
		int n = 0;
		for (int i = 0; i < nRepeats; i ++) {

			TestDataGenerator.randomMultipleSpots(ip, nParticles, sigma2, peakIntensity, bgIntensity);
			TestDataGenerator.addShotNoise(ip, 1);

			module.setGaussianFitModule(new GaussianFit());
			module.setGaussianFitParameters(kernelSize, sigma, false, true);
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

	public static void watershedTest3(WatershedAnalysis module) {
		ShortProcessor ip = new ShortProcessor(imgSize, imgSize);
		TestDataGenerator.randomMultipleSpots(ip, nParticles, sigma2, peakIntensity, bgIntensity);
		TestDataGenerator.addShotNoise(ip, 1);
		
		ImagePlus imp = showImg("Test Image", ip);
		showImg("Test Image", ip);

		GaussianFit3DSimple fitModule = new GaussianFit3DSimple();
		fitModule.setCalibrationValues(new double[] {0.7, 0, 0.018});

		module.setGaussianFitModule(fitModule);
		
		module.setGaussianFitParameters(kernelSize, sigma, false, true);
		Rectangle mask = new Rectangle(0, 0, imgSize, imgSize);
		
		long start = System.currentTimeMillis();
		module.process(ip, mask, bgIntensity, peakIntensity / 6);
		long duration = System.currentTimeMillis() - start;

		imp.setRoi(module.createPointRoi());
		// n += module.reportNumParticles();
		System.out.println("Time lapsed: " + duration + " ms. Detected particles: " + module.reportNumParticles());
		// totalTime += duration;
	}
	
	public static void main(String[] args){
		
		// WaterShed test
		WatershedAnalysis module = new WatershedAnalysis();
		
		watershedTest3(module);
		
		//watershedTest2(module);

	}
}
