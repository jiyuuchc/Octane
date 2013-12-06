package edu.uchc.octane.test;

import java.util.Arrays;

import ij.ImagePlus;
import ij.process.ShortProcessor;

public class TestDataGenerator {
	
	static double square(double x) {
		return x * x;
	}
	
	// approximate gaussian noise with 0 mean and stdev of 1 
	static private double randn() {
		
		return -3 + Math.random() * 2 + Math.random() *2 + Math.random() * 2;
	
	}
	
	static void addNoise(ShortProcessor ip, double noise) {
		
		short [] pixels = (short[])ip.getPixels();
		
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] += (short) (noise * randn() + 0.5);
		}
	}
	
	static void addShotNoise(ShortProcessor ip, double scale) {
		
		short [] pixels = (short[])ip.getPixels();
		
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] += (int)(Math.sqrt((double)pixels[i]) * scale * randn() + 0.5);
		}
	}
	
	static ShortProcessor gaussianImage(int halfSize, double xOffset, double yOffset, double sigma2, int height, int bg) {
	
		return astigmaticGaussianImage(halfSize, xOffset, yOffset, sigma2, sigma2, height, bg);
	
	}
	
	static ShortProcessor astigmaticGaussianImage ( 
			int halfSize, double xOffset, double yOffset, double sigmax2, double sigmay2, 
			int height, int bg) {

		ShortProcessor ip = new ShortProcessor( halfSize * 2 + 1, halfSize * 2 + 1);
		
		for (int xi = -halfSize; xi <= halfSize; xi ++) {
			for (int yi = -halfSize; yi <= halfSize; yi++) {
				
				double v = Math.exp( 
						- square( xi - xOffset ) / sigmax2 
						- square( yi - yOffset ) / sigmay2 ); 
				v = ( v * height / Math.sqrt(Math.sqrt(sigmax2 * sigmay2)) + bg);
			
				ip.set(xi + halfSize, yi + halfSize, (int) (v + 0.5) );
			}
		}
		
		return ip;
		
	}
	
	static double [][] randomMultipleSpots(ShortProcessor ip, int N, double sigma2, int height, int bg) {
		
		short [] pixels = (short [])ip.getPixels();
		
		Arrays.fill(pixels, (short)bg);
		
		double [][] positions = new double[2][];
		double [] xPositions = new double[N];
		double [] yPositions = new double[N];
		
		positions[0] = xPositions;
		positions[1] = yPositions;
		          
		int halfSize = Math.max((int)(Math.sqrt(sigma2/2) * 4 + 0.5), 2);
		
		for (int i =0; i < N; i++) {
			
			double x = (Math.random() * (ip.getWidth() - halfSize * 2)) + halfSize;
			double y = (Math.random() * (ip.getHeight() - halfSize * 2)) + halfSize;
			
			int x0 = (int)(x + .5 );
			int y0 = (int)(y + .5);
			
			xPositions[i] = x;
			yPositions[i] = y;
			
			for (int xi = -halfSize; xi <= halfSize; xi ++) {
				for (int yi = -halfSize; yi <= halfSize; yi++) {
			
					int x1 = x0 + xi;
					int y1 = y0 + yi;
					
					if (x1 >= 0 && x1 < ip.getWidth()&& y1 >= 0 && y1 < ip.getHeight()) {
						
						double v = Math.exp( -square(x1 - x) / sigma2  - square(y1 - y ) / sigma2 );
						v = (v * height);
						
						ip.set(x1, y1, ip.get(x1,y1) + (int) (v + 0.5) );
					
					}
				}
			}
		}
		return positions;
	}
	
	static double [][] randomAstigmaticMultipleSpots(ShortProcessor ip, int N, double sigma2, int height, int bg) {
		
		short [] pixels = (short [])ip.getPixels();
		
		Arrays.fill(pixels, (short)bg);
		
		double [][] parameters = new double[4][];
		double [] xPositions = new double[N];
		double [] yPositions = new double[N];
		double [] xSigmas = new double[N];
		double [] ySigmas = new double[N];
		
		parameters[0] = xPositions;
		parameters[1] = yPositions;
		parameters[2] = xSigmas;
		parameters[3] = ySigmas;

		int halfSize = Math.max((int)(Math.sqrt(sigma2/2) * 4 + 0.5), 2);
		
		for (int i =0; i < N; i++) {
			
			double x = (Math.random() * (ip.getWidth() - halfSize * 2)) + halfSize;
			double y = (Math.random() * (ip.getHeight() - halfSize * 2)) + halfSize;
			double z = Math.random();
			double sx = sigma2 * square( 1 + z * z);
			double sy = sigma2 * square( 1 + (1-z) * (1-z));
			
			int x0 = (int)(x + .5 );
			int y0 = (int)(y + .5);
			
			xPositions[i] = x;
			yPositions[i] = y;
			
			for (int xi = -halfSize; xi <= halfSize; xi ++) {
				for (int yi = -halfSize; yi <= halfSize; yi++) {
			
					int x1 = x0 + xi;
					int y1 = y0 + yi;
					
					if (x1 >= 0 && x1 < ip.getWidth()&& y1 >= 0 && y1 < ip.getHeight()) {
						
						double v = Math.exp( -square(x1 - x) / sx  - square(y1 - y ) / sy );
						v = (v * height);
						
						ip.set(x1, y1, ip.get(x1,y1) + (int) (v / Math.sqrt(Math.sqrt(sx * sy)) + 0.5) );
					
					}
				}
			}
		}
		return parameters;
	}

	public static void main(String[] args){
		
		ShortProcessor ip = new ShortProcessor(256,256);
		double [][] p = randomAstigmaticMultipleSpots(ip, 100, 0.8 * 0.8 * 2, 100, 50);
		addShotNoise(ip,1);
		
		ImagePlus imp = new ImagePlus("TestImage", ip);
		imp.show();
		
		imp.getCanvas().zoomIn(0,0);
		imp.getCanvas().zoomIn(0,0);
	}

}

