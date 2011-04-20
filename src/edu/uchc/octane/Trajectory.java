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

/**
 * Represents a trajectory.
 * It is a vector.
 */
public class Trajectory extends Vector<SmNode> {

	private static final long serialVersionUID = 8187158272341013922L;

	private double stepSize2_ = -1;

	/** Whether it is marked. */
	public boolean marked = false;

	/** Whether it is already deleted. */
	public boolean deleted = false;

	/** A text note. */
	public String note = null;

	/**
	 * Gets the length of the trajectory.
	 *
	 * @return the length
	 */
	public int getLength() {
		return get(size() - 1).frame - get(0).frame + 1;
	}

	/**
	 * Gets the average stepsize^2 of the trajectory
	 * 
	 * @return Average stepsize^2
	 */
	public double getAvgSquareStepSize() {
		if (size() < 2)
			return 0;
		if (stepSize2_ >= 0)
			return stepSize2_;
		stepSize2_ = 0;
		int cnt = 0;
		for (int i = 0; i < size()-1; i++) {
			if (get(i+1).frame - get(i).frame == 1) {
				stepSize2_ += get(i+1).distance2(get(i));
				cnt ++;
			}
		}
		if (cnt > 0) 
			stepSize2_ /= cnt;
		return stepSize2_;
	}
}
