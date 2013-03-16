//FILE:          PFGWResolver.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/16/11
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

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.QRDecompositionImpl;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.stat.descriptive.moment.Variance;


/**
 * Subpixel fitting the peak using polynomial functions with Guassian weighting function.
 */
public class PFGWSPL implements SubPixelLocalization {

	ImageProcessor frame_;

	private RealVector p_;
	private double x_out, y_out, h_out;
	private double sigma_2 = 2;
	private final int poly_order = 2;
	private final int max_iter = 5;
	private final double tol = 0.005;
	private final double max_step = 1;
	private final double cuvLimit = 1.8;
	private double residue_;
	private double bg_;
	private int numPixels = (Prefs.kernelSize_ * 2 + 1) * (Prefs.kernelSize_ * 2 + 1);
	private ArrayRealVector xps = new ArrayRealVector(numPixels);
	private ArrayRealVector yps = new ArrayRealVector(numPixels);
	private Array2DRowRealMatrix V = new Array2DRowRealMatrix(numPixels, (poly_order + 1) * (poly_order + 2) / 2);
	private ArrayRealVector z = new ArrayRealVector(numPixels);

	private double pixels[] = new double[numPixels];

	/* (non-Javadoc)
	 * @see edu.uchc.octane.SubPixelRefiner#setImageData(ij.process.ImageProcessor)
	 */
	 @Override
	 public void setImageData(ImageProcessor ip) {
		 frame_ = ip;
		 bg_ = frame_.getAutoThreshold();
	 }

	 // This is adapted from Salman Roger's method
	 // ref: Salman S Rogers et al 2007 Phys. Biol. 4 220-227   doi: 10.1088/1478-3975/4/3/008
	 protected void polyfitgaussweight(double x, double y) {
		 int x0_ , y0_;
		 double xp, yp;
		 if (x < Prefs.kernelSize_) {
			 x0_ = Prefs.kernelSize_;
		 } else if (x >= frame_.getWidth() - Prefs.kernelSize_) {
			 x0_ = frame_.getWidth() - Prefs.kernelSize_ - 1;
		 } else { 
			 x0_ = (int) x;
		 }
		 xp = x0_ + .5 - x;

		 if (y < Prefs.kernelSize_) {
			 y0_ = Prefs.kernelSize_;
		 } else if (y >= frame_.getHeight() - Prefs.kernelSize_) {
			 y0_ = frame_.getHeight() - Prefs.kernelSize_ - 1;
		 } else { 
			 y0_ = (int) y;
		 }
		 yp = y0_ + .5 - y;

		 int i = 0;
		 for (int xi = -Prefs.kernelSize_; xi <= Prefs.kernelSize_; xi++) {
			 double xd = xp + xi;
			 for (int yi = -Prefs.kernelSize_; yi <= Prefs.kernelSize_; yi++) {
				 double yd = yp + yi;
				 double w = Math.exp(-(xd * xd + yd * yd) / sigma_2);
				 xps.setEntry(i, xd);
				 yps.setEntry(i, yd);
				 V.setEntry(i, 0, w);
				 z.setEntry(i, frame_.getf(x0_ + xi, y0_ + yi) * w);
				 
				 pixels[i]= frame_.getf(x0_+xi, y0_ + yi);
				 i++;
			 }
		 }

		 int column = 0;
		 for (int order = 1; order <= poly_order; order++) {
			 for (int sub_order = 1; sub_order <= order; sub_order++) {
				 column++;
				 V.setColumnVector(column, xps.ebeMultiply(V.getColumn(column - order)));
			 }
			 column++;
			 V.setColumnVector(column, yps.ebeMultiply(V.getColumn(column - order - 1)));
		 }

		 QRDecompositionImpl qr = new QRDecompositionImpl(V);
		 p_ = qr.getSolver().solve(z);
		 RealVector r = V.operate(p_).subtract(z);
		 residue_ = r.dotProduct(r);
	 }

	 private double p(int i) {
		 return p_.getEntry(i);
	 }

	 /* (non-Javadoc)
	  * @see edu.uchc.octane.SubPixelRefiner#refine(double, double)
	  */
	 @Override
	 public int refine(double x_in, double y_in){
		 sigma_2 = Prefs.sigma_ * Prefs.sigma_ * 2 * 1.5; // use a slightly broader gaussian function
		 
		 int iter_n = 1;
		 x_out = x_in;
		 y_out = y_in;

		 while (iter_n < max_iter) {
			 try {
				 polyfitgaussweight(x_out, y_out);
			 } catch (Exception e) { //
				 return -4;
			 }

			 // a=p(3); b=p(4)/2; c=p(5); 
			 // J=a*c-b^2;
			 double j = p(3) * p(5) - p(4) * p(4) / 4;
			 if (j < 0)
				 return -1;
			 double xc = (p(4) * p(2) - 2 * p(5) * p(1)) / j / 4;
			 double yc = (p(4) * p(1) - 2 * p(3) * p(2)) / j / 4;
			 x_out = x_out + Math.signum(xc) * Math.min(Math.abs(xc), max_step);
			 y_out = y_out + Math.signum(yc) * Math.min(Math.abs(yc), max_step);

			 if (Math.abs(x_out - x_in) > 3 * max_step || Math.abs(y_out - y_in) > 3 * max_step) {
				 return -1;
			 }

			 if ((xc * xc + yc * yc) < tol) {

				 h_out = p(0) + p(1)*xc + p(2)*yc + p(3)*xc*xc + p(4)*xc*yc + p(5)*yc*yc - bg_;
//				 if (h_out < 0) {
//					 return -2;
//				 }
				 double h = -(p(3) + p(5)) / 2 * cuvLimit; // should be around 1/2*sigma^2
				 //	        	if ( - cuv < cuvLimit) {
					 //	        		return -2;
				 //	        	}
				 residue_ = residue_ / h / h;
				 return iter_n;
			 }

			 iter_n = iter_n + 1;
		 }
		 return -3;
	 }

	 /* (non-Javadoc)
	  * @see edu.uchc.octane.SubPixelRefiner#getXOut()
	  */
	 @Override
	 public double getX() {
		 return x_out;
	 }

	 /* (non-Javadoc)
	  * @see edu.uchc.octane.SubPixelRefiner#getYOut()
	  */
	 @Override
	 public double getY() {
		 return y_out;
	 }

	 /* (non-Javadoc)
	  * @see edu.uchc.octane.SubPixelRefiner#getHeightOut()
	  */
	 @Override
	 public double getHeight() {
		 return h_out;
	 }

	 /* (non-Javadoc)
	  * @see edu.uchc.octane.SubPixelRefiner#getResidue()
	  */
	 @Override
	 public double getError() {
		 double m;
		 m = new Variance(false).evaluate(pixels);
		 return numPixels * Math.log(m/residue_);
	 }
}

