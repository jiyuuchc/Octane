//FILE:          Browser.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 1/16/11
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.HistogramWindow;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;

public class Browser implements ClipboardOwner{

	ImagePlus imp_ = null;
	TrajDataset dataset_ = null;
	//TrajsTable trajsTable_;
	//NodesTable nodesTable_;
	String path_;
	Animator animator_ = null;
	BrowserWindow browserWindow_ = null;

	public Browser(ImagePlus imp) {
		super();		
		setupPath(imp);
	}
	
	public void setup() throws IOException, ClassNotFoundException {
		loadDataset();
		createWindow();
	}

	public void setup(TrajDataset data) {
		dataset_ = data;
		createWindow();
	}
	
	public void setup(Vector<SmNode> nodes) {
		dataset_ = new TrajDataset();
		dataset_.setNodes(nodes);
		dataset_.buildTrajectoriesFromNodes();
		
		createWindow();
	}

	void createWindow() {
		browserWindow_ = new BrowserWindow(this);
		browserWindow_.setVisible(true);
	}

	void setupPath(ImagePlus imp) {
		path_ = null;
		imp_ = imp;
		FileInfo fi = imp.getOriginalFileInfo();
		if (fi != null) {
			path_ = fi.directory; 
		} 
	}

	public BrowserWindow getWindow() {
		return browserWindow_;
	}

	public Vector<Trajectory> getTrajectories() {
		return dataset_.getTrajectories();
	}

	void selectTrajectoriesWithinRoi() {
		Roi roi = imp_.getRoi();
		if (roi == null) {
			return;
		}
		boolean firstSel = true;
		for (int i = 0; i < getTrajectories().size(); i++) {
			Trajectory t = getTrajectories().get(i);
			for (int j = 0; j< t.size(); j++) {
				if (roi.contains( (int)t.get(j).x, (int)t.get(j).y)) {
					if (firstSel) {
						browserWindow_.selectTrajectoriesByIndex(i);
						firstSel = false;
					} else {
						browserWindow_.addTrajectoriesToSelection(i);
					}
					break;
				}
			}
		}
	}
	
	void copySelectedTrajectories() {
		StringBuilder buf = new StringBuilder();
		int [] selected = browserWindow_.getSelectedTrajectories();
		Trajectory traj;
		for (int i = 0; i < selected.length; i++) {
			traj = getTrajectories().get(selected[i]);
			for (int j = 0; j < traj.size(); j++) {
				buf.append(String.format("%10.4f, %10.4f, %10d, %5d%n", traj.get(j).x, traj.get(j).y, traj.get(j).frame, i));
			}
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection contents = new StringSelection(buf.toString());
		clipboard.setContents(contents, this);		
	}

	void findMolecule() {
		ImageCanvas canvas = imp_.getCanvas();
		Point p = canvas.getCursorLoc();
		int frame = imp_.getSlice();
		
		int index = 0;
		boolean found = false;
		int lastIndex = dataset_.getTrajectories().size();
		while (!found && index < lastIndex) {
			Trajectory t = dataset_.getTrajectories().get(index);
			if (t.get(0).frame <= frame && t.get(t.size()-1).frame >= frame) {
				int fi = frame - t.get(0).frame;
				if (fi >= t.size()) { 
					fi = t.size() - 1;
				}
				while (t.get(fi).frame > frame) {
					fi --;
				}
				if (t.get(fi).frame == frame) {
					if ( Math.abs(t.get(fi).x - p.x) < 2.5 && Math.abs(t.get(fi).y - p.y) < 2.5) {
						found = true;
					}
				}
			}
			index ++;
		}
		if (found) {
			browserWindow_.selectTrajectoriesByIndex(index - 1);
		}
	}

/*	private FloatProcessor gaussianImage(ImageProcessor img) {
		int width = img.getWidth();
		int height = img.getHeight();
		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmRatio_;
		FloatProcessor ip = new FloatProcessor(width, height);
		for (int x = 0; x < width; x ++) {
			for (int y = 0; y < height; y++) {
				double v = Math.exp( -((x*2-width) * (x*2-width) + (y*2-height)*(y*2-height))/(8.0*psdWidth*psdWidth) );
				ip.setf(x, y, (float)v);
			}
		}

		return ip;
	}*/

	void drawOverlay() {
		if (!Prefs.showOverlay_) {
			imp_.setOverlay(null);
			return;
		}
		GeneralPath path = new GeneralPath();
		int [] selected = browserWindow_.getSelectedTrajectories();
		for (int i = 0; i < selected.length; i++) {
			Trajectory v = getTrajectories().get(selected[i]);
			path.moveTo(v.get(0).x, v.get(0).y);
			for (int j = 1; j < v.size(); j++) {
				path.lineTo(v.get(j).x, v.get(j).y);
			}
		}
		imp_.setOverlay(path, Color.yellow, new BasicStroke(1f));			
	}
	
	public void drawBox() {
		SmNode node = browserWindow_.getCurrentNode();
		int x,y;
		if (node != null && imp_ != null) {
			imp_.setSlice(node.frame);
			x = (int) Math.round(node.x);
			y = (int) Math.round(node.y);
			imp_.setRoi(x-5,y-5,11,11);
			ImageCanvas canvas = imp_.getCanvas();
			Rectangle r = canvas.getSrcRect();
			int sx = canvas.screenX(x);
			int sy = canvas.screenY(y);
			if (sx < 4 || sx > r.width - 5 || sy < 4 || sy > r.height - 5) {
				int nx = Math.max(x - r.width/2, 0);
				int ny = Math.max(y - r.height/2, 0);
				if (nx + r.width > imp_.getWidth()) {
					nx = imp_.getWidth() - r.width;
				}
				if (ny + r.height > imp_.getHeight()){
					ny = imp_.getHeight() - r.height;
				}
				canvas.setSourceRect(new Rectangle(nx, ny, r.width, r.height));
				imp_.updateAndDraw();
			}
		}
	}
	
	public void constructIFS() {
		Rectangle rect;
		imp_.killRoi();
		rect = imp_.getProcessor().getRoi();

		ImageStack is =  new ImageStack(rect.width, rect.height);
		for (int i = 0; i < imp_.getNSlices(); i++) {
			is.addSlice(""+i, new FloatProcessor(rect.width, rect.height));
		}
		ImagePlus img = new ImagePlus("IFS", is);
		double psdWidth = 0.85;

		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectories().get(selected[i]);
			for (int j = 0; j < traj.size(); j++ ) {
				double xs = (traj.get(j).x - rect.x);
				double ys = (traj.get(j).y- rect.y);
				//IJ.log(String.format("%5d%6d%6d", rowIndex, x, y));
				for (int x = Math.max(0, (int)(xs - 3*psdWidth)); x < Math.min(rect.width, (int)(xs + 3*psdWidth)); x ++) {
					for (int y = Math.max(0, (int)(ys - 3*psdWidth)); y < Math.min(rect.height, (int)(ys + 3*psdWidth)); y++) {
						double v = Math.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*psdWidth*psdWidth) );
						FloatProcessor ip = (FloatProcessor) is.getProcessor(traj.get(j).frame);
						ip.setf(x, y, (float)v + ip.getf(x,y));
					}
				}
			} 
		}
		img.show();
		
	}
	
	public void constructPalm() {
		FloatProcessor ip = new FloatProcessor((int) (imp_.getProcessor().getWidth() * Prefs.palmRatio_) + 1, (int) (imp_.getProcessor().getHeight() * Prefs.palmRatio_) + 1);
		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmRatio_;
		int nPlotted = 0;
		int nSkipped = 0;
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectories().get(selected[i]);
			double xx = traj.get(0).x;
			double yy = traj.get(0).y;
			boolean converge = true;
			for (int j = 1; j < traj.size(); j++ ) {
				if (Math.abs(xx / j - traj.get(j).x) > Prefs.palmThreshold_ || Math.abs(yy / j - traj.get(j).y) > Prefs.palmThreshold_ ) {
					converge = false;
					break;
				}
				xx += traj.get(j).x;
				yy += traj.get(j).y;
			}
			if (converge) {
				xx /= traj.size();
				yy /= traj.size();
				double xs = xx * Prefs.palmRatio_;
				double ys = yy * Prefs.palmRatio_;

				for (int x = Math.max(0, (int)(xs - 3*psdWidth)); x < Math.min(ip.getWidth(), (int)(xs + 3*psdWidth)); x ++) {
					for (int y = Math.max(0, (int)(ys - 3*psdWidth)); y < Math.min(ip.getHeight(), (int)(ys + 3*psdWidth)); y++) {
						double v = Math.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*psdWidth*psdWidth) );
						ip.setf(x, y, (float)v + ip.getf(x,y));
					}
				}
				nPlotted ++;
			} else {
				nSkipped ++;
			}
		}
		ImagePlus img = new ImagePlus("PALM", ip);
		img.show();
		IJ.log(String.format("Plotted %d molecules, skipped %d molecules.", nPlotted, nSkipped));
	}

	public void constructMobilityMap() {
		Rectangle rect;
		imp_.killRoi();
		rect = imp_.getProcessor().getRoi();

		float [][] m = new float[rect.width][rect.height];
		float [][] n = new float[rect.width][rect.height];
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		int i,j;
		for (i =0; i < selected.length; i++) {
			Trajectory t = dataset_.getTrajectories().get(selected[i]);
			for (j = 1; j < t.size(); j++) {
				if ( rect.contains(t.get(j-1).x, t.get(j-1).y)) {
					int x = (int) t.get(j-1).x - rect.x + 1;
					int y = (int) t.get(j-1).y - rect.y + 1;
					double dx = (t.get(j).x - t.get(j-1).x)/(t.get(j).frame-t.get(j-1).frame);
					double dy = (t.get(j).y - t.get(j-1).y)/(t.get(j).frame-t.get(j-1).frame);
					double dr = Math.sqrt(dx*dx+dy*dy);
					n[x][y] += 1.0f;
					m[x][y] += dr;
				}
			}
		}
		
		for (i = 0; i < rect.width; i ++) {
			for (j = 0; j < rect.height; j++) {
				if (n[i][j] > 0) {
					m[i][j] = m[i][j] / n[i][j];
				}
			}
		}
		
		FloatProcessor fp = new FloatProcessor(m);
		FloatProcessor np = new FloatProcessor(n);
		
		new ImagePlus(imp_.getTitle() + " MobilityMap", fp).show();
		new ImagePlus(imp_.getTitle() + " MobilityCnt", np).show();
	}
	
	public void constructFlowMap() {
		Rectangle rect;
		imp_.killRoi();
		rect = imp_.getProcessor().getRoi();

		float [][] dxs = new float[rect.width][rect.height];
		float [][] dys = new float[rect.width][rect.height];
		float [][] n = new float[rect.width][rect.height];
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		int i,j;
		for (i =0; i < selected.length; i++) {
			Trajectory t = dataset_.getTrajectories().get(selected[i]);
			for (j = 1; j < t.size(); j++) {
				if ( rect.contains(t.get(j-1).x, t.get(j-1).y)) {
					int x = (int) t.get(j-1).x - rect.x + 1;
					int y = (int) t.get(j-1).y - rect.y + 1;
					double dx = (t.get(j).x - t.get(j-1).x)/(t.get(j).frame-t.get(j-1).frame);
					double dy = (t.get(j).y - t.get(j-1).y)/(t.get(j).frame-t.get(j-1).frame);
					dxs[x][y] += dx;
					dys[x][y] += dy;
					n[x][y] += 1.0f;
				}
			}
		}
		
		float maxDx = -1.0f, maxDy = -1.0f;
		for (i = 0; i < rect.width; i ++) {
			for (j = 0; j < rect.height; j++) {
				if (n[i][j] > 0) {
					dxs[i][j] = dxs[i][j] / n[i][j];
					dys[i][j] = dys[i][j] / n[i][j];
					if (Math.abs(dxs[i][j]) > maxDx) 
						maxDx = Math.abs(dxs[i][j]);
					if (Math.abs(dys[i][j]) > maxDx) 
						maxDy = Math.abs(dys[i][j]);
				}
			}
		}
		
		
		GeneralPath gp = new GeneralPath();
		float max = (maxDx > maxDy? maxDx:maxDy) * 2.0f;
		for (i = 0; i < rect.width; i ++) {
			for (j = 0; j < rect.height; j++) {
				if (n[i][j] > 0) {
					dxs[i][j] = dxs[i][j] / max;
					dys[i][j] = dys[i][j] / max;
					gp.moveTo(rect.x + i + 0.5f, rect.y + j + 0.5f);
					gp.lineTo(rect.x + i + 0.5f + dxs[i][j], rect.y + j + 0.5f + dys[i][j]);					
				}
			}
		}
		
		FloatProcessor fp = new FloatProcessor(n);
		ImagePlus imp = new ImagePlus(imp_.getTitle() + " Flowmap", fp);
		imp.show();
		imp.setOverlay(gp, Color.yellow, new BasicStroke(1f));
		
	}

	public ImagePlus getImp() {
		return imp_;
	}

	public void selectMarked(boolean b) {
		browserWindow_.selectTrajectoriesByIndex(-1); //clear selection
		for (int i = 0; i < dataset_.getTrajectories().size(); i++) {
			if (getTrajectories().get(i).marked == b) {
				browserWindow_.addTrajectoriesToSelection(i);
			}
		}
	}

	public void rebuildTrajectories(){
		dataset_.buildTrajectoriesFromNodes();
		browserWindow_.updateNewData();
	}

	public void showLengthHistogram() {
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		short [] d = new short[selected.length];
		
		int min = 10000;
		int max = -1;
		for (int i = 0; i < selected.length; i++) {
			Trajectory t = getTrajectories().get(selected[i]);
			d[i] = (short) (t.getLength());
			if (d[i] > max) {
				max = d[i];
			}
			if (d[i]< min) {
				min = d[i];
			}
		}
		ShortProcessor ip = new ShortProcessor(1, d.length, d, null);
		ImagePlus imp = new ImagePlus("",ip);
		HistogramWindow hw = new HistogramWindow("Trajectory Length Histogram", imp, max-min);
		hw.setVisible(true);
		imp.close();
	} 

	public void showResidueHistogram() {
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		double [] d = new double[dataset_.getNodes().size()];
		int cnt = 0;
		for ( int i= 0; i < selected.length; i++) {
			Iterator<SmNode> itr = getTrajectories().get(selected[i]).iterator();
			while (itr.hasNext()) {
				d[cnt ++] = itr.next().reserved;
			}
		}
		FloatProcessor ip = new FloatProcessor(1, cnt, Arrays.copyOf(d, cnt));
		ImagePlus imp = new ImagePlus("", ip);
		HistogramWindow hw = new HistogramWindow("Residue Histogram", imp, Prefs.histogramBins_);
		hw.setVisible(true);
		imp.close();		
	}
	
	public void animate() {
		if (animator_ == null) {
			animator_ = new Animator(imp_);
			animator_.setLoop(true);
		}
		
		int index= browserWindow_.getSelectedTrajectoryIndex();
		if (index >=0) {
			animator_.animate(dataset_.getTrajectories().get(index));
		}
		
	}

	public void deleteSelectedTrajectories() {
		int [] selected = browserWindow_.getSelectedTrajectories();
		for (int i = 0; i < selected.length; i ++) {
			getTrajectories().get(i).deleted = true;
		}
	}

	String defaultSaveFilename() {
		final String s = path_ + File.separator + imp_.getTitle() + ".dataset";
		return s;
	}

	public void saveDataset(String pathname) {
		try {
			File file = new File(pathname);
			dataset_.saveDataset(file);
		} catch (IOException e) {
			IJ.showMessage("IOError: Failed to save file.");
		}
		
	}

	public void saveDataset() {
		saveDataset(defaultSaveFilename());
	}

	public void loadDataset(String pathname) throws IOException, ClassNotFoundException {
		File file = new File(pathname);
		dataset_ = TrajDataset.loadDataset(file);
	}

	public void loadDataset() throws IOException, ClassNotFoundException {
		loadDataset(defaultSaveFilename());
	}
	
	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// 
	}
}
