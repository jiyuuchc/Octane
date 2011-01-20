//FILE:          Trajectory.java
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

import java.util.Vector;

public class Trajectory extends Vector<SmNode> {
   private static final long serialVersionUID = 8399421969489786849L;

   double quality_ = -1.0;
   double mobility_ = -1.0;
   double vMobility_ = -1.0;
   boolean disabled_ = false; 
   String note_ = null;

   public double getX(int index) {
      return get(index).x;
   }

   public double getY(int index) {
      return get(index).y;
   }

   public int getFrame(int index) {
      return get(index).frame;
   }

   public double getQuality() {
      if (quality_ < 0){ 
         quality_ = 0;
         for ( int i = 0; i < this.size(); i ++ ) {
            quality_ += get(i).quality; 
         }
         quality_ /= this.size();
      }
      return quality_;
   }

   public double getMobility() { // mean square displacement at 1 frame step 
      if (mobility_ < 0) {
         mobility_ = 0;
         if (size() > 1) {
            for ( int i = 0; i < this.size() - 1; i ++) {
               double dx = get(i+1).x - get(i).x;
               double dy = get(i+1).y - get(i).y;
               mobility_ += dx * dx + dy * dy; 
            }
            mobility_ /= this.size(); 
         }
      }
      return mobility_;
   }

   public double getVectorialMobility() {
	   if (vMobility_ < 0) {
		   vMobility_ = 0;
		   if (size() > 1) {
			   vMobility_ = (get(size()-1).x - get(0).x) * (get(size()-1).x - get(0).x);
			   vMobility_ += (get(size()-1).y - get(0).y) * (get(size()-1).y - get(0).y);
			   vMobility_ /= size();
		   }
	   }
	   return vMobility_;
   }
   
   public String getNote() {
	   return note_;
   }
   
   public void setNote(String note) {
	   note_ = note;
   }

   public boolean isDisabled() {
      return disabled_;
   }

   public void setEnable(boolean b) {
      disabled_ = ! b;
   }
}
