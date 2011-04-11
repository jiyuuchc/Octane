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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

public class TrajDataset implements Serializable{
	private static final long serialVersionUID = 1L;

	private Vector<Trajectory> trajs_;
	private String path_;
	
	public TrajDataset() {
		trajs_ = new Vector<Trajectory>();
	}
	
	public void buildDataset(String path) throws IOException {
		path_ = path;
		readTrajs();
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

	
	public void posToTracks(File outfile) throws IOException{
		File file = new File(path_ + File.separator + "analysis" + File.separator + "positions");
		
		Tracker tracker = new Tracker(file, Prefs.trackerMaxDsp_, Prefs.trackerMaxBlinking_);
		tracker.doTracking();
		//tracker.toDisk(outfile);
		trajs_ = tracker.getTrajs();
		writeToText(outfile);
	}

	void readTrajs() throws IOException {
		File file = new File(path_ + File.separator + "analysis" + File.separator + "trajs");

		if (! file.exists()) {
			IJ.log("No Trajectory available. Build the trajectories.");
			posToTracks(file);
			return;
		} else {
			readFromText(file);
		}

	}
	
	public void writeToText(File file)throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (int i = 0; i < trajs_.size(); i ++ ) {
			for (int j = 0; j < trajs_.get(i).size(); j++) {
				SmNode s = trajs_.get(i).get(j);
				bw.write(String.format("%f, %f, %d, %f, %d\n", s.x, s.y, s.frame, s.reserved, i));				
			}
		}
		bw.close(); 		
	}
	
	public void readFromText(File file) throws IOException {
		Trajectory oneTraj = new Trajectory();
		int cur_cnt = -1;
		String line;
		BufferedReader br = new BufferedReader(new FileReader(file));

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
	}

	public void saveDataset() {
		ObjectOutputStream out;
		BufferedOutputStream fs;
		try {
			fs = new BufferedOutputStream(new FileOutputStream(path_ + File.separator + "analysis" + File.separator + "dataset"));
			out = new ObjectOutputStream(fs);
			out.writeObject(this);
			out.close();
			fs.close();
		} catch (IOException e) {
			IJ.showMessage(e.toString() + "\n" + e.getMessage());
			return;
		} catch (Exception e) {
			e.printStackTrace(); 
			System.exit(1); 
		}
	}
}
