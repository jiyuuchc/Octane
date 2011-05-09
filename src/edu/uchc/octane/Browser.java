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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.GeneralPath;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.HistogramWindow;
import ij.gui.ImageCanvas;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;

/**
 * Controller of the the browser window.
 */
public class Browser implements ClipboardOwner{

	ImagePlus imp_ = null;

	TrajDataset dataset_ = null;
	//TrajsTable trajsTable_;
	//NodesTable nodesTable_;

	protected String path_;
	
	protected Animator animator_ = null;
	
	BrowserWindow browserWindow_ = null;

	/**
	 * Constructor
	 *
	 * @param imp the image
	 */
	public Browser(ImagePlus imp) {
		super();		
		setupPath(imp);
	}
	
	/**
	 * Load dataset from default disk location
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	public void setup() throws IOException, ClassNotFoundException {
		loadDataset();
		createWindow();
	}

	/**
	 * Setup window using provided dataset
	 *
	 * @param data the dataset
	 */
	public void setup(TrajDataset data) {
		dataset_ = data;
		createWindow();
	}

	/**
	 * Setup window from positions of molecules.
	 *
	 * @param nodes positions of molecules
	 */
	public void setup(SmNode[][] nodes) {
		dataset_ = TrajDataset.createDatasetFromNodes(nodes);
		createWindow();
	}

	protected void createWindow() {
		browserWindow_ = new BrowserWindow(this);
		browserWindow_.setVisible(true);
	}

	private void setupPath(ImagePlus imp) {
		path_ = null;
		imp_ = imp;
		FileInfo fi = imp.getOriginalFileInfo();
		if (fi != null) {
			path_ = fi.directory; 
		} 
		
		imp.getCanvas().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 ) {
					findMolecule();
				}
			}			
		});
	}

	/**
	 * Returns the window object.
	 *
	 * @return the window
	 */
	public BrowserWindow getWindow() {
		return browserWindow_;
	}

	/**
	 * Gets the dataset.
	 *
	 * @return the data
	 */
	public TrajDataset getData() {
		return dataset_;
	}

	protected void selectTrajectoriesWithinRoi() {
		Roi roi = imp_.getRoi();
		if (roi == null) {
			return;
		}
		if (roi instanceof PointRoi && ((PointRoi) roi).getNCoordinates() == 1) {
			int frame = imp_.getFrame();
			int x = ((PointRoi) roi).getXCoordinates() [0];
			int y = ((PointRoi) roi).getYCoordinates() [0];
			findMolecule(x,y,frame);
		} else {
			boolean firstSel = true;
			for (int i = 0; i < dataset_.getSize(); i++) {
				Trajectory t = dataset_.getTrajectoryByIndex(i);
				for (int j = 0; j< t.size(); j++) {
					if (roi.contains( (int)t.get(j).x, (int)t.get(j).y)) {
						if (firstSel) {
							browserWindow_.selectTrajectoryByIndex(i);
							firstSel = false;
						} else {
							browserWindow_.addTrajectoriesToSelection(i);
						}
						break;
					}
				}
			}
		}
	}
	
	protected void copySelectedTrajectories() {
		StringBuilder buf = new StringBuilder();
		int [] selected = browserWindow_.getSelectedTrajectories();
		Trajectory traj;
		for (int i = 0; i < selected.length; i++) {
			traj = dataset_.getTrajectoryByIndex(selected[i]);
			for (int j = 0; j < traj.size(); j++) {
				buf.append(String.format("%10.4f, %10.4f, %10d, %5d%n", traj.get(j).x, traj.get(j).y, traj.get(j).frame, i));
			}
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection contents = new StringSelection(buf.toString());
		clipboard.setContents(contents, this);		
	}

	protected void findMolecule(int x, int y, int f) {
		int index = 0;
		int fi = 0;
		boolean found = false;
		int lastIndex = dataset_.getSize();
		while (!found && index < lastIndex) {
			Trajectory t = dataset_.getTrajectoryByIndex(index);
			if (t != null && t.size() > 0 && t.get(0).frame <= f && t.get(t.size()-1).frame >= f) {
				fi = f - t.get(0).frame;
				if (fi >= t.size()) { 
					fi = t.size() - 1;
				}
				while (t.get(fi).frame > f) {
					fi --;
				}
				if (t.get(fi).frame == f) {
					if ( Math.abs(t.get(fi).x - x) < 2.5 && Math.abs(t.get(fi).y - y) < 2.5) {
						found = true;
					}
				}
			}
			index ++;
		}
		if (found) {
			browserWindow_.selectTrajectoryAndNodeByIndex(index - 1 , fi);
		}		
	}
	
	protected void findMolecule() {
		ImageCanvas canvas = imp_.getCanvas();
		Point p = canvas.getCursorLoc();
		int frame = imp_.getSlice();
		
		findMolecule(p.x, p.y, frame);
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

	protected void drawOverlay() {
		if (!Prefs.showOverlay_) {
			imp_.setOverlay(null);
			return;
		}
		GeneralPath path = new GeneralPath();
		int [] selected = browserWindow_.getSelectedTrajectories();
		for (int i = 0; i < selected.length; i++) {
			Trajectory v = dataset_.getTrajectoryByIndex(selected[i]);
			path.moveTo(v.get(0).x, v.get(0).y);
			for (int j = 1; j < v.size(); j++) {
				path.lineTo(v.get(j).x, v.get(j).y);
			}
		}
		imp_.setOverlay(path, Color.yellow, new BasicStroke(1f));			
	}
	
	protected void drawBox() {
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
	
	/**
	 * Construct ifs stack.
	 */
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
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
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
	
	/**
	 * Construct PALM image.
	 */
	public void constructPalm() {
		FloatProcessor ip = new FloatProcessor((int) (imp_.getProcessor().getWidth() * Prefs.palmRatio_) + 1, (int) (imp_.getProcessor().getHeight() * Prefs.palmRatio_) + 1);
		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmRatio_;
		int nPlotted = 0;
		int nSkipped = 0;
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		for ( int i = 0; i < selected.length; i ++) {
			Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
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

	/**
	 * Construct mobility map.
	 */
	public void constructMobilityMap() {
		Rectangle rect;
		imp_.killRoi();
		rect = imp_.getProcessor().getRoi();

		float [][] m = new float[rect.width][rect.height];
		float [][] n = new float[rect.width][rect.height];
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		int i,j;
		for (i =0; i < selected.length; i++) {
			Trajectory t = dataset_.getTrajectoryByIndex(selected[i]);
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
	
	/**
	 * Construct flow map.
	 */
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
			Trajectory t = dataset_.getTrajectoryByIndex(selected[i]);
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

	/**
	 * Gets the Imagej image.
	 *
	 * @return the imageplus
	 */
	public ImagePlus getImp() {
		return imp_;
	}

	/**
	 * Select all (un)marked trajectory in the trajectory table.
	 *
	 * @param b select marked if true, unmarked if false
	 */
	public void selectMarked(boolean b) {
		browserWindow_.selectTrajectoryByIndex(-1); //clear selection
		for (int i = 0; i < dataset_.getSize(); i++) {
			if (dataset_.getTrajectoryByIndex(i).marked == b) {
				browserWindow_.addTrajectoriesToSelection(i);
			}
		}
	}

	/**
	 * Rebuild trajectories.
	 */
	public void rebuildTrajectories(){
		dataset_.reTrack();
		browserWindow_.updateNewData();
	}

	/**
	 * Show trajectory length histogram.
	 */
	public void showLengthHistogram() {
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		short [] d = new short[selected.length];
		
		int min = 10000;
		int max = -1;
		for (int i = 0; i < selected.length; i++) {
			Trajectory t = dataset_.getTrajectoryByIndex(selected[i]);
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

	/**
	 * Show fitting residue histogram.
	 */
	public void showResidueHistogram() {
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		//ArrayList<Double> d = new ArrayList<Double>();
		int numOfNodes = 0;
		for ( int i= 0; i < selected.length; i++) {
			numOfNodes += dataset_.getTrajectoryByIndex(selected[i]).size();
		}
		double [] d = new double[numOfNodes];
		int cnt = 0;
		for ( int i= 0; i < selected.length; i++) {
			Iterator<SmNode> itr = dataset_.getTrajectoryByIndex(selected[i]).iterator();
			while (itr.hasNext()) {
				d[cnt++] = itr.next().reserved;
			}
		}
		FloatProcessor ip = new FloatProcessor(1, d.length, d);
		ImagePlus imp = new ImagePlus("", ip);
		HistogramWindow hw = new HistogramWindow("Residue Histogram", imp, Prefs.histogramBins_);
		hw.setVisible(true);
		imp.close();		
	}
	
	/**
	 * Show displacement histogram.
	 * 
	 * @param stepSize the stepsize for calculating the displacement
	 */
	public void showDisplacementHistogram(int stepSize) {
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		ArrayList<Double> dl = new ArrayList<Double>();
		for (int i = 0; i < selected.length; i++) {
			Trajectory t = dataset_.getTrajectoryByIndex(selected[i]);
			for (int j = 0; j < t.size() - stepSize; j++) {
				int k = j + 1;
				int frame = t.get(j).frame;
				while ( k < t.size()) {
					if (t.get(k).frame - frame < stepSize) {
						k++;
					} else if (t.get(k).frame - frame == stepSize) {
						dl.add(t.get(j).distance(t.get(k)));
						break;
					} else {
						break;
					}
				}
			}
			IJ.showProgress(i, selected.length);
		}
		double [] d = new double[dl.size()];
		for (int i = 0; i < dl.size(); i ++) {
			d[i] = dl.get(i).doubleValue();
		}
		if (d.length <= 1) {
			IJ.showMessage("Not enough data point. Stepsize too large?");
			return;
		}
		FloatProcessor ip = new FloatProcessor(1, d.length, d);
		ImagePlus imp = new ImagePlus("", ip);
		HistogramWindow hw = new HistogramWindow("Displacement Histogram", imp, Prefs.histogramBins_);
		hw.setVisible(true);
		imp.close();
	}
	
	/**
	 * Show mean square displacement.
	 */
	public void showMSD() {
		int [] selected = browserWindow_.getSelectedTrajectoriesOrAll();
		ArrayList<Double> dl = new ArrayList<Double>();
		ArrayList<Integer> nl = new ArrayList<Integer>();
		for (int i = 0; i < selected.length; i ++) {
			Trajectory t = dataset_.getTrajectoryByIndex(selected[i]);
			for (int j = 0; j < t.size()-1; j++) {
				int frame = t.get(j).frame;
				for (int k = j + 1; k < t.size(); k++) {
					int deltaframe = t.get(k).frame - frame;
					while (deltaframe > dl.size()) {
						dl.add(0.0);
						nl.add(0);
					}
					dl.set(deltaframe - 1, dl.get(deltaframe-1) + t.get(j).distance2(t.get(k))); 
					nl.set(deltaframe - 1, nl.get(deltaframe-1) + 1);
				}
			}
			IJ.showProgress(i, selected.length);
		}
		double [] x = new double [dl.size() + 1];
		double [] y = new double [dl.size() + 1];
		x[0] = 0;
		y[0] = 0;
		for (int i = 0; i < dl.size(); i++) {
			x[i+1] = 1.0 + i;
			y[i+1] = dl.get(i).doubleValue() / nl.get(i).intValue();
		}
		if (x.length > 0) {
			Plot plotWin = new Plot("MSD Plot", "T/T-frame", "MSD (pixel^2)", x, y);
			plotWin.show();
			plotWin.addPoints(x, y, Plot.BOX);
		} 
	}

	/**
	 * Animate the current trajectory.
	 */
	public void animate() {
		if (animator_ == null) {
			animator_ = new Animator(imp_);
			animator_.setLoop(true);
		}
		
		int index= browserWindow_.getSelectedTrajectoryIndex();
		if (index >=0) {
			animator_.animate(dataset_.getTrajectoryByIndex(index));
		}
		
	}

	/**
	 * Delete all selected trajectories from trajectory table.
	 */
	public void deleteSelectedTrajectories() {
		int [] selected = browserWindow_.getSelectedTrajectories();
		for (int i = 0; i < selected.length; i ++) {
			dataset_.getTrajectoryByIndex(i).deleted = true;
		}
	}

	/**
	 * Returns the default pathname for saving the dataset.
	 *
	 * @return the pathname
	 */
	protected String defaultSaveFilename() {
		final String s = path_ + File.separator + imp_.getTitle() + ".dataset";
		return s;
	}

	/**
	 * Save the dataset to disk.
	 *
	 * @param pathname the pathname
	 */
	public void saveDataset(String pathname) {
		try {
			File file = new File(pathname);
			dataset_.saveDataset(file);
		} catch (IOException e) {
			IJ.showMessage("IOError: Failed to save file.");
		}
		
	}

	/**
	 * Save the dataset using the default pathname (imagetitle + .dataset).
	 */
	public void saveDataset() {
		saveDataset(defaultSaveFilename());
	}

	/**
	 * Load dataset from disk.
	 *
	 * @param pathname the pathname
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	public void loadDataset(String pathname) throws IOException, ClassNotFoundException {
		File file = new File(pathname);
		dataset_ = TrajDataset.loadDataset(file);
	}

	/**
	 * Load dataset from disk at default save location.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	public void loadDataset() throws IOException, ClassNotFoundException {
		loadDataset(defaultSaveFilename());
	}
	
	/* (non-Javadoc)
	 * @see java.awt.datatransfer.ClipboardOwner#lostOwnership(java.awt.datatransfer.Clipboard, java.awt.datatransfer.Transferable)
	 */
	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// 
	}

	/**
	 * Export dataset to text.
	 * Each molecule occupy one line: x, y, frame, residue
	 * 
	 * @param file the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void exportNodes(File file) throws IOException {
		dataset_.writePositionsToText(file);		
	}

	/**
	 * Export selected trajectories to text.
	 * Each molecule occupy one line: x, y, frame, height, trackIdx
	 * 
	 * @param file the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void exportTrajectories(File file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		int [] selected = browserWindow_.getSelectedTrajectories();
		Trajectory traj;
		for (int i = 0; i < selected.length; i++) {
			traj = dataset_.getTrajectoryByIndex(selected[i]);
			for (int j = 0; j < traj.size(); j++) {
				SmNode n = traj.get(j);
				bw.append(n.toString());
				bw.append(", " + i + "\n");
			}
		}
		bw.close();
	}
}
