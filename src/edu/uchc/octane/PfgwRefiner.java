//FILE:          PfgwRefiner.java
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

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.QR;
import no.uib.cipr.matrix.Vector;
import ij.IJ;
import ij.process.ImageProcessor;
import java.lang.Math;

public class PfgwRefiner implements SubPixelRefiner {

   ImageProcessor frame_;
   DenseVector p_;
   double x_out, y_out;
   protected double sigma_2;
   protected int fit_area;
   protected int poly_order;
   protected int max_iter;
   protected double tol;
   protected double max_step;
   private double quality_;

   public PfgwRefiner(ImageProcessor ip) {
      sigma_2 = 3.4;
      fit_area = 3;
      poly_order = 4;
      tol = 0.00005;
      max_step = 2;
      max_iter = 20;

      frame_ = ip;
   }

   // This is adapted from Salman Roger's method
   // ref: Salman S Rogers et al 2007 Phys. Biol. 4 220-227   doi: 10.1088/1478-3975/4/3/008
   protected void polyfitgaussweight(double x_in, double y_in) {
      int x_int = (int) (x_in + 0.5);
      int y_int = (int) (y_in + 0.5);
      int x1 = Math.max(x_int - fit_area, 0);
      int y1 = Math.max(y_int - fit_area, 0);
      int x2 = Math.min(x_int + fit_area, frame_.getWidth() - 1);
      int y2 = Math.min(y_int + fit_area, frame_.getHeight() - 1);

      int numPixels = (x2 - x1 + 1) * (y2 - y1 + 1);

      DenseVector xp = new DenseVector(numPixels);
      DenseVector yp = new DenseVector(numPixels);
      // DenseVector weight = new DenseVector(numPixels);
      DenseMatrix V = new DenseMatrix(numPixels, (poly_order + 1) * (poly_order + 2) / 2);
      DenseVector z = new DenseVector(numPixels);

      int i = 0;

      for (int x = x1; x <= x2; x++) {
         double xd = x - x_in;
         for (int y = y1; y <= y2; y++) {
            double yd = y - y_in;
            double w = Math.exp(-(xd * xd + yd * yd) / sigma_2);
            xp.set(i, xd);
            yp.set(i, yd);
            // weight.set(i, w);
            V.set(i, 0, w);
            z.set(i, frame_.get(x, y) * w);
            i++;
         }
      }

      int column = 0;
      for (int order = 1; order <= poly_order; order++) {
         for (int sub_order = 1; sub_order <= order; sub_order++) {
            column++;
            for (int row = 0; row < V.numRows(); row++) {
               V.set(row, column, V.get(row, column - order) * xp.get(row));
            }
         }
         column++;
         for (int row = 0; row < V.numRows(); row++) {
            V.set(row, column, V.get(row, column - order - 1) * yp.get(row));
         }
      }

      QR qr = new QR(V.numRows(), V.numColumns());
      // logMatrix(V);
      qr.factor(V.copy());

      // p = R\(Q'*(weightexp.*frame))
      DenseVector tmp = new DenseVector(V.numColumns());
      qr.getQ().transMult(z, tmp);
      p_ = tmp.copy();
      qr.getR().solve(tmp, p_);

      // p_ = new DenseVector(V.numColumns());
      // V.solve(z, p_);
   }

   double p(int i) {
      return p_.get(i);
   }

   @Override
   public int refine(double x_in, double y_in){
      int iter_n = 1;
      x_out = x_in;
      y_out = y_in;

      while (iter_n < max_iter) {
         // IJ.log("x: " + x_out + " y: " + y_out);
         polyfitgaussweight(x_out, y_out);

         // logVector(p_);

         // a=p_(3); b=p_(4)/2; c=p(5); d=p(1)/2; f=p(2)/2; g=p(0);
         // J=a*c-b^2;
         double j = p(3) * p(5) - p(4) * p(4) / 4;
         if (j < 0)
        	return -1;
            //throw new Exception("J < 0: lost tracking");
         double xc = (p(4) * p(2) - 2 * p(5) * p(1)) / j / 4;
         double yc = (p(4) * p(1) - 2 * p(3) * p(2)) / j / 4;
         x_out = x_out + Math.signum(xc) * Math.min(Math.abs(xc), max_step);
         y_out = y_out + Math.signum(yc) * Math.min(Math.abs(yc), max_step);

         if (Math.abs(x_out - x_in) > 5 * max_step || Math.abs(y_out - y_in) > 5 * max_step) {
        	return -1;
            //throw new Exception("Wandered to far: lost tracking");
         }

         if ((xc * xc + yc * yc) < tol) {
            //IJ.log("" + p(3)/p(0) + "    " + p(5)/p(0));
            quality_ = -(p(0) + p(1)*xc + p(2)*yc + p(3)*xc*xc + p(4)*xc*yc + p(5)*yc*yc - 1500) / (p(3) + p(5))/2;
            return iter_n;
         }

         iter_n = iter_n + 1;
      }
      return -1;
      //throw new Exception("Too many iteration without converging.");
   }

   @Override
   public double getXOut() {
      return x_out;
   }

   @Override
   public double getYOut() {
      return y_out;
   }

   @Override
   public double getQuality() {
      return quality_;
   }

   void logMatrix(Matrix m) {
      String s1;
      for (int row = 0; row < m.numRows(); row++) {
         s1 = "";
         for (int column = 0; column < m.numColumns(); column++) {
            s1 += String.format("%+6.3f ", m.get(row, column));
         }
         IJ.log(s1);
      }
   }

   void logVector(Vector v) {
      String s1;
      s1 = "";
      for (int column = 0; column < v.size(); column++) {
         s1 += String.format("%+6.3f ", v.get(column));
      }
      IJ.log(s1);
   }
}
