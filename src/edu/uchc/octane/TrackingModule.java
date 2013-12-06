//FILE:          TrackingModule.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 2/16/13
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

/**
 * Tracking module to generate trajectories
 * @author Ji-Yu
 *
 */
public class TrackingModule {
	TrajDataset dataset_;
	protected Vector<Trajectory> trajectories_;
	protected SmNode [][] nodes_; //nodes_[frame][#]

	private Integer [][] backwardBonds_;
	private Bond [][] forwardBonds_;
	private boolean [] isTrackedParticle_; 
	private LinkedList<Trajectory> activeTracks_;	
	private Trajectory wasted_;
	private double threshold_;
	private double threshold2_;
	private int maxBlinking_;
	private int curFrame_;

	class Bond implements Comparable<Bond> {
		int bondTo;
		double bondLength;
		@Override
		public int compareTo(Bond o) {
			return (int) Math.signum(bondLength - o.bondLength); 
		}
	}

	/**
	 * Constructor
	 * @param dataset The dataset
	 */
	public TrackingModule(TrajDataset dataset) {
		dataset_ = dataset;
	}

	private void clusterAndOptimize(int seed) {
		Vector<Integer> headList = new Vector<Integer>();
		Vector<Integer> tailList = new Vector<Integer>();

		headList.add(seed);
		ListIterator<Integer> itHead = headList.listIterator();
		ListIterator<Integer> itTail = tailList.listIterator();
		itHead.next();

		while ( itHead.hasPrevious()) {
			while (itHead.hasPrevious()) {
				int trackIdx = itHead.previous();
				for (int j = 0; j < forwardBonds_[trackIdx].length; j ++) {
					int tail = forwardBonds_[trackIdx][j].bondTo;
					if ( !isTrackedParticle_[tail] && ! tailList.contains(tail)) {
						itTail.add(tail); // surprising, this add _before_ the cursor.
					}
				}
			}
			//lastHeadListEnd = headList.size();	

			while (itTail.hasPrevious()) {
				int posIdx = itTail.previous();
				for (int j = 0; j < backwardBonds_[posIdx].length; j++) {
					int head = backwardBonds_[posIdx][j];
					if (forwardBonds_[head] != null && ! headList.contains(head)) {
						itHead.add(head);
					}
				}
			}
			//lastTailListEnd = tailList.size();
		}

		optimizeSubnetwork(headList, tailList);
	}

	private void optimizeSubnetwork(Vector<Integer> headList, Vector<Integer> tailList) {
		double bestDistanceSum = Double.MAX_VALUE;
		int curBondIdx = -1;
		double curDistanceSum = 0;
		Stack<Integer> stack = new Stack<Integer>();
//		Stack<Integer> occupiedTails = new Stack<Integer>();
		HashSet<Integer> occupiedTails = new HashSet<Integer>(headList.size());
		Stack<HashSet<Integer>> tailStack = new Stack<HashSet<Integer>>();
		Stack<Double> distanceStack = new Stack<Double>();
		Stack<Integer> stack_c = null;
		int trackIdx; 
		int tail;
		double nextBondLength;
		double [] minExtraLength = new double[headList.size()];

		if (headList.size() + tailList.size() > 400) {
			IJ.log("Optimizing a very large network: " + headList.size() + "," + tailList.size() + ". This might take for ever.");
		}
		
		double m = 0;
		for (int i = headList.size()-1 ; i >=0; i--) {
			minExtraLength[i] = m;
			m += forwardBonds_[headList.get(i)][0].bondLength;
		}

		while (true) {
			trackIdx = headList.get(stack.size());

			// try next possible bond		
			while (curBondIdx  < forwardBonds_[trackIdx].length) { 
				curBondIdx ++ ;

				// test if this is a good bond
				if (curBondIdx == forwardBonds_[trackIdx].length) { //special case, no bonding
					if (curDistanceSum + threshold_ >= bestDistanceSum) {
						break;
					}
					//curDistanceSum += threshold_;
					nextBondLength = threshold_;
					tail = -1;
				} else {
					tail = forwardBonds_[trackIdx][curBondIdx].bondTo;
					if (occupiedTails.contains(tail)) {
						continue; //next bond
					}

					nextBondLength = forwardBonds_[trackIdx][curBondIdx].bondLength;

					if (curDistanceSum + nextBondLength + minExtraLength[stack.size()] >= bestDistanceSum) {
						break; //fail
					}
					//curDistanceSum += bondLengths_.get(trackIdx).get(curBondIdx);
				}

				//looks ok, push to stack
				if (stack.size() < headList.size()-1) {
					stack.push(curBondIdx);
					tailStack.push(new HashSet<Integer>(occupiedTails));
					occupiedTails.add(tail);
					distanceStack.push(curDistanceSum);
					curBondIdx = -1;
					curDistanceSum += nextBondLength;
					trackIdx = headList.get(stack.size());
				} else { // unless this is the last element
					bestDistanceSum = curDistanceSum + nextBondLength;
					stack.push(curBondIdx);
					stack_c = (Stack<Integer>) stack.clone();
					stack.pop();
					break;
				}
			}

			if (stack.size() > 0) {
				curBondIdx = stack.pop();
				occupiedTails = tailStack.pop();
				curDistanceSum = distanceStack.pop();
			} else { // finished here
				break;
			}

		} //while

		// got best route. creating new bonds
		for (int i = 0; i < stack_c.size(); i ++) {			
			trackIdx = headList.get(i);
			int bondIdx = stack_c.get(i);
			if (bondIdx < forwardBonds_[trackIdx].length) {
				int bondTo = forwardBonds_[trackIdx][bondIdx].bondTo;
				SmNode n = nodes_[curFrame_][bondTo];
				activeTracks_.get(trackIdx).add(n); //might be slow if tracks_ is too big
				assert(isTrackedParticle_[bondTo] == false);
				isTrackedParticle_[bondTo] = true;
				backwardBonds_[bondTo] = null;
			}
			forwardBonds_[trackIdx] = null;
		}
	}


	private void buildAllPossibleBonds() {
		forwardBonds_ = new Bond[activeTracks_.size()][];
		//bondLengths_ = new double[activeTracks_.size()][];
		backwardBonds_ = new Integer[nodes_[curFrame_].length][];
		isTrackedParticle_ = new boolean[nodes_[curFrame_].length];
		
		Vector<Integer> [] backBonds = new Vector[nodes_[curFrame_].length];

//		for ( int i = 0; i < activeTracks_.size(); i ++) {
//			forwardBonds_[i] = new LinkedList<Integer>();
//			bondLengths_[i] = new LinkedList<Double>();
//		}
		for ( int i = 0; i < nodes_[curFrame_].length; i ++) {
			backBonds[i] = new Vector<Integer>();
		}

		Bond [] bonds = new Bond[nodes_[curFrame_].length];
		//double [] bondLengths = new double[nodes_[curFrame_].length];
		int nBonds = 0;

		// calculated all possible bonds
		ListIterator <Trajectory> it = activeTracks_.listIterator();
		while( it.hasNext() ) {
			int id = it.nextIndex();
			SmNode trackHead = it.next().lastElement();
			
			nBonds = 0;

			for (int j = 0; j < nodes_[curFrame_].length; j ++) {
				if (nodes_[curFrame_][j].residue > TrackingParameters.errorThreshold_) {
					double d = trackHead.distance2(nodes_[curFrame_][j]);
					if (d <= threshold2_) { // don't miss the = sign
						Bond b = new Bond();
						b.bondLength = d;
						b.bondTo = j;
						bonds[nBonds++] = b;
						backBonds[j].add(id);
//						forwardBonds_[id].add(j);
//						bondLengths_[id].add(d);
//						backwardBonds_[j].add(id);
					} 
				}
			}
			
			forwardBonds_[id] = Arrays.copyOf(bonds, nBonds);
			if (nBonds > 1) {
				Arrays.sort(forwardBonds_[id]);
				
				// we won't do network search if the shortest link is small enough
				if (forwardBonds_[id][0].bondLength <= TrackingParameters.trackerLowerBound_) {
					forwardBonds_[id] = Arrays.copyOf(forwardBonds_[id], 1);
				}
			}
//			bondLengths_[id] = Arrays.copyOf(bondLengths, nBonds);
			
		}
		
		// create backward bons
		for (int i = 0; i < backBonds.length; i ++) {
			backwardBonds_[i] = new Integer[backBonds[i].size()];
			backBonds[i].toArray(backwardBonds_[i]);
		}

	}

	private void trivialBonds() {
		// search all trivial bonds
		for (int i = 0; i < forwardBonds_.length; i++ ) {
			if (forwardBonds_[i].length == 1) {
				int bondTo = forwardBonds_[i][0].bondTo;
				if (backwardBonds_[bondTo].length == 1) {
					// trivial bond
					forwardBonds_[i] = null;
					backwardBonds_[bondTo] = null;
					SmNode n = nodes_[curFrame_][bondTo];
					activeTracks_.get(i).add(n);
					isTrackedParticle_[bondTo] = true;
					continue;
				}
			} 

			for (int j = 0; j < forwardBonds_[i].length; j++) {
				forwardBonds_[i][j].bondLength = Math.sqrt(forwardBonds_[i][j].bondLength);
//				bondLengths_[i][j]=Math.sqrt(bondLengths_[i][j]);
			}
		
		}
	} // TrivialBonds()


	/**
	 * Create trajectories.
	 *
	 * @return the trajectories
	 */
	public Vector<Trajectory> doTracking() {
		threshold_ = TrackingParameters.trackerMaxDsp_;
		threshold2_ = threshold_ * threshold_;
		maxBlinking_ = TrackingParameters.trackerMaxBlinking_;
		nodes_ = dataset_.nodes_;

		activeTracks_ = new LinkedList<Trajectory>();
		trajectories_ = new Vector<Trajectory>();
		wasted_ = new Trajectory();

		//initial track # = first frame particle #  
		for (int i = 0; i < nodes_[0].length; i ++ ) {
			Trajectory t;
			t = new Trajectory();
			if ( nodes_[0][i].residue > TrackingParameters.errorThreshold_) {
				t.add(nodes_[0][i]);
				activeTracks_.add(t);
			} else {
				wasted_.add(nodes_[0][i]);
			}
		}

		curFrame_ = 1;
		while (curFrame_ < nodes_.length) {
			
			if (curFrame_ % 50 == 0 ) {
				IJ.log("Frame " + curFrame_ + ", "
						+ activeTracks_.size() + " active tracks, " 
						+ trajectories_.size() + " stopped tracks."
						+ nodes_[curFrame_].length + "new nodes");
			}
			IJ.showProgress(curFrame_, nodes_.length);
			buildAllPossibleBonds();
			trivialBonds();

			for ( int i = 0; i < forwardBonds_.length; i ++) {
				if (forwardBonds_[i] != null && forwardBonds_[i].length > 0) {
					clusterAndOptimize(i);
				}
			}

			//remove all tracks that has been lost for too long
			//int firstPos = xytData_.getFirstOfFrame(curFrame_);
			Iterator<Trajectory> it = activeTracks_.iterator(); 
			while ( it.hasNext() ) {
				Trajectory track = it.next();
				int frame = track.lastElement().frame;
				if (curFrame_ - frame >= maxBlinking_) {
					it.remove();
					trajectories_.add(track);
				} 
			}

			//add new particles into the track list
			for (int i = 0; i < nodes_[curFrame_].length; i++) {
				if (! isTrackedParticle_[i]) {
					assert(backwardBonds_[i] != null);
					if (nodes_[curFrame_][i].residue > TrackingParameters.errorThreshold_) {
						Trajectory t;
						t = new Trajectory();
						t.add(nodes_[curFrame_][i]);
						activeTracks_.add(t);
					} else {
						wasted_.add(nodes_[curFrame_][i]);
					}
				}
			}

			curFrame_ ++;
		} //while

		// add all tracks to stoppedTracks list
		Iterator<Trajectory> it = activeTracks_.iterator(); 
		while (it.hasNext()) {
			Trajectory track = it.next();
			trajectories_.add(track);
		}
		if (wasted_ != null && wasted_.size() > 0) {
			wasted_.deleted = true;
			trajectories_.add(wasted_);
		}
		activeTracks_.clear();
		activeTracks_ = null;
		nodes_ = null;
		wasted_ = null;
		
		return trajectories_;

	} //doTracking
}
