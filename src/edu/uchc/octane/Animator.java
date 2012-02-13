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
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//
package edu.uchc.octane;

import ij.ImagePlus;
import ij.gui.Overlay;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manage trajectory animation.
 */
public class Animator extends MouseAdapter {
	
	/** Time delay between each frame during animation */
	protected long ANIMATIONDELAY_ = 100;
		
	private boolean loop_;
	private Timer animateTimer_;
	private ImagePlus imp_;
	private int increment_ = 1; 
	private Overlay overlay_;

	/**
	 * Constructor.
	 *
	 * @param imp the image to be animated
	 */
	public Animator(ImagePlus imp) {
		imp_ = imp;
	}
	
	/**
	 * The animation task.
	 */
	protected class AnimateTimerTask extends TimerTask {
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
		
		/* (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
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

	/**
	 * Control whether animation loops.
	 *
	 * @param loop 
	 */
	public void setLoop(boolean loop) {
		loop_ = loop;
	}	

	/**
	 * Start animation.
	 *
	 * @param traj the trajectory to be animated
	 */
	public void animate(Trajectory traj) {
		stopAnimation();
		increment_ = 1;
		
		imp_.getCanvas().addMouseListener(this);
		animateTimer_ = new Timer();
		animateTimer_.schedule(new AnimateTimerTask(traj), ANIMATIONDELAY_, ANIMATIONDELAY_);
	}

	/**
	 * Stop animation.
	 */
	public void stopAnimation() {
		if (animateTimer_ != null) {
			animateTimer_.cancel();
			animateTimer_ = null;
		}
		if (imp_ != null ) {
			imp_.getCanvas().removeMouseListener(this);
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		stopAnimation();
	}
		
}
