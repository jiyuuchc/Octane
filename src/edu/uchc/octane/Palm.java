//FILE:          Palm.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 11/14/12
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

package edu.uchc.octane;

import ij.IJ;
import ij.process.FloatProcessor;
import java.awt.Rectangle;

/**
 *  PALM plotting module
 */
public class Palm {

	public enum PalmType {AVERAGE, HEAD, TAIL, ALLPOINTS};
	public enum IFSType {SPOT, LINE, SQUARE};
	
	boolean correctDrift_;
	TrajDataset dataset_;

	FloatProcessor ip_;
	Rectangle rect_;
	int [] selected_;

	int nPlotted_;
	int nSkipped_;

	public Palm (TrajDataset dataset) {
		dataset_ = dataset;
	}

	public void setCorrectDrift(boolean b) {
		correctDrift_ = b;
	}
	
	public boolean getCorrectDrift() {
		return correctDrift_;
	}

	public int getNPlotted() {
		return nPlotted_;
	}

	public int getNSkipped() {
		return nSkipped_;
	}

	void gaussianImage(double xs, double ys, double w) {
		for (int x = Math.max(0, (int)(xs - 3*w)); x < Math.min(ip_.getWidth(), (int)(xs + 3*w)); x ++) {
			for (int y = Math.max(0, (int)(ys - 3*w)); y < Math.min(ip_.getHeight(), (int)(ys + 3*w)); y++) {
				double v = 100 * Math.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*w*w) );
				ip_.setf(x, y, (float)v + ip_.getf(x,y));
			}
		}
	}

	public FloatProcessor constructPalm(PalmType palmType, Rectangle rect, int [] selected) {
		nPlotted_ = 0;
		nSkipped_ = 0;

		ip_ = new FloatProcessor((int) (rect.width * Prefs.palmScaleFactor_), (int) (rect.height * Prefs.palmScaleFactor_));
	
		switch (palmType) {
		case HEAD:
			constructPalmTypeHeadOrTail(rect, selected, true);
			break;
		case TAIL:
			constructPalmTypeHeadOrTail(rect, selected, false);
			break;
		case AVERAGE:
			constructPalmTypeAverage(rect, selected);
			break;
		case ALLPOINTS:
			constructPalmTypeAllPoints(rect, selected);
			break;
		}
		return ip_;
	}

	void constructPalmTypeHeadOrTail(Rectangle rect, int [] selected, boolean isHead) {
		double xs, ys;

		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmScaleFactor_;
		
		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
//			if (correctDrift_ && traj.marked){
//				continue;
//			}
			try {
				SmNode node;
				if (isHead) {
					node = traj.get(0);
				} else {
					node = traj.get(traj.size()-1);
				}
				if (correctDrift_) {
					node = dataset_.correctDrift(node);
				}
				xs = (node.x - rect.x)* Prefs.palmScaleFactor_;
				ys = (node.y - rect.y)* Prefs.palmScaleFactor_;
				gaussianImage(xs, ys, psdWidth);
				nPlotted_ ++;
			} catch (OctaneException e) {
				IJ.showMessage("Error drift compensation.");
				return;
			}

		}
	}	
	
	void constructPalmTypeAverage(Rectangle rect, int [] selected) {
		double xx, yy, xx2, yy2, xs, ys;

		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmScaleFactor_;
		
		try {
			for ( int i = 0; i < selected.length; i ++) {
				Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
//				if (correctDrift_ && traj.marked){
//					continue;
//				}
				SmNode node;
				node = traj.get(0);
				if (correctDrift_) {
					node = dataset_.correctDrift(node);
				}
				xx= node.x;
				yy= node.y;
				xx2 = xx*xx;
				yy2 = yy*yy;

				for (int j = 1; j < traj.size(); j++ ) {
					node = traj.get(j);
					if (correctDrift_) {
						node = dataset_.correctDrift(node);
					}
					xx += node.x;
					yy += node.y;
					xx2 += node.x * node.x;
					yy2 += node.y * node.y;
				}

				xx /= traj.size();
				yy /= traj.size();
				xx2 /= traj.size();
				yy2 /= traj.size();

				if (xx2 - xx * xx < Prefs.palmThreshold_ && yy2 - yy * yy < Prefs.palmThreshold_) {
					xs = (xx - rect.x)* Prefs.palmScaleFactor_;
					ys = (yy - rect.y)* Prefs.palmScaleFactor_;
					gaussianImage(xs, ys, psdWidth);
					nPlotted_ ++;
				} else {
					nSkipped_ ++;
				}
			}
		} catch (OctaneException e) {
			IJ.showMessage("Error drift compensation.");
			return;
		}
	}

	void constructPalmTypeAllPoints(Rectangle rect, int [] selected) {
		double xs, ys;

		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmScaleFactor_;

		try {
			for ( int i = 0; i < selected.length; i ++) {
				Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
//				if (correctDrift_ && traj.marked){
//					continue;
//				}
				for (int j = 0; j < traj.size(); j++ ) {
					SmNode node;
					node = traj.get(j);
					if (correctDrift_) {
						node = dataset_.correctDrift(node);
					}
					xs = (node.x - rect.x)* Prefs.palmScaleFactor_;
					ys = (node.y - rect.y)* Prefs.palmScaleFactor_;
					gaussianImage(xs, ys, psdWidth);
					nPlotted_ ++;
				}
			}
		} catch (OctaneException e) {
			IJ.showMessage("Error drift compensation.");
			return;
		}

	}
}
