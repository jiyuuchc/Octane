//FILE:          FlowAnalysis.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 3/6/12
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
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatProcessor;

import java.awt.Rectangle;


/**
 * Carry out particle flow analysis
 * @author Ji-Yu
 *
 */
public class FlowAnalysis {
	
	static double renderingResolution_ = 1; // # of pixels

	TrajDataset dataset_;
	
	/**
	 * Constructor
	 * @param dataset The associated trajectory data
	 */
	public FlowAnalysis(TrajDataset dataset) {
		dataset_ = dataset;
	}
	
	/**
	 * Calculate and display mobility map.
	 * @param imp The image data
	 * @param selected A set of trajectories to be used for analysis
	 */
	public void constructMobilityMap(ImagePlus imp, int [] selected) {
		Rectangle rect;
		Roi roi = imp.getRoi();
		if (roi!=null && !roi.isArea()) {
			imp.killRoi(); 
		}
		rect = imp.getProcessor().getRoi();
 
		int w = (int)(rect.width / renderingResolution_) + 1;
		int h = (int)(rect.height / renderingResolution_) + 1;
		
	
		float [][] m = new float[w][h];
		float [][] n = new float[w][h];
		float [][] fx = new float[w][h];
		float [][] fy = new float[w][h];

		int i,j;
		for (i =0; i < selected.length; i++) {
			Trajectory t = dataset_.getTrajectoryByIndex(selected[i]);
			for (j = 1; j < t.size(); j++) {
				int df = t.get(j).frame - t.get(j-1).frame;
				if ( df == 1 && rect.contains(t.get(j-1).x, t.get(j-1).y)) {
					int x = (int) ((t.get(j-1).x - rect.x) / renderingResolution_);
					int y = (int) ((t.get(j-1).y - rect.y) / renderingResolution_) ;
					n[x][y] += 1.0f;
					m[x][y] += t.get(j).distance2(t.get(j-1));
					fx[x][y] += (t.get(j).x - t.get(j-1).x);
					fy[x][y] += (t.get(j).y - t.get(j-1).y);
				}
			}
		}

		for (i = 0; i < rect.width; i ++) {
			for (j = 0; j < rect.height; j++) {
				if (n[i][j] > 0) {
					m[i][j] = m[i][j] / n[i][j];
					fx[i][j] = fx[i][j] / n[i][j];
					fy[i][j] = fy[i][j] / n[i][j];
				}
			}
		}
		
		ImageStack stack = new ImageStack(w, h);
		stack.addSlice("MobilityMap", new FloatProcessor(m));
		stack.addSlice("XDisplacement",new FloatProcessor(fx));
		stack.addSlice("YDisplacement",new FloatProcessor(fy));
		stack.addSlice("MobilityCnt", new FloatProcessor(n));
		new ImagePlus(imp.getTitle() + " MobilityMap", stack).show();
	}
	
//	/**
//	 * Construct flow map.
//	 */
//	public void constructFlowMap(ImagePlus imp, int [] selected) {
//		Rectangle rect;
//		Roi roi = imp.getRoi();
//		if (roi!=null && !roi.isArea()) {
//			imp.killRoi(); 
//		}
//		rect = imp.getProcessor().getRoi();
//
//		float [][] dxs = new float[rect.width][rect.height];
//		float [][] dys = new float[rect.width][rect.height];
//		float [][] n = new float[rect.width][rect.height];
////		int [] selected = frame_.getTrajsTable().getSelectedTrajectoriesOrAll();
//		int i,j;
//		for (i =0; i < selected.length; i++) {
//			Trajectory t = dataset_.getTrajectoryByIndex(selected[i]);
//			for (j = 1; j < t.size(); j++) {
//				if ( rect.contains(t.get(j-1).x, t.get(j-1).y)) {
//					int x = (int) t.get(j-1).x - rect.x ;
//					int y = (int) t.get(j-1).y - rect.y ;
//					double dx = (t.get(j).x - t.get(j-1).x)/(t.get(j).frame-t.get(j-1).frame);
//					double dy = (t.get(j).y - t.get(j-1).y)/(t.get(j).frame-t.get(j-1).frame);
//					dxs[x][y] += dx;
//					dys[x][y] += dy;
//					n[x][y] += 1.0f;
//				}
//			}
//		}
//
//		float maxDx = -1.0f, maxDy = -1.0f;
//		for (i = 0; i < rect.width; i ++) {
//			for (j = 0; j < rect.height; j++) {
//				if (n[i][j] > 0) {
//					dxs[i][j] = dxs[i][j] / n[i][j];
//					dys[i][j] = dys[i][j] / n[i][j];
//					if (Math.abs(dxs[i][j]) > maxDx) 
//						maxDx = Math.abs(dxs[i][j]);
//					if (Math.abs(dys[i][j]) > maxDx) 
//						maxDy = Math.abs(dys[i][j]);
//				}
//			}
//		}
//		
//		GeneralPath gp = new GeneralPath();
//		float max = (maxDx > maxDy? maxDx:maxDy) * 2.0f;
//		for (i = 0; i < rect.width; i ++) {
//			for (j = 0; j < rect.height; j++) {
//				if (n[i][j] > 0) {
//					double x1 = dxs[i][j] / max;
//					double y1 = dys[i][j] / max;
//					double r1 = Math.sqrt(x1*x1 + y1 * y1);
//					gp.moveTo(i + 0.5f, j + 0.5f);
//					gp.lineTo(i + 0.5f + x1, j + 0.5f + y1);
//					if (r1 > 0.2) {
//						double x3 = x1 - x1 / r1 * 0.3;
//						double y3 = y1 - y1 / r1 * 0.3;
//						double x4 = x3 + y1 / r1 * 0.3 * 0.45;
//						double y4 = y3 - x1 / r1 * 0.3 * 0.45;
//						double x5 = x3 - y1 / r1 * 0.3 * 0.45;
//						double y5 = y3 + x1 / r1 * 0.3 * 0.45;
//						gp.moveTo(i + 0.5f + x4, j + 0.5f + y4);
//						gp.lineTo(i + 0.5f + x1, j + 0.5f + y1);
//						gp.lineTo(i + 0.5f + x5, j + 0.5f + y5);
//					}
//				}
//			}
//		}
//		
//		FloatProcessor fp = new FloatProcessor(n);
//		ImagePlus imp2 = new ImagePlus(imp.getTitle() + " Flowmap", fp);
//		imp2.show();
//		imp2.setOverlay(gp, Color.yellow, new BasicStroke(1f));
//		
//	}

}
