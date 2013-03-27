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
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.commons.math3.util.FastMath;

/**
 *  PALM plotting module
 */
public class Palm {
	int nPlotted_;
	int nSkipped_;
	
	public enum PalmType {AVERAGE, HEAD, TAIL, ALLPOINTS, TIMELAPSE};
	public enum ZRenderMode {NONE, COLOR, STACK}; 
	
	private boolean correctDrift_;
	private TrajDataset dataset_;

	private ZRenderMode zRenderMode_;
	private double palmScaleFactor_;
	private double palmPSFWidth_;
	
	private final double palmThreshold_ = 1e6;

	int lut_[] = null;
	double zMin_, zMax_;

	public Palm (TrajDataset dataset) {
		dataset_ = dataset;
		lut_ = new int[256];
		for (int i = 0; i < 256; i++) {
			lut_[i] = Color.getHSBColor(i/255f, 1f, 1f).getRGB();
		}
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
	
//	public void setCorrectDrift(boolean b) {
//		correctDrift_ = b;
//	}
//	
//	public boolean getCorrectDrift() {
//		return correctDrift_;
//	}

	public int getNPlotted() {
		return nPlotted_;
	}

	public int getNSkipped() {
		return nSkipped_;
	}

	private SmNode getCorrectedNode(SmNode node) {
		if (correctDrift_) {
			SmNode n = node;
			try {
				node = dataset_.correctDrift(node);
			} catch (OctaneException e) {
				IJ.log("Cannot compensate drift. No data available.");
				correctDrift_ = false;
				return n;
			}
		}
		return node;
	}

	void gaussianImage(ImageProcessor [] ips, double xs, double ys, double w, double z) {
		int zIndex;
		if (z < zMin_) {
			zIndex = 0;
		} else if (z > zMax_) {
			zIndex = lut_.length - 1;
		} else {
			zIndex = (int)((z - zMin_) / (zMax_ - zMin_) * lut_.length);
		}
		gaussianImage(ips, xs, ys, w, zIndex);
	}

	void gaussianImage(ImageProcessor [] ips, double xs, double ys, double w, int zIndex) {
		
		if (ips.length != 3) {
			throw (new IllegalArgumentException()); 
		}
		
		int rgb = lut_[zIndex];
		int r = (rgb&0xff0000)>>16;
		int g = (rgb&0xff00)>>8;
		int b = rgb&0xff;
		int width = ips[0].getWidth();
		int height = ips[0].getHeight();
		for (int x = FastMath.max(0, (int)(xs - 3*w)); x < FastMath.min(width, (int)(xs + 3*w)); x ++) {
			for (int y = FastMath.max(0, (int)(ys - 3*w)); y < FastMath.min(height, (int)(ys + 3*w)); y++) {
				double v = 100 * FastMath.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*w*w) );
				
				ips[0].setf(x, y, (float)v * r + ips[0].getf(x,y));
				ips[1].setf(x, y, (float)v * g + ips[1].getf(x,y));
				ips[2].setf(x, y, (float)v * b + ips[2].getf(x,y));
			}
		}
	}

	void gaussianImage(ImageProcessor ip, double xs, double ys, double w) {
		for (int x = FastMath.max(0, (int)(xs - 3*w)); x < FastMath.min(ip.getWidth(), (int)(xs + 3*w)); x ++) {
			for (int y = FastMath.max(0, (int)(ys - 3*w)); y < FastMath.min(ip.getHeight(), (int)(ys + 3*w)); y++) {
				double v = 100 * FastMath.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*w*w) );
				ip.setf(x, y, (float)v + ip.getf(x,y));
			}
		}
	}

	ColorProcessor renderRGB(FloatProcessor [] ips) {
		if (ips.length != 3) {
			throw (new IllegalArgumentException()); 
		}
		
		double max = 0 ;
		for (int i = 0; i < 3; i ++) {
			FloatProcessor ip = ips[i];
			float [] pixels = (float [])ip.getPixels();
			
			for (int j = 0; j < pixels.length; j++) {
				max = FastMath.max(max, pixels[j]);
			}
		}
		
		ColorProcessor cp = new ColorProcessor(ips[0].getWidth(), ips[0].getHeight());
		
		for (int i = 0; i < cp.getWidth() * cp.getHeight(); i++) {
			int r = (int)(ips[0].getf(i) * 255 / max);
			int g = (int)(ips[1].getf(i) * 255 / max);
			int b = (int)(ips[2].getf(i) * 255 / max);
			int rgb = r << 16 | g << 8 | b ;
			cp.set(i,rgb);
		}
		
		return cp;
		
	}
	
	public void constructPalm(final ImagePlus imp, final int [] selected) {
		nPlotted_ = 0;
		nSkipped_ = 0;
		
		PalmType palmType = PalmParameters.getPalmType();
		
		palmScaleFactor_ = PalmParameters.palmScaleFactor_;
		palmPSFWidth_ = PalmParameters.palmPSFWidth_;
		zRenderMode_ = PalmParameters.getZRenderMode(); 
		zMin_ = PalmParameters.zMin_;
		zMax_ = PalmParameters.zMax_;
		
		correctDrift_ = GlobalPrefs.compensateDrift_;

		final Rectangle rect = getCurrentROI(imp);
		
		if (palmType == PalmType.TIMELAPSE) {
			
			ImageStack stack = constructPalmTimeLapse(imp, rect, selected);

			if (stack != null) {
				ImagePlus img = imp.createImagePlus();
				img.setStack("PALMStack-" + imp.getTitle(), stack);
				img.show();
			}

		} else {
			
			ImageProcessor ip = null; 

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

	ImageStack constructPalmTimeLapse(final ImagePlus imp, final Rectangle rect, final int [] selected) {

		class MySwingWorker extends SwingWorker<ImageStack, Void> {
			ImageStack stack_ = null;
			
			public ImageStack doInBackground() {
				ImageStack is =  new ImageStack((int)(rect.width * palmScaleFactor_), (int)(rect.height * palmScaleFactor_));
				for (int i = 0; i < imp.getStack().getSize(); i++) {
					ImageProcessor ip = new FloatProcessor((int)(rect.width * palmScaleFactor_), (int)(rect.height * palmScaleFactor_));
					is.addSlice(""+i, ip);
				}
				for ( int i = 0; i < selected.length; i ++) {
					Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
					rendeTrajectory(traj, rect, is);
					firePropertyChange("Progress", i, i + 1);
				}
				return is;
			}

			@Override
			public void done() {
				try {
					stack_ = get();
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
			}
			
			ImageStack getResult() {
				return stack_;
			}
		}
		
		MySwingWorker task = new MySwingWorker();
		task.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName() == "Progress") {
					IJ.showProgress((Integer) evt.getNewValue(), imp.getStack().getSize());
				}
			}
		});
		
		task.execute();
		
		return task.getResult();
	}
	
	ImageProcessor constructPalmTypeHeadOrTail(Rectangle rect, int [] selected, boolean isHead) {
		double xs, ys;
		double psdWidth = palmPSFWidth_ * palmScaleFactor_;

		ImageProcessor ip = null;
		FloatProcessor ips[] = null;
		
		int w = (int) (rect.width * palmScaleFactor_);
		int h = (int) (rect.height * palmScaleFactor_);
		
		switch (zRenderMode_) {
		case NONE:
			ip = new FloatProcessor(w, h);
			break;
		case COLOR:
			//ip = new ColorProcessor(w,h);
			ips = new FloatProcessor[3];
			for (int i = 0; i < 3; i++) {
				ips[i] = new FloatProcessor(w,h);
			}
			break;
		case STACK:
			break;			
		}

		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
//			if (correctDrift_ && traj.marked){
//				continue;
//			}
				SmNode node;
				if (isHead) {
					node = traj.get(0);
				} else {
					node = traj.get(traj.size()-1);
				}
				node = getCorrectedNode(node);
				xs = (node.x - rect.x)* palmScaleFactor_;
				ys = (node.y - rect.y)* palmScaleFactor_;
				
				switch (zRenderMode_) {
				case NONE:
					gaussianImage(ip, xs, ys, psdWidth);
					break;
				case COLOR:
					double zs = node.z;
					gaussianImage(ips, xs, ys, psdWidth, zs);
					break;
				case STACK:
					break;
				}
				nPlotted_ ++;
		}
		
		if (zRenderMode_ == ZRenderMode.COLOR) {
			ip = renderRGB(ips);
		}

		return ip;
	}	
	
	ImageProcessor constructPalmTypeAverage(Rectangle rect, int [] selected) {
		double xx, yy, zz, xx2, yy2, zz2, xs, ys, zs;

		//double psdWidth = palmPSFWidth_ * palmScaleFactor_;
		double psdWidth = palmPSFWidth_ * palmScaleFactor_;
		
		ImageProcessor ip = null;
		FloatProcessor ips[] = null;
		
		int w = (int) (rect.width * palmScaleFactor_);
		int h = (int) (rect.height * palmScaleFactor_);
		
		switch (zRenderMode_) {
		case NONE:
			ip = new FloatProcessor(w, h);
			break;
		case COLOR:
			//ip = new ColorProcessor(w,h);
			ips = new FloatProcessor[3];
			for (int i = 0; i < 3; i++) {
				ips[i] = new FloatProcessor(w,h);
			}
			break;
		case STACK:
			break;			
		}

		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
			//				if (correctDrift_ && traj.marked){
			//					continue;
			//				}
			SmNode node;
			node = traj.get(0);
			node = getCorrectedNode(node);
			xx= node.x;
			yy= node.y;
			zz = node.z;
			xx2 = xx*xx;
			yy2 = yy*yy;
			zz2 = zz*zz;

			for (int j = 1; j < traj.size(); j++ ) {
				node = traj.get(j);
				node = getCorrectedNode(node);
				xx += node.x;
				yy += node.y;
				zz += node.z;
				xx2 += node.x * node.x;
				yy2 += node.y * node.y;
				zz2 += node.z * node.z;
			}

			xx /= traj.size();
			yy /= traj.size();
			zz /= traj.size();
			xx2 /= traj.size();
			yy2 /= traj.size();
			zz2 /= traj.size();

			if (xx2 - xx * xx < palmThreshold_ && yy2 - yy * yy < palmThreshold_) {
				xs = (xx - rect.x)* palmScaleFactor_;
				ys = (yy - rect.y)* palmScaleFactor_;
				
				switch (zRenderMode_) {
				case NONE:
					gaussianImage(ip, xs, ys, psdWidth);
					break;
				case COLOR:
					zs = zz;
					gaussianImage(ips, xs, ys, psdWidth, zs);
					break;
				case STACK:
					break;
				}
				nPlotted_ ++;
			} else {
				nSkipped_ ++;
			}
		}

		if (zRenderMode_ == ZRenderMode.COLOR) {
			ip = renderRGB(ips);
		}

		return ip;
	}

	ImageProcessor constructPalmTypeAllPoints(Rectangle rect, int [] selected) {
		double xs, ys;

		double psdWidth = palmPSFWidth_ * palmScaleFactor_;

		ImageProcessor ip = null;
		FloatProcessor ips[] = null;
		
		int w = (int) (rect.width * palmScaleFactor_);
		int h = (int) (rect.height * palmScaleFactor_);
		
		switch (zRenderMode_) {
		case NONE:
			ip = new FloatProcessor(w, h);
			break;
		case COLOR:
			//ip = new ColorProcessor(w,h);
			ips = new FloatProcessor[3];
			for (int i = 0; i < 3; i++) {
				ips[i] = new FloatProcessor(w,h);
			}
			break;
		case STACK:
			break;			
		}

		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
			//				if (correctDrift_ && traj.marked){
			//					continue;
			//				}
			for (int j = 0; j < traj.size(); j++ ) {
				SmNode node;
				node = traj.get(j);
				node = getCorrectedNode(node);
				xs = (node.x - rect.x)* palmScaleFactor_;
				ys = (node.y - rect.y)* palmScaleFactor_;
				
				switch (zRenderMode_) {
				case NONE:
					gaussianImage(ip, xs, ys, psdWidth);
					break;
				case COLOR:
					double zs = node.z;
					gaussianImage(ips, xs, ys, psdWidth, zs);
					break;
				case STACK:
					break;
				}
				nPlotted_ ++;
			}
		}

		if (zRenderMode_ == ZRenderMode.COLOR) {
			ip = renderRGB(ips);
		}

		return ip;
	}
	
	void rendeTrajectory(Trajectory traj, Rectangle rect, ImageStack stack) {
		if (traj == null ) {
			return;
		}
		for (int i = 0; i < traj.size(); i++ ) {
			SmNode node = traj.get(i);
			node = getCorrectedNode(node);
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
