//FILE:          DriftCorrectionModule.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 2/16/13
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

public class DriftCorrectionModule {
	TrajDataset dataset_;

	boolean hasDriftCorrectionData_;
	double [] drift_x_; 
	double [] drift_y_;
	double [] drift_z_;

	public DriftCorrectionModule(TrajDataset dataset) {
		dataset_ = dataset;
		hasDriftCorrectionData_ = false;
	}

	public boolean hasDrfitCorrectionData(){
		return hasDrfitCorrectionData();
	}
	
	public double [] getDriftX() {
		return drift_x_;
	}

	public double [] getDriftY() {
		return drift_y_;
	}

	public double [] getDriftZ() {
		return drift_z_;
	}
	
	public void setDriftData(double [] driftx, double [] drifty, double [] driftz) throws OctaneException {
		int nFrames = dataset_.getMaximumFrameNumber();
		if (driftx.length != nFrames || drifty.length != nFrames || driftz.length != nFrames) {
			throw new OctaneException("Error importing drift data: length mismatch");
		}		
		drift_x_ = driftx;
		drift_y_ = drifty;
		drift_z_ = driftz;
		hasDriftCorrectionData_ = true;
	}

	public void setFiducialPoints(int [] selection) {
		int nFrames = dataset_.getMaximumFrameNumber();
		drift_x_ = new double[nFrames];
		drift_y_ = new double[nFrames];
		drift_z_ = new double[nFrames];
		int npoints[] = new int[nFrames];

		for ( int i = 0; i < selection.length; i++) {
			Trajectory t =  dataset_.getTrajectoryByIndex(selection[i]);
			for (int j = 1; j < t.size(); j++) {
				int frame = t.get(j).frame - 1;
				drift_x_[frame] += t.get(j).x - t.get(0).x;
				drift_y_[frame] += t.get(j).y - t.get(0).y;
				drift_z_[frame] += t.get(j).z - t.get(0).z;
				npoints[frame]++;
			}
		}
		int cnt = 0;
		for (int j = 1; j < nFrames; j++) {
			if (npoints[j] > 0) {
				drift_x_[j] /= npoints[j];
				drift_y_[j] /= npoints[j];
				drift_z_[j] /= npoints[j];
				cnt++;
			} else {
				drift_x_[j] = drift_x_[j-1];
				drift_y_[j] = drift_y_[j-1];
				drift_z_[j] = drift_z_[j-1];
			}
		}

		if (cnt == 0 )
			hasDriftCorrectionData_ = false;
		else
			hasDriftCorrectionData_ = true;

		return;		
	}

	public void setFiducialPoints() {
		int [] marked = new int[dataset_.getSize()];
		int cnt = 0;
		for ( int i = 0; i < dataset_.getSize(); i++) {
			Trajectory t =  dataset_.getTrajectoryByIndex(i);
			if (t.marked && ! t.deleted) {
				marked[cnt++] = i;
			}			
		}
		int [] marked_n = new int[cnt];
		for (int i = 0; i < cnt; i++) {
			marked_n[i] = marked[i];
		}
		setFiducialPoints(marked_n);

		return;
	}

	public void calcDriftByGlobalDisplacement(int [] selection) {
		int nFrames = dataset_.getMaximumFrameNumber();
		drift_x_ = new double[nFrames];
		drift_y_ = new double[nFrames];
		drift_z_ = new double[nFrames];

		for ( int i = 0; i < selection.length; i++) {
			Trajectory t =  dataset_.getTrajectoryByIndex(selection[i]);
			for (int j = 1; j < t.size() - 1; j++) {
				int frame = t.get(j + 1).frame;
				if (t.get(j).frame == frame - 1) {
					drift_x_[frame] += t.get(j).x - t.get(j+1).x;
					drift_y_[frame] += t.get(j).y - t.get(j+1).y;
					drift_z_[frame] += t.get(j).z - t.get(j+1).z;
				}
			}
		}

		//cumulate
		for (int i = 1; i < nFrames; i++ ) {
			drift_x_[i] += drift_x_[i-1];
			drift_y_[i] += drift_y_[i-1];
			drift_z_[i] += drift_z_[i-1];
		}

		hasDriftCorrectionData_ = true;
		return;
	}

	public SmNode correctDrift(SmNode node) throws OctaneException {
		if (!hasDriftCorrectionData_) {
			throw(new OctaneException("No drift compensation data available."));
		}
		SmNode correctedNode  = node.clone();
		int frame = correctedNode.frame;
		correctedNode.x -= drift_x_[frame-1];
		correctedNode.y -= drift_y_[frame-1];
		correctedNode.z -= drift_z_[frame-1];
		return correctedNode;
	}
}
