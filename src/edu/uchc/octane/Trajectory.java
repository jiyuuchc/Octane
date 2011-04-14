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

   public boolean marked = false; 
   public String note = null;

   public double getX(int index) {
      return get(index).x;
   }

   public double getY(int index) {
      return get(index).y;
   }

   public int getFrame(int index) {
      return get(index).frame;
   }
   
   public int getLength() {
	   return get(size() - 1).frame - get(0).frame + 1;
   }

   public boolean isMarked() {
      return marked;
   }

   public void mark(boolean b) {
      marked = b;
   }
}
