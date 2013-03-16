package edu.uchc.octane.test;

import ij.process.ShortProcessor;

public class TestDataGenerator {
	
	/* approximate gaussian noise with 0 mean and stdev of 1 */ 
	static private double randn() {
		return -3 + Math.random() * 2 + Math.random() *2 + Math.random() * 2;
	}
	
	static void addNoise(ShortProcessor ip, double noise) {
		short [] pixels = (short[])ip.getPixels();
		
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] += (int) (noise * randn() + 0.5);
		}
	}
	
	static void addShotNoise(ShortProcessor ip, double scale) {
		short [] pixels = (short[])ip.getPixels();
		
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] += (int)(Math.sqrt((double)pixels[i]) * scale * randn() + 0.5);
		}
	}
	
	static ShortProcessor gaussianImage(int halfSize, double xOffset, double yOffset, double sigma2, int height, int bg) {
		ShortProcessor ip = new ShortProcessor(halfSize*2 + 1, halfSize * 2 + 1);
		for (int xi = -halfSize; xi <= halfSize; xi ++) {
			for (int yi = -halfSize; yi <= halfSize; yi++) {
				double v = Math.exp( -((- xOffset + xi) * (- xOffset + xi) + (- yOffset + yi)*(- yOffset + yi))/ sigma2 );
				v = (v * height + bg);
				ip.set(xi + halfSize, yi + halfSize, (int) (v + 0.5) );
			}
		}
		return ip;
	}
	
	static ShortProcessor noisyGaussianImage(int halfSize, double xOffset, double yOffset, double sigma2, int height, int bg, int noise) {
		ShortProcessor ip = new ShortProcessor(halfSize*2 + 1, halfSize * 2 + 1);
		for (int xi = -halfSize; xi <= halfSize; xi ++) {
			for (int yi = -halfSize; yi <= halfSize; yi++) {
				double v = Math.exp( -((- xOffset + xi) * (- xOffset + xi) + (- yOffset + yi)*(- yOffset + yi))/ sigma2 );
				v = (v * height + bg) * (1 + noise * randn());
				ip.set(xi + halfSize, yi + halfSize, (int) (v + 0.5) );
			}
		}
		return ip;
	}
	
	static ShortProcessor randomMultipleSpots(int size, int N, double sigma2, int height, int bg) {
		ShortProcessor ip = new ShortProcessor(size,size);
		int halfSize = Math.max((int)(Math.sqrt(sigma2) * 3 + 0.5), 2);
		
		for (int i =0; i < N; i++) {
			double x = (int)(Math.random() * size);
			double y = (int)(Math.random() * size);
			int x0 = (int)(x + 0.5);
			int y0 = (int)(y + 0.5);
			for (int xi = -halfSize; xi <= halfSize; xi ++) {
				for (int yi = -halfSize; yi <= halfSize; yi++) {
					int x1 = x0 + xi;
					int y1 = y0 + yi;
					if (x1 >= 0 && x1 < size && y1 >= 0 && y1 < size) { 
						double v = Math.exp( -((x1 - x) * (x1 - x) + (y1 - y )*(y1 - y)) / sigma2 );
						v = (v * height + bg);
						ip.set(x1, y1, (int) (v + 0.5) );
					}
				}
			}
		}
		return ip;
	}

}

