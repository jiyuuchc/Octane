//FILE:          DeflationAnalysis.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/30/13
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES./**
//

package edu.uchc.octane;

import java.awt.Rectangle;
import java.util.Arrays;

import ij.process.ImageProcessor;

/**
 * A simple particle analysis module that simply analyze all pixels sorted on intensity.
 * 
 * @author Ji-Yu
 *
 */
public class DeflationAnalysis extends ParticleAnalysis {

	class Pixel implements Comparable<Pixel> {
		
		public int value;
		public int x;
		public int y;

		Pixel(int x, int y, int value) {
			this.x = x;
			this.y = y;
			this.value = value;
		}
		
		public int compareTo(Pixel o) {
			return value - o.value; 
		}
	}

	/**
	 * Analyze the image
	 * @param ip The image to be analyzed 
	 * @param mask A region of the image. Pixels outside are ignored
	 * @param kernelSize The size of a rectangle for Gaussian fitting 
	 * @param sigma The sigma width of the point spread function 
	 * @param threshold Lowest pixel intensity to be analyzed 
	 * @param bZeroBackground Whether the background should be subtracted before fitting 
	 */
	public void process(ImageProcessor ip, Rectangle mask, int kernelSize, double sigma, int threshold, boolean bZeroBackground) {
		Rectangle bbox = new Rectangle(kernelSize, kernelSize, ip.getWidth() - 2 * kernelSize, ip.getHeight() - 2 * kernelSize);
		bbox = (Rectangle) mask.createIntersection(bbox);

		Pixel [] pixels = new Pixel[bbox.height * bbox.width];
		int idx = 0;
		for (int y = bbox.y; y < bbox.y + bbox.height; y++) {
			for (int x = bbox.x; x < bbox.x + bbox.width; x++) {
				int v = ip.getPixel(x, y);
				if (v > threshold) {
					pixels[idx ++ ] = new Pixel(x,y,v);
				}
			}
		}
		Arrays.sort(pixels, 0, idx);

		GaussianFit spl = new GaussianFit();

		boolean [] labeled = new boolean[ip.getWidth() * ip.getHeight()];
		spl.setImageData(ip, bZeroBackground);

		nParticles_ = 0;
		x_ = new double[idx];
		y_ = new double[idx];
		h_ = new double[idx];
		e_ = new double[idx];
		
		idx--;
		while (idx >= 0) {
			int x = 0 ,y = 0;

			while (idx >= 0) {
				Pixel p = pixels[idx--];
				x = p.x;
				y = p.y;
				if (!labeled[y * ip.getWidth() + x] ) {
					break;
				}
			}

			if (idx >= 0 ) {
				spl.setFittingRegion(x, y, kernelSize);
				spl.setPreferredSigmaValue(sigma);
				double [] ret = spl.fit();
				if ( ret == null ) {
					System.out.println("Fitting error at: " + idx + " X:" + x + ", Y:" + y + " returns:"+ ret);
				} else {
					x_[nParticles_] = spl.getX();
					y_[nParticles_] = spl.getY();
					h_[nParticles_] = spl.getH();
					e_[nParticles_] = spl.getE();
					nParticles_ ++;
					spl.deflate();
				}
				for (int xi = - kernelSize; xi <= kernelSize; xi ++) {
					for (int yi = - kernelSize; yi < kernelSize; yi ++) {
						labeled[ x + xi + (y + yi) * ip.getWidth()] = true;
					}
				}
			}
		}
	}
}
