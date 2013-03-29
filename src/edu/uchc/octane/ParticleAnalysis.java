//FILE:          ParticleAnalysis.java
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

import ij.gui.PointRoi;

/**
 * Base class for all particle analysis methods
 * @author Ji-Yu
 *
 */
public abstract class ParticleAnalysis {

	double [] x_;
	double [] y_;
	double [] z_;
	double [] e_;
	double [] h_;
	int nParticles_;
	
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
