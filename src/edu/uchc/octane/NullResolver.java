//FILE:          NullResolver.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/30/11
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

import ij.process.ImageProcessor;

public class NullResolver implements SubPixelResolver {

	double x_, y_;
	ImageProcessor ip_;

	@Override
	public void setImageData(ImageProcessor ip) {
		ip_ = ip;
	}

	@Override
	public int refine(double x, double y) {
		x_ = x; y_ = y;
		return 0;
	}

	@Override
	public double getXOut() {
		return x_;
	}

	@Override
	public double getYOut() {
		return y_;
	}

	@Override
	public double getHeightOut() {
		return ip_.getPixelValue((int)x_, (int)y_);
	}

	@Override
	public double getConfidenceEstimator() {
		return 0;
	}

}
