//FILE:          PeakFinder.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 2/16/08
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

import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Vector;

public class PeakFinder {
	private double tol_;
	private int[] dirOffset_;
	private int[] dirXoffset_;
	private int[] dirYoffset_;
	final static byte LISTED = (byte) 1;
	final static byte OWNED = (byte) 2;
	final static byte MAX = (byte) 4;
	private ImageProcessor ip_;
	private int width_;
	private int height_;
	private double minThreshold_, maxThreshold_;
	private Roi roi_;
	private SubPixelRefiner refiner_;
	private double[] xArray_;
	private double[] yArray_;
	private int[] peakSize_;
	private double[] residue_;
	private short nMaxima_;

	public void setRoi(Roi roi) {
		roi_ = roi;
	}

	class Pixel implements Comparable<Pixel> {
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

	public PeakFinder() {
		tol_ = 2000;
		refiner_ = null;
	}

	public void setImageProcessor(ImageProcessor ip) {
		setImageProcessor(ip, false);
	}

	public void setImageProcessor(ImageProcessor ip, boolean bSmooth) {
		ip_ = ip.duplicate();
		if (bSmooth)
			ip_.smooth();
		width_ = ip_.getWidth();
		height_ = ip_.getHeight();
		makeDirectionOffsets();
	}

	public double getTolerance() {
		return tol_;
	}

	public void setTolerance(double tol) {
		tol_ = tol;
	}

	public void setRefiner(SubPixelRefiner refiner) {
		refiner_ = refiner;
	}

	// The followings are copied form ImageJ
	private void makeDirectionOffsets() {
		dirOffset_ = new int[] { -width_, -width_ + 1, +1, +width_ + 1, +width_, +width_ - 1, -1, -width_ - 1 };
		dirXoffset_ = new int[] { 0, 1, 1, 1, 0, -1, -1, -1 };
		dirYoffset_ = new int[] { -1, -1, 0, 1, 1, 1, 0, -1 };
	}

	public int findMaxima() {
		Rectangle bbox;
		if (roi_ != null) {
			bbox = roi_.getBounds();
		} else {
			bbox = ip_.getRoi();
		}
		// make sure the roi does not include edges
		if (bbox.x == 0) {
			bbox.x++;
			bbox.width--;
		}
		if (bbox.y == 0) {
			bbox.y++;
			bbox.height--;
		}
		if (bbox.width + bbox.x == width_) {
			bbox.width--;
		}
		if (bbox.height + bbox.y == height_) {
			bbox.height--;
		}

		Pixel [] pixels = new Pixel[bbox.height * bbox.width];
		float globalMin = Float.MAX_VALUE;
		float globalMax = -Float.MAX_VALUE;
		int idx = 0;
		for (int y = bbox.y; y < bbox.y + bbox.height; y++) {
			for (int x = bbox.x; x < bbox.x + bbox.width; x++) {
				float v = ip_.getPixelValue(x, y);
				if (globalMin > v) {
					globalMin = v;
				}
				if (globalMax < v) {
					globalMax = v;
				}
				pixels[idx ++ ] = new Pixel(x,y,v); 
			}
		}
		Arrays.sort(pixels);

		minThreshold_ = ip_.getMinThreshold();
		if (minThreshold_ == ImageProcessor.NO_THRESHOLD) {
			minThreshold_ = -Float.MAX_VALUE;
		}
		maxThreshold_ = ip_.getMaxThreshold();
		if (maxThreshold_ == ImageProcessor.NO_THRESHOLD) {
			maxThreshold_ = Float.MAX_VALUE;
		}

		nMaxima_ = 0;
		xArray_ = new double[pixels.length];
		yArray_ = new double[pixels.length];
		peakSize_ = new int[pixels.length];
		residue_ = new double[pixels.length];
		return analyzeMaxima(pixels);
	} 

	private short analyzeMaxima(Pixel[] pixels) {
		byte[] pixelStates = new byte[width_ * height_];

		int[] xList = new int[width_ * height_]; // here we enter points
		int[] yList = new int[width_ * height_];

		nMaxima_ = 0;

		for (int i = pixels.length - 1; i >= 0; i--) {
			double v = pixels[i].value;

			int offset = pixels[i].x + width_ * pixels[i].y;
			if ((pixelStates[offset] & OWNED) != 0) {
				continue;
			}

			xList[0] = pixels[i].x;
			yList[0] = pixels[i].y;
			pixelStates[offset] |= LISTED;
			int listLen = 1;
			int listCurPos = 0;
			int listCurEnd = listLen;
			boolean isMax = true;
			do {
				while (listCurPos < listCurEnd) {
					offset = xList[listCurPos] + width_ * yList[listCurPos];
					for (int d = 0; d < 8; d++) { // analyze all neighbors (in 8 directions) at the same level
						int offset2 = offset + dirOffset_[d];
						if (! isDirAllowed(xList[listCurPos], yList[listCurPos], d)) {
							continue;
						}
						if ((pixelStates[offset2] & OWNED) != 0) { //conflict
							isMax = false; 
							break;
						}
						if ((pixelStates[offset2] & LISTED) != 0) { //already listed
							continue;
						}
						int x2 = xList[listCurPos] + dirXoffset_[d];
						int y2 = yList[listCurPos] + dirYoffset_[d];
						float v2 = ip_.getPixelValue(x2, y2);
						if (v2 >= v - tol_) {// we have found a new point within the tolerance
							xList[listLen] = x2;
							yList[listLen] = y2;
							listLen++; 
							pixelStates[offset2] = LISTED;
						}
					} // for directions d
					listCurPos++;
				} // while listCurPose < listCurEnd
				listCurEnd = listLen;
			} while (listCurPos < listLen - Prefs.sloppyness_ );

			for (listCurPos = 0; listCurPos < listLen; listCurPos++) {
				offset = xList[listCurPos] + width_ * yList[listCurPos];
				pixelStates[offset] = OWNED; // mark as processed
			} 

			if (isMax && v < maxThreshold_ && v > minThreshold_) {
				if (roi_ == null || roi_.contains(pixels[i].x, pixels[i].y)) {
					xArray_[nMaxima_] = pixels[i].x;
					yArray_[nMaxima_] = pixels[i].y;
					peakSize_[nMaxima_] = listLen;
					nMaxima_++;
				}
			}
		}

		return nMaxima_;
	}

	public short refineMaxima() {
		if (refiner_ == null ) {
			switch (Prefs.refiner_) {
			case 0: 
				refiner_ = new PFGWRefiner2(ip_);
				break;
			case 1:
				refiner_ = new GaussianRefiner(ip_);
				break;
			case 2:
				refiner_ = new ZeroBgGaussianRefiner(ip_);
				break;
			}
		}

		short nMissed = 0;
		short nNewMaxima = 0;
		residue_ = new double[nMaxima_];
		if (nMaxima_ > 0 && Prefs.refinePeak_ ) {
			for (int i = 0; i < nMaxima_; i++) {
				int rtmp;
				rtmp = refiner_.refine(xArray_[i], yArray_[i]);
				if (rtmp >= 0) {
					xArray_[nNewMaxima] = refiner_.getXOut();
					yArray_[nNewMaxima] = refiner_.getYOut();
					residue_[nNewMaxima] = refiner_.getResidue();
					peakSize_[nNewMaxima] = (int)Math.round(refiner_.getHeightOut());
					nNewMaxima++;
				}else {
					nMissed++;
				}
			}
		} else if (nMaxima_ > 0) {
			nNewMaxima = nMaxima_;
		}
		assert (nMissed + nNewMaxima == nMaxima_);
		nMaxima_ = nNewMaxima;
		return nMissed;
	}

	public Roi markMaxima() {
		if (nMaxima_ > 0) {
			int[] xpoints = new int[nMaxima_];
			int[] ypoints = new int[nMaxima_];
			for (int i = 0; i < nMaxima_; i++) {
				xpoints[i] = (int) Math.round(xArray_[i]);
				ypoints[i] = (int) Math.round(yArray_[i]);
			}
			return new PointRoi(xpoints, ypoints, nMaxima_);
		} else {
			return null;
		}
	}

	public void exportCurrentMaxima(Writer writer, int frame) throws IOException {
		if (nMaxima_ > 0) {
			for (int i = 0; i < nMaxima_; i++) {
				double x = xArray_[i];
				double y = yArray_[i];
				writer.write(x + ", " + y +  ", " + frame + "," + peakSize_[i] + "," + residue_[i] + '\n');
			}
		}
	}

	public SmNode[] getCurrentNodes(int frame) {
		SmNode [] nodes;
		nodes = new SmNode[nMaxima_];
		for ( int i = 0; i < nMaxima_; i ++) {
			nodes[i] = new SmNode(xArray_[i], yArray_[i], frame, residue_[i]);
		}
		return nodes;
	}

	private boolean isDirAllowed(int x, int y, int direction) {
		int xmax = width_ - 1;
		int ymax = height_ - 1;
		switch (direction) {
		case 0:
			return (y > 0);
		case 1:
			return (x < xmax && y > 0);
		case 2:
			return (x < xmax);
		case 3:
			return (x < xmax && y < ymax);
		case 4:
			return (y < ymax);
		case 5:
			return (x > 0 && y < ymax);
		case 6:
			return (x > 0);
		case 7:
			return (x > 0 && y > 0);
		}
		return false; // to make the compiler happy :-)
	}
}
