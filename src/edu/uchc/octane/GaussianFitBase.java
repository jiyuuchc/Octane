//FILE:          GaussianFitBase.java
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

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.optim.PointValuePair;

import ij.IJ;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Base class for Gaussian fit modules.
 * @author Ji-Yu
 *
 */
public abstract class GaussianFitBase {

	protected int x0_,y0_; 
	protected int windowSize_;
	protected double bg_ = 0;
	protected boolean bPreprocessBg_ = false;
	protected boolean bDeflation_ = false;
	
	protected boolean bBgProcessed_ = false;
	
	PointValuePair pvp_;
	
	private float [] imageData_;
	private int width_;
	private int height_;
	
	/**
	 * Carry out the Gaussian fit 
	 * @return The fitting parameters
	 */
	public abstract double [] doFit(); 
	
	public double[] fit() {
		
		if (bPreprocessBg_) {
			preProcessBackground();
		}
		
		double [] ret = doFit();

		if (bDeflation_) {
			deflate();
		}
		
		return ret;
	}

	/**
	 * The value of the Gaussian fitting function at specified coordinate
	 * @param xi The X coordinate offset from the initial value
	 * @param yi The Y coordinate offset from the initial value
	 * @param point The rest of the fitting parameters
	 * @return The Gaussian function value 
	 */
	public abstract double getValueExcludingBackground(int xi, int yi, double [] point);	
	
	private final static int backgroundFilterSize_ = 40;
	
	/**
	 * Default constructor
	 */
	public GaussianFitBase() {
	}
	
	/**
	 * Assign image data to the module. 
	 * A copy of the image data is made and further operation will not alter the orginal image. 
	 * @param ip The image processor that will be analyzed
	 */
	public void setImageData(ImageProcessor ip) {

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
		
		bg_ = ip.getAutoThreshold();
		
		bBgProcessed_ = false;
	}
	
	
	/**
	 * Set whether to do background substraction first. 
	 * If yes, the imagedata is processed to remove background and bZeroBg_ is set to True;
	 * @param bPreProcessBackground Whether the background should be pre-substracted using rolling ball method.
	 */
	public void setPreprocessBackground(boolean bPreProcessBackground) {
		bPreprocessBg_ = bPreProcessBackground;
		
		if (bPreProcessBackground) {
		
			bg_ = 0;
			//preProcessBackground();
		}
	}

	/**
	 * Substract background from internal data storage using rolling ball method. 
	 * Currently the ball size is hard-coded to be 4 pixels.
	 */
	public void preProcessBackground() {

		if (! bBgProcessed_ && bPreprocessBg_) {
			float[] backgroundData;

			backgroundData = Arrays.copyOf(imageData_, imageData_.length);
			FloatProcessor fp = new FloatProcessor(width_, height_, backgroundData);

			BackgroundSubtracter bs = new BackgroundSubtracter();
			bs.rollingBallBackground(fp, backgroundFilterSize_, true, fp.isInvertedLut(), false, false, true);
			IJ.showProgress(1.0);

			for (int i = 0; i < imageData_.length; i++) {
				imageData_[i] -= backgroundData[i];
			}
			
			bBgProcessed_ = true;
		}
	}

	/**
	 * Define initial coordinates 
	 * @param x0 The x coordinate
	 * @param y0 The y coordinate
	 */
	public void setInitialCoordinates(int x0, int y0) {
		x0_ = (int) x0;
		y0_ = (int) y0;		
	}
	
	
	/**
	 * Set fitting window size
	 * @param size The size of the fitting rectangle is (2 * size + 1)   
	 */
	public void setWindowSize(int size) {
		windowSize_ = size;
	}

	/**
	 * Get fitting window size
	 * @return halfsize of the fitting rectangle   
	 */
	public int getWindowSize() {
		return windowSize_;
	}

	/**
	 * Set whether to deflate image after fitting
	 * @param b    
	 */
	public void setDeflation(boolean b) {
		bDeflation_ = b;
	}

	/**
	 * Get whether to deflate image after fitting 
	 * @return boolean
	 */
	public boolean getDeflation() {
		return bDeflation_;
	}

	/**
	 * Substract the value of the last fitting from the image   
	 */
	public void deflate() {
		for (int xi = - windowSize_; xi <= windowSize_; xi++) {
			for (int yi = - windowSize_; yi <= windowSize_; yi++) {
				imageData_[x0_ + xi + width_ * (y0_ + yi)] -= getValueExcludingBackground(xi, yi, pvp_.getPoint());
			}
		}
	}

	/**
	 * Get the pixel value of the image
	 * @param xi X coordinate
	 * @param yi Y coordinate
	 * @return The pixel value in double
	 */
	protected double pixelValue(int xi, int yi) {
		return imageData_[xi + x0_ + (yi + y0_) * width_];
	}
	
	/**
	 * Get the fitting result 
	 * @return X coordinate of the centroid
	 */
	public double getX() {
		return pvp_.getPoint()[0] + x0_;
	}

	/**
	 * Get the fitting result 
	 * @return Y coordinate of the centroid
	 */
	public double getY() {
		return pvp_.getPoint()[1] + y0_;
	}

	/**
	 * Get the fitting result 
	 * @return Z coordinate. Not all subclasses implement this
	 */
	public double getZ() {
		return 0;
	}

	/**
	 * Get the fitting result 
	 * @return Intensity of the spot 
	 */
	public double getH() {
		return pvp_.getPoint()[2];
	}

	/**
	 * Get the fitting result 
	 * @return Log Likelyhood of the fitting. Bigger value indicate higher confidence in fitting results.
	 */
	public double getE() {
		double m = 0;
		double m2 = 0;
		for (int xi = - windowSize_; xi <= windowSize_; xi++) {
			for (int yi = - windowSize_; yi <= windowSize_; yi++) {
				double v = pixelValue(xi, yi);
				m += v;
				m2 += v * v ;
 			} 
		}

		int nPixels = (1 + 2 * windowSize_)*(1 + 2 * windowSize_);
		m = m2 - m * m / nPixels; //variance of the grey values

		return nPixels * FastMath.log(m / pvp_.getValue());
	}
}
