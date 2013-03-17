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
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

/**
 *  PALM plotting module
 */
public class Palm {

	public enum PalmType {AVERAGE, HEAD, TAIL, ALLPOINTS, STACK};
	
	boolean correctDrift_;
	TrajDataset dataset_;

	double palmScaleFactor_;
	double palmPSFWidth_;
	
	double palmThreshold_ = 1e6;

	int nPlotted_;
	int nSkipped_;
	
	public Palm (TrajDataset dataset) {
		dataset_ = dataset;
	}

	Rectangle getCurrentROI(ImagePlus imp) {
		Rectangle rect;
		Roi roi = imp.getRoi();
		if (roi!=null && roi.isArea()) {
			rect = roi.getBounds();
		} else {
			imp.killRoi();
			rect = imp.getProcessor().getRoi();
			imp.setRoi(roi);
		}
		return rect;
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

	void gaussianImage(ImageProcessor ip, double xs, double ys, double w) {
		for (int x = Math.max(0, (int)(xs - 3*w)); x < Math.min(ip.getWidth(), (int)(xs + 3*w)); x ++) {
			for (int y = Math.max(0, (int)(ys - 3*w)); y < Math.min(ip.getHeight(), (int)(ys + 3*w)); y++) {
				double v = 100 * Math.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*w*w) );
				ip.setf(x, y, (float)v + ip.getf(x,y));
			}
		}
	}

	public void constructPalm(PalmType palmType, double scale, double psfSigma, final ImagePlus imp, final int [] selected) {
		nPlotted_ = 0;
		nSkipped_ = 0;
		
		palmScaleFactor_ = scale;
		palmPSFWidth_ = psfSigma;

		final Rectangle rect = getCurrentROI(imp);

		if (palmType == PalmType.STACK) {
			SwingWorker task = new SwingWorker<ImageStack, Void>() {
				public ImageStack doInBackground() {
					ImageStack is =  new ImageStack((int)(rect.width * palmScaleFactor_), (int)(rect.height * palmScaleFactor_));
					for (int i = 0; i < imp.getStack().getSize(); i++) {
						ImageProcessor ip = new FloatProcessor((int)(rect.width * palmScaleFactor_), (int)(rect.height * palmScaleFactor_));
						is.addSlice(""+i, ip);
					}
					for ( int i = 0; i < selected.length; i ++) {
						Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
						constructPALMStack(traj, rect, is);
						firePropertyChange("Progress", i, i + 1);
					}
					return is;
				}

				public void done() {
					ImagePlus img = imp.createImagePlus();
					try {
						img.setStack("PALMStack-" + imp.getTitle(), get());
					} 
					catch (InterruptedException ignore) {}
					catch (ExecutionException e) {
						IJ.showMessage("Error constructing PALM stack ");
						Throwable cause = e.getCause();
						if (cause != null) {
							System.err.println(cause.getLocalizedMessage());
						} else {
							System.err.println(e.getLocalizedMessage());
						}
							
					}
					img.show();
				}				
			};
			task.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getPropertyName() == "Progress") {
						IJ.showProgress((Integer) evt.getNewValue(), imp.getStack().getSize());
					}
				}
			});
			task.execute();

		} else {
			
			FloatProcessor ip = null; 

			switch (palmType) {
			case HEAD:
				ip = constructPalmTypeHeadOrTail(rect, selected, true);
				break;
			case TAIL:
				ip = constructPalmTypeHeadOrTail(rect, selected, false);
				break;
			case AVERAGE:
				ip = constructPalmTypeAverage(rect, selected);
				break;
			case ALLPOINTS:
				ip = constructPalmTypeAllPoints(rect, selected);
				break;
			}

			if (ip != null ) {
				ImagePlus img = new ImagePlus("PALM-" + imp.getTitle(), ip);
				img.show();
			}

			IJ.log(String.format("Plotted %d molecules, skipped %d molecules.", getNPlotted(), getNSkipped()));
		}
	}

	FloatProcessor constructPalmTypeHeadOrTail(Rectangle rect, int [] selected, boolean isHead) {
		double xs, ys;
		double psdWidth = palmPSFWidth_ * palmScaleFactor_;

		FloatProcessor ip = new FloatProcessor((int) (rect.width * palmScaleFactor_), (int) (rect.height * palmScaleFactor_));	

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
				xs = (node.x - rect.x)* palmScaleFactor_;
				ys = (node.y - rect.y)* palmScaleFactor_;
				gaussianImage(ip, xs, ys, psdWidth);
				nPlotted_ ++;
			} catch (OctaneException e) {
				IJ.showMessage("Error drift compensation.");
				return null;
			}

		}
		
		return ip;
	}	
	
	FloatProcessor constructPalmTypeAverage(Rectangle rect, int [] selected) {
		double xx, yy, xx2, yy2, xs, ys;

		double psdWidth = palmPSFWidth_ * palmScaleFactor_;
		
		FloatProcessor ip = new FloatProcessor((int) (rect.width * palmScaleFactor_), (int) (rect.height * palmScaleFactor_));		

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

				if (xx2 - xx * xx < palmThreshold_ && yy2 - yy * yy < palmThreshold_) {
					xs = (xx - rect.x)* palmScaleFactor_;
					ys = (yy - rect.y)* palmScaleFactor_;
					gaussianImage(ip, xs, ys, psdWidth);
					nPlotted_ ++;
				} else {
					nSkipped_ ++;
				}
			}
		} catch (OctaneException e) {
			IJ.showMessage("Error drift compensation.");
			return null;
		}
		
		return ip;
	}

	FloatProcessor constructPalmTypeAllPoints(Rectangle rect, int [] selected) {
		double xs, ys;

		double psdWidth = palmPSFWidth_ * palmScaleFactor_;

		FloatProcessor ip = new FloatProcessor((int) (rect.width * palmScaleFactor_), (int) (rect.height * palmScaleFactor_));

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
					xs = (node.x - rect.x)* palmScaleFactor_;
					ys = (node.y - rect.y)* palmScaleFactor_;
					gaussianImage(ip, xs, ys, psdWidth);
					nPlotted_ ++;
				}
			}
		} catch (OctaneException e) {
			IJ.showMessage("Error drift compensation.");
			return null;
		}
		
		return ip;
	}
	
	void constructPALMStack(Trajectory traj, Rectangle rect, ImageStack stack) {
		if (traj == null ) {
			return;
		}
		for (int i = 0; i < traj.size(); i++ ) {
			SmNode node = traj.get(i);
			if (correctDrift_) {
				try {
					node = dataset_.correctDrift(node);
				} catch (OctaneException e) {
					IJ.showMessage("Error drift compensation.");
					node = traj.get(i);
					correctDrift_ = false;
				}
			}
			ImageProcessor ip = stack.getProcessor(traj.get(i).frame);
			double xs = (traj.get(i).x - rect.x) * palmScaleFactor_;
			double ys = (traj.get(i).y - rect.y) * palmScaleFactor_;
			gaussianImage(ip, xs, ys, palmPSFWidth_ * palmScaleFactor_);
		}
	}
	
//	void drawIFSLineOverlay(Trajectory traj, Rectangle rect, ImageStack stack) {
//		if (traj == null || traj.size() < 2) 
//			return;
//
//		int [] xs = new int[traj.size()];
//		int [] ys = new int[traj.size()];
//		for (int i = 0; i < traj.size(); i ++) {
//			xs[i] = (int)((traj.get(i).x - rect.x) * IFSScaleFactor_);
//			ys[i] = (int)((traj.get(i).y - rect.y) * IFSScaleFactor_);			
//		}
//		
//		PolygonRoi roi = null;
//		int frame = traj.get(0).frame;
//		ImageProcessor ip;
//		for (int i = 0; i < traj.size(); i++) {
//			while (frame < traj.get(i).frame) {
//				ip = stack.getProcessor(frame ++);
//				ip.setColor(Toolbar.getForegroundColor());
//				roi.drawPixels(ip);
//			}
//			roi = new PolygonRoi(xs, ys, i + 1, Roi.POLYLINE);
//			ip = stack.getProcessor(frame ++);
//			ip.setColor(Toolbar.getForegroundColor());
//			roi.drawPixels(ip);
//		}
//	}
//
//	void drawIFSSquareOverlay(Trajectory traj, Rectangle rect, ImageStack stack) {
//		if (traj == null || traj.size() < 2) 
//			return;
//	
//		int frame = traj.get(0).frame;
//		Roi roi = null;
//		ImageProcessor ip;
//
//		for ( int i = 0; i < traj.size(); i ++ ) {
//			if (frame < traj.get(i).frame) {
//				ip = stack.getProcessor(frame ++);
//				ip.setColor(Toolbar.getForegroundColor());
//				roi.drawPixels(ip);
//			}
//			int nx = (int)((traj.get(i).x - rect.x - 4) * IFSScaleFactor_);
//			int ny = (int)((traj.get(i).y - rect.y - 4) * IFSScaleFactor_);
//			roi = new Roi(nx,ny, 9 * IFSScaleFactor_, 9 * IFSScaleFactor_);
//			ip = stack.getProcessor(frame ++);
//			ip.setColor(Toolbar.getForegroundColor());
//			roi.drawPixels(ip);
//		}
//	}	
}
