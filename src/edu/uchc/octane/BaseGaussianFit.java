//FILE:          BaseGaussianFitting.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/26/13
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
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package edu.uchc.octane;

import java.util.Arrays;

import ij.plugin.filter.BackgroundSubtracter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Subpixel refiner by Gaussian fit.
 */
public abstract class BaseGaussianFit {

	protected int x0_,y0_; 
	protected double sigma2_;
	protected int windowSize_;
	protected double bg_ = 0;
	protected boolean bZeroBg_ = false;
	
	private float [] imageData_;
	private int width_;
	private int height_;
	
	public abstract double [] fit(); 
	public abstract double getValueExcludingBackground(int xi, int yi);	
	public abstract double getX(); 
	public abstract double getY(); 
	public abstract double getZ(); 
	public abstract double getH();
	public abstract double getE();
	
	public final static int backgroundFilterSize_ = 4;
	
	/**
	 * Default constructor
	 */
	public BaseGaussianFit() {
	}
	
	public void setImageData(ImageProcessor ip, boolean bPreProcessBackground) {

		Object pixels = ip.getPixels();
		width_ = ip.getWidth();
		height_ = ip.getHeight();
		imageData_ = new float[width_ * height_];

		if (pixels instanceof byte[]) {
			byte[] b = (byte[])pixels;
			for (int i = 0; i < b.length; i++) {
				imageData_[i] = (float) (b[i] & 0xff);  //assume unsigned
			}
		} else if (pixels instanceof short[]) {
			short [] b = (short [])pixels;
			for (int i = 0; i < b.length; i++) {
				imageData_[i] = (float) (b[i] & 0xffff); //assume unsigned
			}
		} else if (pixels instanceof float[]) {
			imageData_ = (float [])pixels;
		} else if (pixels instanceof int[]) {
			int [] p = (int []) pixels;
			for (int i = 0; i < p.length; i++) {
				int r = (p[i]&0xff0000)>>16;
				int g = (p[i]&0xff00)>>8;
				int b = p[i]&0xff;
				imageData_[i] = (float)(0.2126 * r + 0.7152 * g + 0.0722 * b);
			}
		}
		
		bZeroBg_ = bPreProcessBackground;
		if (bPreProcessBackground) {
			bg_ = 0;
			preProcessBackground();
		} else {
			bg_ = ip.getAutoThreshold();
		}
	}
	
	public void preProcessBackground() {

		float[] backgroundData;
		
		backgroundData = Arrays.copyOf(imageData_, imageData_.length);
		FloatProcessor fp = new FloatProcessor(width_, height_, backgroundData);
		
		BackgroundSubtracter bs = new BackgroundSubtracter();
		bs.rollingBallBackground(fp, backgroundFilterSize_, true, fp.isInvertedLut(), false, false, true);
		
		for (int i = 0; i < imageData_.length; i++) {
			imageData_[i] -= backgroundData[i];
		}
	}

	public void setupInitalValues(int x, int y, double sigma, int size) {
		sigma2_ = sigma * sigma * 2;
		windowSize_ = size;
		x0_ = (int) x;
		y0_ = (int) y;		
	}
	
	public void deflate() {
		for (int xi = - windowSize_; xi <= windowSize_; xi++) {
			for (int yi = - windowSize_; yi <= windowSize_; yi++) {
				imageData_[x0_ + xi + width_ * (y0_ + yi)] -= getValueExcludingBackground(xi, yi);
			}
		}
	}

	protected double pixelValue(int xi, int yi) {
		return imageData_[xi + x0_ + (yi + y0_) * width_];
	}
}
