//FILE:          GaussianFit3DSimple.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/21/13
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
//
package edu.uchc.octane;

import org.apache.commons.math3.util.FastMath;

public class GaussianFit3DSimple extends GaussianFit {
	
	final static double errTol_ = 0.1; 

	double [] calibration_ = null;
	double z_ = 0;
	double zMin_ = 0;
	double sigmaMin_ = 0;
	
	public GaussianFit3DSimple() {
		setFloatingSigma(true); 
	}
	
	public void setCalibrationValues(double[] p) throws IllegalArgumentException {
		if (p.length != 3) {
			throw new IllegalArgumentException("Length must be 3");
		}
		
		if (p[2] < 0) {
			throw new IllegalArgumentException("2nd order coeff should be positive");
		}

		zMin_ = - p[1]/p[2]/2; // focus position
		sigmaMin_ = p[0] + zMin_ * p[1] + zMin_ * zMin_ * p[2];
		
		calibration_ = p;
	}
	
	@Override
	public double[] fit() {
		double [] ret = super.fit();
		if (ret == null || calibration_ == null) {
			return ret;
		}
		
		double sigma = FastMath.sqrt(ret[ret.length - 1] / 2);
		if (sigma <= sigmaMin_) { // smaller than the min possible value 
			if ((sigmaMin_ - sigma) <  (errTol_ * sigmaMin_)) { // only a little
				z_ = zMin_;
				return ret;
			} else {
				return null;
			}
		}
		
		z_ = FastMath.sqrt((sigma - sigmaMin_)/calibration_[2]) + zMin_;
		
		return ret;
	}
	
	@Override
	public double getZ() {
		if (calibration_ == null) {  
			return 0;
		} else {
			return z_;
		}	
	}
}
