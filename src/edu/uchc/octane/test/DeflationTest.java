package edu.uchc.octane.test;

import java.awt.Rectangle;
import java.util.Arrays;

import edu.uchc.octane.DeflationAnalysis;
import edu.uchc.octane.GaussianSPL;
import edu.uchc.octane.Prefs;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class DeflationTest {
	private class Pixel implements Comparable<Pixel> {
		
		public float value;
		public int x;
		public int y;

		Pixel(int x, int y, float value) {
			this.x = x;
			this.y = y;
			this.value = value;
		}
		
		public int compareTo(Pixel o) {
			return Float.compare(value,o.value); 
		}
	}
	
	public static void main(String[] args) {
		DeflationTest test = new DeflationTest();
		ImagePlus imp = ij.IJ.openImage(args[0]);
		//imp.setRoi(new PointRoi(17,51));
		imp.show();
		ImageProcessor ip = imp.getProcessor();

		Prefs.loadPrefs();
		Prefs.sigma_ = 1.0;
		Rectangle bbox = ip.getRoi();
		bbox.x = Prefs.kernelSize_;
		bbox.width = bbox.width - 2 * Prefs.kernelSize_;
		bbox.y = Prefs.kernelSize_;
		bbox.height = bbox.height - 2 * Prefs.kernelSize_;

		Pixel [] pixels = new Pixel[bbox.height * bbox.width];
		float globalMin = Float.MAX_VALUE;
		float globalMax = -Float.MAX_VALUE;
		int idx = 0;
		for (int y = bbox.y; y < bbox.y + bbox.height; y++) {
			for (int x = bbox.x; x < bbox.x + bbox.width; x++) {
				float v = ip.getPixelValue(x, y);
				if (globalMin > v) {
					globalMin = v;
				}
				if (globalMax < v) {
					globalMax = v;
				}
				pixels[idx ++ ] = test.new Pixel(x,y,v);
			}
		}
		Arrays.sort(pixels);
		ImageStack stack = new ImageStack(ip.getWidth(), ip.getHeight());
		GaussianSPL spl = new GaussianSPL(true);
		boolean [] labeled = new boolean[ip.getWidth() * ip.getHeight()];
		Arrays.fill(labeled, false);
		idx = pixels.length - 1;
		for (int i = 0; i < 100; i++) {
			ImageProcessor ip2 = ip.duplicate();
			spl.setImageData(ip2);
			int x,y;
			while (true) {
				Pixel p = pixels[idx--];
				x = p.x;
				y = p.y;
				if (!labeled[y * ip.getWidth() + x] ) {
					break;
				}
			}
			if ( spl.refine((double)x,(double)y) != 0 ) {
				IJ.showMessage("Fitting error at:" + i + "X:" + x + "Y:" + y);
			} else {
				spl.deflate();
				stack.addSlice("" + i, ip2);
				ip = ip2;
			}
			for (int xi = - Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi ++) {
				for (int yi = - Prefs.kernelSize_; yi < Prefs.kernelSize_; yi ++) {
					labeled[ x + xi + (y + yi) * ip.getWidth()] = true;
				}
			}
		}
		
		ImagePlus nimp = new ImagePlus("deflation", stack);
		nimp.show();
		nimp.getCanvas().zoomIn(0,0);
		nimp.getCanvas().zoomIn(0,0);
	}
}
