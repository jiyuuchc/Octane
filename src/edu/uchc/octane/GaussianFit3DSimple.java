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

/**
 * A very simple 3D Gaussian fitting module.
 * The module try to calculate the z coordinate based on how much defocusing the image has. It does
 * not consider the degeneracy problem (defocusing can be caused by focusing shift up or down), and 
 * assume focus always shift up. The sensitivity of the analysis near focus is poor.  
 * @author Ji-Yu
 *
 */
public class GaussianFit3DSimple extends GaussianFit {
	
	final static double errTol_ = 0.1; 

	double [] calibration_ = null;
	double z_ = 0;
	double zMin_ = 0;
	double sigmaMin_ = 0;
	
	/**
	 * Constructor
	 */
	public GaussianFit3DSimple() {
		setFloatingSigma(true); 
	}
	
	/**
	 * Specify the relationship between Z coordinates and sigma of the Gaussian function.
	 * The relationship is expressed as a 2nd order polynomial function  
	 * @param p The zeroth, 1st and 2nd order polynomial coefficient of the function 
	 * @throws IllegalArgumentException
	 */
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
	
	/* (non-Javadoc)
	 * @see edu.uchc.octane.GaussianFit#fit()
	 */
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
	
	/* (non-Javadoc)
	 * @see edu.uchc.octane.BaseGaussianFit#getZ()
	 */
	@Override
	public double getZ() {
		if (calibration_ == null) {  
			return 0;
		} else {
			return z_;
		}	
	}
}
