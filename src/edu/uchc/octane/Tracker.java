//FILE:          Tracker.java
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

package edu.uchc.octane;

import ij.IJ;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

// TODO: Auto-generated Javadoc
/**
 * The Class Tracker.
 */
@SuppressWarnings("unchecked")
public class Tracker {
	
	/** The dataset_. */
	TrajDataset dataset_;
	
	/** The cur frame_. */
	int curFrame_;
	
	/** The max blinking_. */
	int maxBlinking_;
	
	/** The tracks_. */
	LinkedList<LinkedList<Integer>> tracks_;
	
	/** The stopped tracks_. */
	LinkedList<LinkedList<Integer>> stoppedTracks_;
	
	/** The backward bonds_. */
	LinkedList<Integer> [] backwardBonds_;
	
	/** The forward bonds_. */
	LinkedList<Integer> [] forwardBonds_;
	
	/** The bond lengths_. */
	LinkedList<Double> [] bondLengths_;
	
	/** The distances_. */
	double distances_[][]; // numOfTracks x numOfNodesInNextFrame
	
	/** The threshold_. */
	double threshold_;
	
	/** The threshold2_. */
	double threshold2_;

	/** The xyt data_. */
	XytData xytData_;

	/**
	 * The Class XytData.
	 */
	public class XytData {
		
		//double [][][] data_;
		/** The data_. */
		SmNode [][] data_;
		
		/** The frame length_. */
		int [] frameLength_;
		
		/** The Constant MAX_PARTICLES_PER_FRAME. */
		static final int MAX_PARTICLES_PER_FRAME = 10000;
		
		/**
		 * Instantiates a new xyt data.
		 */
		public XytData() {
			// 
		}

		/**
		 * Builds the from nodes.
		 *
		 * @param nodes the nodes
		 */
		public void buildFromNodes(SmNode [] nodes) {
			int maxFrame = -1;
			for ( int i = 0; i < nodes.length; i ++) {
				if (nodes[i].frame > maxFrame) {
					maxFrame = nodes[i].frame;
				}
			}
			frameLength_ = new int[maxFrame];
			Arrays.fill(frameLength_, 0);
			for (int i = 0; i < nodes.length; i++) {
				SmNode s = nodes[i];
				if (s.reserved < Prefs.residueThreshold_) {
					frameLength_[s.frame - 1]++;
				}
			}

			//data_ = new double[frameLength_.size()][][];
			data_ = new SmNode[frameLength_.length][];
			for (int i = 0; i < frameLength_.length; i++ ) {
				//data_[i] = new double[frameLength_.get(i)][3];
				data_[i] = new SmNode[frameLength_[i]];
			}
			Arrays.fill(frameLength_, 0);

			for (int i = 0; i < nodes.length; i++) {
				SmNode s = nodes[i];
				if (s.reserved < Prefs.residueThreshold_) {
					int cnt = frameLength_[s.frame-1];
//					data_[s.frame][cnt][0] = s.x;
//					data_[s.frame][cnt][1] = s.y;
//					data_[s.frame][cnt][2] = s.reserved;
					data_[s.frame - 1][cnt] = s;
					frameLength_[s.frame - 1]++;
				}
			}
		}

//		public void buildFromTrajectories(Vector<Trajectory> trajs) {
//			frameLength_ = new LinkedList<Integer>();
//			
//			for (int i = 0; i < trajs.size(); i ++ ) {
//				Trajectory traj = trajs.get(i);
//				for ( int j = 0; j < traj.size(); j++) {
//					int frame = traj.get(j).frame - 1;
//					while (frame >= frameLength_.size()) {
//						frameLength_.add(0);
//					}
//					frameLength_.set(frame, frameLength_.get(frame) + 1);
//				}
//			}
//			
//			data_ = new double[frameLength_.size()][][];
//			for (int i = 0; i < frameLength_.size(); i++ ) {
//				data_[i] = new double[frameLength_.get(i)][3];
//				frameLength_.set(i,0);
//			}
//
//			for (int i = 0; i < trajs.size(); i++) {
//				Trajectory traj = trajs.get(i);
//				for ( int j = 0; j < traj.size(); j++) {
//					int frame = traj.get(j).frame - 1;
//					int cnt = frameLength_.get(frame);
//					data_[frame][cnt][0] = traj.get(j).x;
//					data_[frame][cnt][1] = traj.get(j).y;
//					data_[frame][cnt][2] = traj.get(j).reserved;
//					frameLength_.set(frame, frameLength_.get(frame) + 1);
//				}
//			}
//		}
		
//		public XytData(File file) {
//			try {
//				BufferedReader br = new BufferedReader(new FileReader(file));
//				String line;
//				frameLength_ = new LinkedList<Integer>();
//				//Vector<Vector<Double[]>> data = new Vector<Vector<Double[]>>();
//				
//				while (null != (line = br.readLine())) {
//					String[] items = line.split(",");
//					int frame = Integer.parseInt(items[2].trim()) - 1;
//					while (frame >= frameLength_.size()) {
//						frameLength_.add(0);
//					}
//					frameLength_.set(frame, frameLength_.get(frame) + 1);
//				}
//				br.close();
//
//				data_ = new double[frameLength_.size()][][];
//				for (int i = 0; i < frameLength_.size(); i++ ) {
//					data_[i] = new double[frameLength_.get(i)][3];
//					frameLength_.set(i,0);
//				}
//				
//				br = new BufferedReader(new FileReader(file));
//				while (null != (line = br.readLine())) {
//					String[] items = line.split(",");
//					int frame = Integer.parseInt(items[2].trim()) - 1;
//					int cnt = frameLength_.get(frame);
//					data_[frame][cnt][0] = Double.parseDouble(items[0]);
//					data_[frame][cnt][1] = Double.parseDouble(items[1]);
//					if (items.length > 3) {
//						data_[frame][cnt][2] = Double.parseDouble(items[3]);
//					}
//					frameLength_.set(frame, frameLength_.get(frame) + 1);					
//				}
//				br.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}

		/**
 * Gets the first of frame.
 *
 * @param frame the frame
 * @return the first of frame
 */
public int getFirstOfFrame(int frame) {
			return MAX_PARTICLES_PER_FRAME * frame;
		}

		/**
		 * Gets the length of frame.
		 *
		 * @param frame the frame
		 * @return the length of frame
		 */
		public int getLengthOfFrame(int frame) {
			return data_[frame].length;
		}

		/**
		 * Gets the distance2.
		 *
		 * @param idx1 the idx1
		 * @param idx2 the idx2
		 * @return the distance2
		 */
		public double getDistance2(int idx1, int idx2) {
			int f1 = idx2Frame(idx1);
			int f2 = idx2Frame(idx2);

			//if (f1 - f2 != 1 && f1 - f2 != -1) {
			//	return threshold2_;
			//}

//			double [] c1 = data_[f1][idx1%MAX_PARTICLES_PER_FRAME];
//			double [] c2 = data_[f2][idx2%MAX_PARTICLES_PER_FRAME];
			SmNode c1 = data_[f1][idx1%MAX_PARTICLES_PER_FRAME];
			SmNode c2 = data_[f2][idx2%MAX_PARTICLES_PER_FRAME];

//			return (c1[0]-c2[0])*(c1[0]-c2[0]) +(c1[1]-c2[1])*(c1[1]-c2[1]); 
			return (c1.x - c2.x)*(c1.x - c2.x) + (c1.y - c2.y)*(c1.y - c2.y); 
		}

		/**
		 * Gets the num frames.
		 *
		 * @return the num frames
		 */
		public int getNumFrames() {
			return data_.length;
		}

		/**
		 * Idx2 frame.
		 *
		 * @param idx the idx
		 * @return the int
		 */
		int idx2Frame(int idx) {
			return idx / MAX_PARTICLES_PER_FRAME;
		}

//		void writeToDisk(File file) throws IOException {
//			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			int cnt = 0;
//			for ( Iterator <LinkedList<Integer>> it = stoppedTracks_.iterator(); it.hasNext();) {
//				LinkedList<Integer> track = it.next();
//				for (Iterator<Integer> it2 = track.iterator(); it2.hasNext();) {
//					int idx = it2.next();
//					int frame = getFrameFromIdx(idx);
//					int n = idx - getFirstOfFrame(frame);
//					double [] c = data_[frame][n];
//					bw.write(String.format("%f, %f, %d, %f, %d\n", c[0], c[1], frame + 1, c[2], cnt));
//				}
//				cnt++;
//			}
//			
//			bw.close(); 
//		} // writeToDisk
		
		/**
 * Creates the trajs.
 *
 * @return the vector
 */
public Vector<Trajectory> createTrajs() {
			Vector<Trajectory> trajs = new Vector<Trajectory>() ;
			
			for (Iterator <LinkedList<Integer>> it = stoppedTracks_.iterator(); it.hasNext(); ) {
				Trajectory oneTraj = new Trajectory();
				trajs.add(oneTraj);
				LinkedList<Integer> track = it.next();
				for (Iterator <Integer> it2 = track.iterator(); it2.hasNext();) {
					int idx = it2.next();
					int frame = idx2Frame(idx);
					int n = idx - getFirstOfFrame(frame);
					//double [] c = data_[frame][n];
					SmNode c = data_[frame][n];
					//oneTraj.add(new SmNode(c[0], c[1], frame + 1, c[2]));
					oneTraj.add(c);
				}				
			}
			return trajs;
		}
	}

	/**
	 * Instantiates a new tracker.
	 *
	 * @param nodes the nodes
	 */
	public Tracker(SmNode [] nodes) {
		xytData_ = new XytData();
		xytData_.buildFromNodes(nodes);
	}

//	public Tracker(Vector<Trajectory> trajs, double max_search_r, int frame_skip_allowed) {
//		//data_ = nodes;
//		threshold_ = max_search_r;
//		threshold2_ = threshold_ * threshold_;
//		maxBlinking_ = frame_skip_allowed;
//
//		xytData_ = new XytData(trajs);
//	}
//
//	public Tracker(File f, double max_search_r, int frame_skip_allowed) {
//		//data_ = nodes;
//		threshold_ = max_search_r;
//		threshold2_ = threshold_ * threshold_;
//		maxBlinking_ = frame_skip_allowed;
//
//		xytData_ = new XytData(f);
//	}

	/**
 * Trivial bonds.
 */
void trivialBonds() {
		int firstPos = xytData_.getFirstOfFrame(curFrame_);
		int nPos = xytData_.getLengthOfFrame(curFrame_);
		
		forwardBonds_ = new LinkedList[tracks_.size()];
		bondLengths_ = new LinkedList[tracks_.size()];
		backwardBonds_ = new LinkedList[nPos];
		for ( int i = 0; i < tracks_.size(); i ++) {
			forwardBonds_[i] = new LinkedList<Integer>();
			bondLengths_[i] = new LinkedList<Double>();
		}
		for ( int i = 0; i < nPos; i ++) {
			backwardBonds_[i] = new LinkedList<Integer>();
		}

		// calculated all possible bonds
		for (ListIterator <LinkedList<Integer>> it = tracks_.listIterator(); it.hasNext(); ) {
			int id = it.nextIndex();
			int trackHead = it.next().getLast();
			if (trackHead >= 0) { // only for active tracks
				for (int j = 0; j < nPos; j ++) {
					double d = xytData_.getDistance2(trackHead, firstPos + j);
					if (d <= threshold2_) { // don't miss the = sign
						forwardBonds_[id].add(j);
						bondLengths_[id].add(d);
						backwardBonds_[j].add(id);
					} 
				}
			}
		}

		// search all trivial bonds
		for (int i = 0; i < forwardBonds_.length; i++ ) {
			if (forwardBonds_[i].size() == 1) {
				int bondTo = forwardBonds_[i].getFirst();
				if (backwardBonds_[bondTo].size() == 1) {
					// trivial bond
					forwardBonds_[i].clear();
					tracks_.get(i).add(bondTo + firstPos);
				}
			} else {
				for (int j = 0; j < forwardBonds_[i].size(); j++) {
					bondLengths_[i].set(j, Math.sqrt(bondLengths_[i].get(j)));
				}
			}
		}

	} // TrivialBonds()

	/**
	 * Cluster and optimize.
	 *
	 * @param seed the seed
	 */
	void clusterAndOptimize(int seed) {
		LinkedList<Integer> headList = new LinkedList<Integer>();
		LinkedList<Integer> tailList = new LinkedList<Integer>();

		headList.add(seed);
		ListIterator<Integer> itHead = headList.listIterator();
		ListIterator<Integer> itTail = tailList.listIterator();
		itHead.next();

		while ( itHead.hasPrevious()) {
			while (itHead.hasPrevious()) {
				int trackIdx = itHead.previous();
				for (int j = 0; j < forwardBonds_[trackIdx].size(); j ++) {
					int tail = forwardBonds_[trackIdx].get(j);
					if ( ! tailList.contains(tail)) {
						itTail.add(tail); // surprising, this add _before_ the cursor.
					}
				}
			}
			//lastHeadListEnd = headList.size();	
			
			while (itTail.hasPrevious()) {
				int posIdx = itTail.previous();
				for (int j = 0; j < backwardBonds_[posIdx].size(); j++) {
					int head = backwardBonds_[posIdx].get(j);
					if (! headList.contains(head)) {
						itHead.add(head);
					}
				}
			}
			//lastTailListEnd = tailList.size();
		}
		
		optimizeSubnetwork(headList, tailList);
	}
	
	/**
	 * Optimize subnetwork.
	 *
	 * @param headList the head list
	 * @param tailList the tail list
	 */
	void optimizeSubnetwork(LinkedList<Integer> headList, LinkedList<Integer> tailList) {
		double bestDistanceSum = 1e20;
		int curBondIdx = -1;
		double curDistanceSum = 0;
		Stack<Integer> stack = new Stack<Integer>();
		Stack<Integer> occupiedTails = new Stack<Integer>();
		Stack<Double> distanceStack = new Stack<Double>();
		Stack<Integer> stack_c = null;
		int trackIdx; 
		int tail;
		double nextBondLength;

		while (true) {
			trackIdx = headList.get(stack.size());

			// try next possible bond		
			if (curBondIdx  < forwardBonds_[trackIdx].size()) { 
				curBondIdx ++ ;

				// test if this is a good bond
				if (curBondIdx == forwardBonds_[trackIdx].size()) { //special case, no bonding
					if (curDistanceSum + threshold_ >= bestDistanceSum) {
						continue;
					}
					//curDistanceSum += threshold_;
					nextBondLength = threshold_;
					tail = -1;
				} else {
					tail = forwardBonds_[trackIdx].get(curBondIdx);
					if (occupiedTails.contains(tail)) {
						continue; //fail
					}

					if (curDistanceSum + bondLengths_[trackIdx].get(curBondIdx) >= bestDistanceSum) {
						continue; //fail
					}
					//curDistanceSum += bondLengths_.get(trackIdx).get(curBondIdx);
					nextBondLength = bondLengths_[trackIdx].get(curBondIdx);
				}

				//looks ok, push to stack
				if (stack.size() < headList.size()-1) {
					stack.push(curBondIdx);
					occupiedTails.push(tail);
					distanceStack.push(curDistanceSum);
					curBondIdx = -1;
					curDistanceSum += nextBondLength;
				} else { // unless this is the last element
					bestDistanceSum = curDistanceSum + nextBondLength;
					stack.push(curBondIdx);
					stack_c = (Stack<Integer>) stack.clone();
					stack.pop();
				}

			} else if (stack.size() > 0) {
				curBondIdx = stack.pop();
				occupiedTails.pop();
				curDistanceSum = distanceStack.pop();
			} else { // finished here
				break;
			}

		} //while

		// got best route. creating new bonds
		for (int i = 0; i < stack_c.size(); i ++) {
			int firstPos = xytData_.getFirstOfFrame(curFrame_);			
			trackIdx = headList.get(i);
			int bondIdx = stack_c.get(i);
			if (bondIdx < forwardBonds_[trackIdx].size()) {
				int bondTo = forwardBonds_[trackIdx].get(bondIdx);
				tracks_.get(trackIdx).add(bondTo + firstPos); //might be slow if tracks_ is too big
			} 
			forwardBonds_[trackIdx].clear();
		}
	
	}
	
	/**
	 * Do tracking.
	 */
	public void doTracking() {
		threshold_ = Prefs.trackerMaxDsp_;
		threshold2_ = threshold_ * threshold_;
		maxBlinking_ = Prefs.trackerMaxBlinking_;
		
		tracks_ = new LinkedList<LinkedList<Integer>>();
		stoppedTracks_ = new LinkedList<LinkedList<Integer>>();
		//initial track # = first frame particle #  
		for (int i = 0; i < xytData_.getLengthOfFrame(0); i ++ ) {
			tracks_.add(new LinkedList<Integer>());
			tracks_.getLast().add(i);
		}
		
		curFrame_ = 1;
		while (curFrame_ < xytData_.getNumFrames()) {
			trivialBonds();

			for ( int i = 0; i < forwardBonds_.length; i ++) {
				if (forwardBonds_[i].size() > 0) {
					clusterAndOptimize(i);
				}
			}
			
			boolean [] isTrackedParticle = new boolean[xytData_.getLengthOfFrame(curFrame_)];

			//remove all tracks that has been lost for too long
			int firstPos = xytData_.getFirstOfFrame(curFrame_);
			for ( Iterator<LinkedList<Integer>> it = tracks_.iterator(); it.hasNext(); ) {
				LinkedList<Integer> track = it.next();
				if (track.getLast() > 0) {
					int frame = xytData_.idx2Frame((track.getLast()));
					if (curFrame_ - frame > maxBlinking_) {
						it.remove();
						stoppedTracks_.add(track);
					} else if (curFrame_ == frame) {
						isTrackedParticle[track.getLast() - firstPos] = true;
					}
				}
			}

			//add new particles into the track list
			for (int i = 0; i < xytData_.getLengthOfFrame(curFrame_); i++) {
				if (!isTrackedParticle[i]) {
					tracks_.add(new LinkedList<Integer>());
					tracks_.getLast().add(i + firstPos);
				}
			}

			curFrame_ ++;
			if ((curFrame_ / 100) * 100 == curFrame_ ) {
				IJ.log("Frame " + curFrame_ + ", " 
						+ tracks_.size() + " active tracks, " 
						+ stoppedTracks_.size() + " stopped tracks.");
			}
		} //while

		// add all tracks to stoppedTracks list
		for (Iterator<LinkedList<Integer>> it = tracks_.iterator(); it.hasNext();) {
			LinkedList<Integer> track = it.next();
			stoppedTracks_.add(track);
		}
		tracks_.clear();

	} //doTracking
	
	/**
	 * Gets the tracks.
	 *
	 * @return the tracks
	 */
	public Vector<Trajectory> getTracks() {
		return xytData_.createTrajs();
	}
}
