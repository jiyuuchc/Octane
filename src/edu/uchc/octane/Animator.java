//FILE:          Animator.java
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

import ij.ImagePlus;
import ij.gui.Overlay;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.util.Timer;
import java.util.TimerTask;

public class Animator {
	final long ANIMATIONDELAY_ = 100;
		
	private boolean loop_;
	private Timer animateTimer_;
	private ImagePlus imp_;
	private int increment_ = 1; 
	private Overlay overlay_;

	public Animator(ImagePlus imp) {
		imp_ = imp;
	}
	
	class AnimateTimerTask extends TimerTask {
		private Trajectory trajectory_;
		private int firstFrame_;
		private int lastFrame_;
		private int curFrame_;
		private int curIndex_;

		private AnimateTimerTask(Trajectory trajectory) {
			trajectory_ = trajectory;
			firstFrame_ = trajectory.get(0).frame;
			lastFrame_ = trajectory.get(trajectory.size() - 1).frame;
			// IJ.write("First "+ firstFrame_ + " Last" + lastFrame_);
			curFrame_ = firstFrame_;
			curIndex_ = 0;
		}
		
		@Override
		public void run() {
			if (curFrame_ < firstFrame_){
				imp_.setOverlay(overlay_);
				stopAnimation();
				return;
			}
			if (curFrame_ > lastFrame_) {
				if (!loop_) {
					stopAnimation();
					return;
				} else {
					overlay_ = imp_.getOverlay();
					increment_ = -1;
					curFrame_ = lastFrame_;
					curIndex_ = trajectory_.size()-1;
				}
			}

			synchronized (imp_) {
				imp_.setSlice(curFrame_);
			}

			if (trajectory_.get(curIndex_).frame == curFrame_) {
				GeneralPath path = new GeneralPath();
				path.moveTo(trajectory_.get(0).x,trajectory_.get(0).y);
				for (int i = 1; i <curIndex_; i++) {
					path.lineTo(trajectory_.get(i).x, trajectory_.get(i).y);
				}
				imp_.setOverlay(path, Color.yellow, new BasicStroke(1f));
				curIndex_ += increment_;
			}
			curFrame_ += increment_;
		}
	}

	void setLoop(boolean loop) {
		loop_ = loop;
	}	

	public void animate(Trajectory traj) {
		stopAnimation();
		increment_ = 1;
		animateTimer_ = new Timer();
		animateTimer_.schedule(new AnimateTimerTask(traj), ANIMATIONDELAY_, ANIMATIONDELAY_);
	}

	public void stopAnimation() {
		if (animateTimer_ != null) {
			animateTimer_.cancel();
			animateTimer_ = null;
		}
		
	}
}
