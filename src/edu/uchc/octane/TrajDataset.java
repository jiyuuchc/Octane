//FILE:          TrajDataset.java
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

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class TrajDataset {

	private Vector<Trajectory> trajs_;
	private String path_;
	
	public TrajDataset(ImagePlus imp) {
		FileInfo fi = imp.getOriginalFileInfo();
		if (fi != null) {
			path_ = fi.directory; 
		} else {
			IJ.showMessage("Can't find trajectories location");
		}
		trajs_ = new Vector<Trajectory>();
	}

	public void posToTracks(File outfile) throws IOException{
		File file = new File(path_ + File.separator + "analysis" + File.separator + "positions");
		
		Tracker tracker = new Tracker(file, Prefs.trackerMaxDsp_, Prefs.trackerMaxBlinking_);
		tracker.doTracking();
		tracker.toDisk(outfile);
		trajs_ = tracker.getTrajs();
		
		file = new File(path_ + File.separator + "analysis" + File.separator + "notes");
		if (file.exists()) {
			file.delete();
		}
	}
	
	public Vector<Trajectory> getTrajectories() {
		if (trajs_ == null || trajs_.size() == 0) {
			try {
				readTrajs();
			} catch (Exception e) {
				IJ.showMessage(e.toString() + "\n" + e.getMessage());
				return null;
			}
		}

		if (trajs_.size() > 0) {		
			return trajs_;
		}
		else
			return null;
	}

	void readTrajs() throws IOException {
		File file = new File(path_ + File.separator + "analysis" + File.separator + "trajs");
		BufferedReader br;

		if (! file.exists()) {
			IJ.log("No Trajectory available. Build the trajectories.");
			posToTracks(file);
			return;
		}

		Trajectory oneTraj = new Trajectory();
		int cur_cnt = -1;
		String line;
		br = new BufferedReader(new FileReader(file));

		while (null != (line = br.readLine())) {
			int c = line.lastIndexOf(',');
			int cnt = Integer.parseInt(line.substring(c + 1).trim());
			if (cur_cnt < cnt) {
				oneTraj = new Trajectory();
				trajs_.add(oneTraj);
				cur_cnt = cnt;
			}
			oneTraj.add(new SmNode(line.substring(0, c)));
		}

		br.close();
		assert (trajs_.size() > 0);
		
		loadNotes();

		//IJ.showMessage("Number of trajs read from disk:" + trajs_.size());
	}

	public void reloadTrajectories() {
		trajs_ = null;
		try {
			readTrajs();
		} catch (Exception e) {
			IJ.showMessage(e.toString() + "\n" + e.getMessage());
		}
	}
	
	public void rebuildTracks() {
		try {
			File file = new File(path_ + File.separator + "analysis" + File.separator + "trajs");
			posToTracks(file); 
		} catch (Exception e) {
			IJ.showMessage(e.toString() + "\n" + e.getMessage());
		}

		//reloadTrajectories();
	}
	
	public void loadNotes() {
		if (trajs_ == null) {
			return;
		}
		try {
			File file = new File(path_ + File.separator + "analysis" + File.separator + "notes");
			if (! file.exists()) {
				return;
			}
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while (null != (line = br.readLine())) {
				int c = line.indexOf(',');
				int cnt = Integer.parseInt(line.substring(0, c).trim());
				if (cnt < trajs_.size()) {
					trajs_.get(cnt).setNote(line.substring(c+1));
				}
			}

			br.close();
		} catch (IOException e) {
			IJ.showMessage(e.toString() + "\n" + e.getMessage());
		}		
	}
	
	public void saveNotes() {
		if (trajs_ == null) {
			return;
		}
		try {
			File file = new File(path_ + File.separator + "analysis" + File.separator + "notes"); 
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			for ( int i = 0; i < trajs_.size(); i ++ ) {
				String s = trajs_.get(i).getNote() ; 
				if (s != null && s.trim().length()>0) {
					bw.write("" + i + "," + s + "\n");
				}
			}
			bw.close();
		} catch (IOException e) {
			IJ.showMessage(e.toString() + "\n" + e.getMessage());
		}
		
	}
}
