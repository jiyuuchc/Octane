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

public abstract class ParticleAnalysis {

	double [] x_;
	double [] y_;
	double [] e_;
	double [] h_;
	int nParticles_;
	
	public double [] reportX() {
		return x_;
	}

	public double [] reportY() {
		return y_;
	}
	
	public double [] reportH() {
		return h_;
	}

	public double [] reportE() {
		return e_;
	}
	
	public int reportNumParticles() {
		return nParticles_;
	}
	
	public SmNode[] createSmNodes(int frameNum) {
		SmNode [] nodes = null;
		if (nParticles_ > 0) {
			nodes = new SmNode[nParticles_];
			for (int i = 0; i < nParticles_; i++) {
				nodes[i] = new SmNode(x_[i], y_[i], 0d, frameNum, (int) h_[i], e_[i]);
			}
		} 
		return nodes;
	}

}
