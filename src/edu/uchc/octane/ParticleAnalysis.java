//FILE:          ParticleAnalysis.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 2/16/08
//
// LICENSE:      This file is distributed under the BSD license.
//	               License text is included with the source distribution.
//
//	               This file is distributed in the hope that it will be useful,
//	               but WITHOUT ANY WARRANTY; without even the implied warranty
//	               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//	               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//	               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//	               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES./**
//

package edu.uchc.octane;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ij.gui.PointRoi;
import ij.process.ImageProcessor;

/**
 * A particle analysis module that uses watershed algrithm to detect particles and then use Gaussian
 * fitting to determine centroids.
 * @author Ji-Yu
 *
 */
public class ParticleAnalysis{

	double [] x_;
	double [] y_;
	double [] z_;
	double [] e_;
	double [] h_;
	int nParticles_;
	
	GaussianFitBase g_ = null;
	
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
			return - value + o.value;  // this is reverse order
		}
	}
	
	private int width_;
	private int height_;

	private final static int PROCESSED = 1;
	private final static int FLOODED = 2;

	class FloodState {
		int [] labels_;
		
		public FloodState(int w, int h) {
			labels_ = new int[w * h];
		}
		
		public boolean isProcessed(int index) {
			return ( ( labels_[index] & PROCESSED ) != 0);
		}
		
		public boolean isFlooded(int index) {
			return ( ( labels_[index] & FLOODED) != 0);
		}

		public void process(int index) {
			labels_[index] |= PROCESSED;
		}
		
		public void flood(int index) {
			labels_[index] |= FLOODED;
		}
		
		public void floodBorders(Rectangle rect) {
			int index1 = (rect.y - 1) * width_ + rect.x - 1;
			Arrays.fill(labels_, index1, index1 + rect.width + 2, FLOODED);
			for (int i = 0; i < rect.height; i ++ ) {
				index1 += width_;
				labels_[index1] = FLOODED;
				labels_[index1 + rect.width + 1] = FLOODED;
			}
			index1 += width_;
			Arrays.fill(labels_, index1, index1 + rect.width + 2, FLOODED);
		}
	}
	
	/**
	 * Specify the Gaussian fitting module
	 * @param module The Gaussian fitting module
	 */
	public void setGaussianFitModule(GaussianFitBase module) {
		if (module != null) {
			//bDoGaussianFit_ = true;
			g_ = module;
		} else {
			//bDoGaussianFit_ = false;
			g_ = null;
		}
	}
	
	/**
	 * Get the Gaussian fitting module
	 * @return The Gaussian fitting module
	 */
	public GaussianFitBase getGaussianFitModule() {
		return g_;
	}

	/**
	 * Analyze the image
	 * @param ip The image to be analyzed
	 * @param mask A rectangle of region of interest
	 * @param threshold Lowest intensity to be analyzed
	 * @param noise The noise threshold of the watershed algorithm
	 * @throws InterruptedException
	 */
	public void process(ImageProcessor ip, Rectangle mask, int threshold, int noise) throws InterruptedException {
		
		int border = 1;
		
		if (g_ != null) {

//			g_.setImageData(ip, isZeroBg_);
			
			border = g_.getWindowSize();
		}

		width_ = ip.getWidth();
		height_ = ip.getHeight();
		
		int [] offsets = {-width_, -width_ + 1, +1, +width_ + 1, +width_, +width_ - 1, -1, -width_ - 1};
	

		Rectangle bbox = new Rectangle(border, border, width_ - 2 * border, height_ - 2 * border);
		bbox = bbox.intersection(mask);

		ArrayList<Pixel> pixels = new ArrayList<Pixel>();

		for (int y = bbox.y; y < bbox.y + bbox.height; y++) {
			for (int x = bbox.x; x < bbox.x + bbox.width; x++) {
				int v = ip.get(x, y);
				if (v > threshold) {
					pixels.add(new Pixel(x,y,v));
				}
			}
		}
		Collections.sort(pixels);

		nParticles_ = 0;
		x_ = new double[pixels.size()];
		y_ = new double[pixels.size()];
		z_ = new double[pixels.size()];
		h_ = new double[pixels.size()];
		e_ = new double[pixels.size()];
		 
		FloodState floodState = new FloodState(width_, height_);
		floodState.floodBorders(bbox);
		
		int idxList, lenList;
		int [] listOfIndexes = new int[width_ * height_];
 
		for (Pixel p : pixels) {
			
			if (Thread.interrupted()) {
				throw( new InterruptedException() );
			}

			int index = p.x + width_ * p.y;
			
			if ( floodState.isProcessed(index) ){
				continue;
			}

			int v = p.value;
			boolean isMax = true;

			idxList = 0;
			lenList = 1;

			listOfIndexes[0] = index;
			
			floodState.flood(index);
			
			do {
				index = listOfIndexes[idxList];
				for (int d = 0; d < 8; d++) { // analyze all neighbors (in 8 directions) at the same level

					int index2 = index + offsets[d];

					if ( floodState.isProcessed(index2) ) { //conflict
						isMax = false; 
						break;
					}

					if ( ! floodState.isFlooded(index2)) {
						int v2 = ip.get(index2);
						if (v2 >= v - noise) {
							listOfIndexes[lenList++] = index2;
							floodState.flood(index2);
						}
					}
				} 
			} while ( ++ idxList < lenList);

			for (idxList = 0; idxList < lenList; idxList++) {
				floodState.process(listOfIndexes[idxList]);
			} 

			if (isMax) {
				
				if (g_ != null ) {

					g_.setInitialCoordinates(p.x, p.y);
					
					double [] result = g_.fit();
					
					if (result != null && g_.getH() > noise ) {
						x_[nParticles_] = g_.getX();
						y_[nParticles_] = g_.getY();
						z_[nParticles_] = g_.getZ();
						h_[nParticles_] = g_.getH();
						e_[nParticles_] = g_.getE();
						nParticles_++;
					
					}
				} else {

					x_[nParticles_] = (double) p.x;
					y_[nParticles_] = (double) p.y;
					h_[nParticles_] = (double) p.value;
					nParticles_++;

				}
			}
		}
	}
	
	/**
	 * Get analysis results
	 * @return X coordinates of all particles detected
	 */
	public double [] reportX() {
		return x_;
	}

	/**
	 * Get analysis results
	 * @return Y coordinates
	 */
	public double [] reportY() {
		return y_;
	}
	
	/**
	 * Get analysis results
	 * @return Z coordinates
	 */
	public double [] reportZ() {
		return z_;
	}
	
	/**
	 * Get analysis results
	 * @return Intensities
	 */
	public double [] reportH() {
		return h_;
	}

	/**
	 * Get analysis results
	 * @return Confidence levels
	 */
	public double [] reportE() {
		return e_;
	}
	
	/**
	 * Get number of particles detected
	 * @return Number of particles detected
	 */
	public int reportNumParticles() {
		return nParticles_;
	}
	
	/**
	 * Create an array of SmNodes corresponding to all particles detected
	 * @param frameNum The frame number of the current image analyzed
	 * @return An array of SmNode representing particles
	 */
	public SmNode[] createSmNodes(int frameNum) {
		SmNode [] nodes = null;
		if (nParticles_ > 0) {
			nodes = new SmNode[nParticles_];
			for (int i = 0; i < nParticles_; i++) {
				nodes[i] = new SmNode(x_[i], y_[i], z_[i], frameNum, (int) h_[i], e_[i]);
			}
		} 
		return nodes;
	}

	/**
	 * Create a PointRoi object that points to all particles detected in the image
	 * @return The PointRoi object
	 */
	public PointRoi createPointRoi() {
		PointRoi roi = null;

		if (nParticles_ > 0) {
			int [] xi = new int[nParticles_];
			int [] yi = new int[nParticles_];
			for (int i = 0; i < nParticles_; i ++ ) {
				xi[i] = (int) x_[i];
				yi[i] = (int) y_[i];
			}
			roi = new PointRoi(xi, yi, nParticles_);
		} 
		
		return roi;
	}
}
