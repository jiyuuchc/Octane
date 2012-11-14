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

import ij.process.FloatProcessor;
import java.awt.Rectangle;


/**
 * @author Ji-Yu
 *
 */
public class Palm {

	public enum PalmType {AVERAGE, HEAD, TAIL, ALLPOINTS};
	public enum IFSType {SPOT, LINE, SQUARE};
	
	boolean useFiducial_;
	TrajDataset dataset_;

	double [] drift_x_; 
	double [] drift_y_;

	FloatProcessor ip_;
	Rectangle rect_;
	int [] selected_;

	int nPlotted_;
	int nSkipped_;

	public Palm (TrajDataset dataset) {
		dataset_ = dataset;
	}

	public void SetUseFiducial(boolean b) {
		useFiducial_ = b;
	}
	
	public boolean getUsefiducial() {
		return useFiducial_;
	}

	public int getnPlotted() {
		return nPlotted_;
	}

	public int getnSkipped() {
		return nSkipped_;
	}

	boolean setFiducialPoints() {
		int nFrames = dataset_.getMaximumFrameNumber();
		drift_x_ = new double[nFrames];
		drift_y_ = new double[nFrames];
		int npoints[] = new int[nFrames];
		for ( int i = 0; i < dataset_.getSize(); i++) {
			Trajectory t =  dataset_.getTrajectoryByIndex(i);
			if (t.marked && ! t.deleted) {
				for (int j = 1; j < t.size(); j++) {
					int frame = t.get(j).frame - 1;
					drift_x_[frame] += t.get(j).x - t.get(0).x;
					drift_y_[frame] += t.get(j).y - t.get(0).y;
					npoints[frame]++;
				}
			}
		}
		int cnt = 0;
		for (int j = 1; j < nFrames; j++) {
			if (npoints[j] > 0) {
				drift_x_[j] /= npoints[j];
				drift_y_[j] /= npoints[j];
				cnt++;
			} else {
				drift_x_[j] = drift_x_[j-1];
				drift_y_[j] = drift_y_[j-1];
			}
		}
		if (cnt == 0 )
			return false;
		else
			return true;
	}

	double getCorrectedX(Trajectory traj, int frameIndex) {
		double x = traj.get(frameIndex).x;
		if (useFiducial_) {
			x -= drift_x_[traj.get(frameIndex).frame - 1];
		}
		return x;
	}
	
	double getCorrectedY(Trajectory traj, int frameIndex) {
		double y = traj.get(frameIndex).y;
		if (useFiducial_) {
			y -= drift_y_[traj.get(frameIndex).frame - 1];
		}
		return y;	
	}


	void gaussianImage(double xs, double ys, double w) {
		for (int x = Math.max(0, (int)(xs - 3*w)); x < Math.min(ip_.getWidth(), (int)(xs + 3*w)); x ++) {
			for (int y = Math.max(0, (int)(ys - 3*w)); y < Math.min(ip_.getHeight(), (int)(ys + 3*w)); y++) {
				double v = Math.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*w*w) );
				ip_.setf(x, y, (float)v + ip_.getf(x,y));
			}
		}
	}

	public FloatProcessor constructPalm(PalmType palmType, Rectangle rect, int [] selected) {
		if (useFiducial_) {
			useFiducial_ = setFiducialPoints();
		}
		
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
		double xx, yy, xs, ys;

		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmScaleFactor_;
		
		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
			if (useFiducial_ && traj.marked){
				continue;
			}
			if (isHead) {
				xx = getCorrectedX(traj, 0);
				yy = getCorrectedY(traj, 0);
				xs = (xx - rect.x)* Prefs.palmScaleFactor_;
				ys = (yy - rect.y)* Prefs.palmScaleFactor_;
				gaussianImage(xs, ys, psdWidth);
				nPlotted_ ++;
			} else {
				int f = traj.size()-1;
				xx = getCorrectedX(traj, f);
				yy = getCorrectedY(traj, f);
				xs = (xx - rect.x)* Prefs.palmScaleFactor_;
				ys = (yy - rect.y)* Prefs.palmScaleFactor_;
				gaussianImage(xs, ys, psdWidth);
				nPlotted_ ++;
			}
		}
	}	
	
	void constructPalmTypeAverage(Rectangle rect, int [] selected) {
		double xx, yy, xx2, yy2, xs, ys;

		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmScaleFactor_;
		
		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
			if (useFiducial_ && traj.marked){
				continue;
			}
			
			xx= getCorrectedX(traj, 0);
			yy= getCorrectedY(traj, 0);
			xx2 = xx*xx;
			yy2 = yy*yy;

			for (int j = 1; j < traj.size(); j++ ) {
				double x = getCorrectedX(traj, j);
				double y = getCorrectedY(traj, j);
				xx += x;
				yy += y;
				xx2 += x * x;
				yy2 += y * y;
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
	}
	
	void constructPalmTypeAllPoints(Rectangle rect, int [] selected) {
		double xx, yy, xs, ys;

		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmScaleFactor_;
		
		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
			if (useFiducial_ && traj.marked){
				continue;
			}
			for (int j = 0; j < traj.size(); j++ ) {
				xx = getCorrectedX(traj, j);
				yy = getCorrectedY(traj, j);
				xs = (xx - rect.x)* Prefs.palmScaleFactor_;
				ys = (yy - rect.y)* Prefs.palmScaleFactor_;
				gaussianImage(xs, ys, psdWidth);
				nPlotted_ ++;
			}
		}
	}
}
