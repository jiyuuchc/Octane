//FILE:          SubPixelRefiner.java
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

import ij.process.ImageProcessor;

/**
 * The subpixel fitter interface.
 */
public interface SubPixelLocalization {

	/**
	 * Sets the image data.
	 *
	 * @param ip the new image data
	 */
	public void setImageData(ImageProcessor ip);
	
	/**
	 * Refine the peak position.
	 *
	 * @param x the initial x value
	 * @param y the initial y value
	 * @return positive or 0 for succucess, negative value for failure.
	 */
	public int refine(double x, double y);
	
	/**
	 * Subpixel X.
	 *
	 * @return x
	 */
	public double getX();
	
	/**
	 * Subpixel y .
	 *
	 * @return y
	 */
	public double getY();
	
	/**
	 * Height of the peak.
	 *
	 * @return height 
	 */
	public double getHeight();
	
	/**
	 * The residue.
	 *
	 * @return residue
	 */
	public double getError(); 
}
