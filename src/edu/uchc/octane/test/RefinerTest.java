package edu.uchc.octane.test;

import java.io.IOException;
import java.io.OutputStreamWriter;

import edu.uchc.octane.GaussianResolver;
import edu.uchc.octane.NelderMeadResolver;
import edu.uchc.octane.PFGWResolver;
import edu.uchc.octane.PeakFinder;
import edu.uchc.octane.SubPixelResolver;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class RefinerTest {
	final static int size = 5;
	final static int height = 500;
	final static int bg = 50;
	final static double sigma2 = 1.8;

	static SubPixelResolver refiner;

	static double noise;

	static double randn() {
		return -3 + Math.random() * 2 + Math.random() *2 + Math.random() * 2;
	}

	static ShortProcessor noisyGaussianImage(double x, double y) {
		ShortProcessor ip = new ShortProcessor(size*2 + 1, size * 2 + 1);
		for (int xi = -size; xi <= size; xi ++) {
			for (int yi = -size; yi <= size; yi++) {
				double v = Math.exp( -((- x + xi) * (- x + xi) + (- y + yi)*(- y + yi))/sigma2 );
				v = (v * height + bg) * (1 + noise * randn());
				ip.set(xi + size, yi + size, (int) (v ) );
			}
		}

		return ip;
	}

	static void refineTest() {
		System.out.println("Refining test.");
		for (int n = 0; n < 4; n++) {
			noise = 0.1 * n;
			System.out.printf("Noise:%d %%\n", n * 10);
			for (int i = 0; i < 5; i++) {
				for ( double y = -.5 ; y <= .5; y+= 0.1 ) {
					for (double x = -.5; x <= .5; x+= 0.1 ) {
						refiner.setImageData(noisyGaussianImage(x,y));
						int ret = refiner.refine(size +.5 , size +.5);
						if (ret >=0 ) {
							System.out.printf("%+.3f,%+.3f\t", refiner.getX(), refiner.getY());
						} else {
							System.out.printf("xxxxxx %6d\t", ret);
						}
					}
					System.out.println();
				}
				System.out.println();
			}
		}		
	}

	static void initValueTest() {
		System.out.println("Initial value test.");
		for (int n = 0; n < 5; n++) {
			noise = 0.1 * n;
			System.out.printf("Noise:%d %%\n", n * 10);
			for (int i = 0; i < 5; i++) {
				refiner.setImageData(noisyGaussianImage(0,0));

				for ( double y = -1 ; y <= 1; y+= 0.5 ) {
					for (double x = -1; x <= 1; x+= 0.5 ) {
						int ret = refiner.refine(x + size +.5 , y + size +.5);
						if (ret >=0 ) {
							System.out.printf("%+.3f,%+.3f\t", refiner.getX(), refiner.getY());
						} else {
							System.out.printf("xxxxxx %6d\t", ret);
						}
					}
					System.out.println();
				}
				System.out.println();
			}
		}
	}
	
	static void searchFromPeakTest() {
		System.out.println("Search from peak test.");
		for (int n = 2; n < 5; n++) {
			noise = 0.1 * n;
			System.out.printf("Noise:%d %%\n", n * 10);
			for (int i = 0; i < 100; i++) {
				ShortProcessor ip = noisyGaussianImage(0,0);
				refiner.setImageData(ip);
				int max = -1;
				int xi = 0,yi = 0;
				for ( int x = 0 ; x < ip.getWidth(); x+= 1 ) {
					for (int y = 0; y < ip.getHeight(); y+= 1 ) {
						if ( max < ip.get(x,y) ){
							max = ip.get(x,y);
							xi = x; yi = y;
						}
					}
				}

				int ret = refiner.refine(.5 + xi , .5 + yi);
				if (ret >=0 ) {
					System.out.printf("%+.3f,%+.3f | ", refiner.getX(), refiner.getY());
				} else {
					System.out.printf("xxxxxx %6d | ", ret);
				}
				
				if (xi != size || yi != size) {
					ret = refiner.refine(.5 + size , .5 + size);
					if (ret >=0 ) {
						System.out.printf("%+.3f,%+.3f\t", refiner.getX(), refiner.getY());
					} else {
						System.out.printf("xxxxxx %6d\t", ret);
					}
				} else {
					System.out.print("------ ------\t");
					
				}

				if ((i+1)%5 == 0) {
					System.out.println();
				}
			}
			System.out.println();
		}
	}

	public static void main(String[] args) {
		noise = 0;
		refiner = new NelderMeadResolver();

//		initValueTest();
		
		searchFromPeakTest();
		
//		refineTest();
	}
}
