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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

public class TrajDataset implements Serializable{
	
	private static final long serialVersionUID = -4434499638684956916L;
	
	private Vector<Trajectory> trajs_;

	public TrajDataset() {
		trajs_ = new Vector<Trajectory>();
	}

//	Vector<Trajectory> getTrajectories() {
//		if (trajs_ == null || trajs_.size() == 0) {
//			return null;
//		} else 
//			return trajs_;
//	}

	public Trajectory getTrjectoryByIndex(int i) {
		return trajs_.get(i);
	}

	public int getSize() {
		return trajs_.size();
	}

//	public void setTrajectories(Vector<Trajectory> trajs) {
//		trajs_ = trajs;
//	}
	
//	Vector<SmNode> getNodes() {
//		if (nodes_ == null || nodes_.size() == 0 )
//			return null;
//		else
//			return nodes_;
//	}
	
//	public void setNodes(Vector<SmNode> nodes) {
//		nodes_ = nodes;
//	}

//	public void writePositionsToText(File file) throws IOException {
//		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//		for (int i = 0; i < nodes_.size(); i ++ ) {
//				SmNode s = nodes_.get(i);
//				bw.write(String.format("%f, %f, %d, %f\n", s.x, s.y, s.frame, s.reserved));				
//		}
//		bw.close();		
//	}

	public void writeTrajectoriesToText(File file)throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (int i = 0; i < trajs_.size(); i ++ ) {
			for (int j = 0; j < trajs_.get(i).size(); j++) {
				SmNode s = trajs_.get(i).get(j);
				bw.write(String.format("%f, %f, %d, %f, %d\n", s.x, s.y, s.frame, s.reserved, i));				
			}
		}
		bw.close(); 		
	}

	public void saveDataset(File file) throws IOException {
		ObjectOutputStream out;
		BufferedOutputStream fs;
		fs = new BufferedOutputStream(new FileOutputStream(file));
		out = new ObjectOutputStream(fs);
		out.writeObject(this);
		out.close();
		fs.close();
	}

	static public TrajDataset loadDataset(File file) throws IOException, ClassNotFoundException {
		ObjectInputStream in;
		FileInputStream fs;
		TrajDataset dataset = null;
		fs = new FileInputStream(file);
		in = new ObjectInputStream(fs);
		dataset = (TrajDataset) in.readObject();
		in.close();
		fs.close();
		return dataset;
	}

	static public TrajDataset importDatasetFromPositionsText(File file) throws IOException {
		BufferedReader br;
		String line;
		ArrayList<SmNode> nodes = new ArrayList<SmNode>(); 
		br = new BufferedReader(new FileReader(file));
		while (null != (line = br.readLine())) {
			SmNode node = new SmNode(line);
			nodes.add(node);
		}
		br.close();
		
		return createDatasetFromNodes((SmNode []) nodes.toArray());
	}
	
	static public TrajDataset importDatasetFromTrajectoriesText(File file) throws IOException {
		
		TrajDataset dataset;
		
		Trajectory oneTraj = new Trajectory();
		int cur_cnt = -1;
		String line;
		BufferedReader br = new BufferedReader(new FileReader(file));

		dataset = new TrajDataset();

		while (null != (line = br.readLine())) {
			int c = line.lastIndexOf(',');
			int cnt = Integer.parseInt(line.substring(c + 1).trim());
			if (cur_cnt < cnt) {
				oneTraj = new Trajectory();
				dataset.trajs_.add(oneTraj);
				cur_cnt = cnt;
			}
			SmNode node = new SmNode(line.substring(0, c)); 
			oneTraj.add(node);
			//dataset.nodes_.add(node);
		}

		br.close();
		assert (dataset.trajs_.size() > 0);	
		
		return dataset;
	}
	
	static public TrajDataset createDatasetFromNodes(SmNode [] nodes) {
		TrajDataset dataset;
		
		dataset = new TrajDataset();
		Tracker tracker = new Tracker(nodes);
		tracker.doTracking();
		dataset.trajs_ = tracker.getTracks();
		return dataset;		
	}
}
