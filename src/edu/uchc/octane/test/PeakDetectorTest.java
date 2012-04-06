package edu.uchc.octane.test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;
import ij.process.ShortProcessor;

import edu.uchc.octane.PeakFinder;

public class PeakDetectorTest {

	static void test_one() throws IOException {
		PeakFinder finder = new PeakFinder();
		
		int size = 10;
		double sigma = 2.0;
		double height = 200;
		double bg = 20;
		
		double tol = 20;
		finder.setTolerance(tol);
		
		ShortProcessor p = new ShortProcessor(size,size);
		ShortProcessor p0 = new ShortProcessor(size,size);
		double x0 = size / 2 - .5;
		double y0 = size / 2 - .5;
		double sigma2 = 2 * sigma * sigma;
		Random g = new Random();

		double v;
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y ++) {
				v = bg + height * Math.exp(-((x - x0)*(x-x0) + (y-y0)*(y-y0))/sigma2);
				p0.setf(x,y,(float)v);
			}
		}

		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(System.out));
		w.write("Single peak test\n");

		for (int i = 0; i < 100; i ++) {
			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y ++) {
					v = p0.getf(x,y);
					v = v + g.nextGaussian()*Math.sqrt(v); // shot noise
					p.setf(x,y,(float)v);
					w.write(" " + (short) v + "\t");
				}
				w.write("\n");
			}
			
			finder.setImageProcessor(p);
			finder.findMaxima();

			finder.exportMaxima(w, 0);
			w.flush();
		}
	}
	
	public static void main(String[] args) throws IOException {
		test_one();
	}
}