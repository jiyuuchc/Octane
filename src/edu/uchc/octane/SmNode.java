//FILE:          SmNode.java
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

import java.io.Serializable;

/**
 * A node contains information of a detected peak.
 */
public class SmNode implements Serializable {
	
	private static final long serialVersionUID = -8876493729593706510L;

	/** x position */
	public double x;
	
	/** y position */
	public double y;
	
	/** The frame number */
	public int frame;
	
	/** The peak value */
	public int height;
	
	/** residue of fitting. */
	public double reserved;

	/**
	 * Instantiates a new node.
	 *
	 * @param x the x
	 * @param y the y
	 * @param f the frame number
	 */
	public SmNode(double x, double y, int f) {
		this(x, y, f, 0, 0.0);
	}

	/**
	 * Instantiates a new node.
	 *
	 * @param x the x
	 * @param y the y
	 * @param f the frame number
	 * @param q the residue
	 */
	public SmNode(double x, double y, int f, int h,  double q) {
		this.x = x;
		this.y = y;
		frame = f;
		height = h;
		reserved = q;
	}

	/**
	 * Instantiates a new node.
	 *
	 * @param line Comma separated text data.
	 */
	public SmNode(String line) {
		String[] items = line.split(",");
		x = Double.parseDouble(items[0]);
		y = Double.parseDouble(items[1]);
		frame = Integer.parseInt(items[2].trim());
		if (items.length > 3) {
			height = Integer.parseInt(items[3]);
		}
		if (items.length > 4) {
			reserved = Double.parseDouble(items[4]);
		}
	}

	/** 
	 * Convert to comma separated text data.
	 */
	public String toString() {
		return (x + ", " + y + ", " + frame + ", " + height + "," + reserved);
	}
	
	/**
	 * Calculate square distance between two nodes
	 *
	 * @param n another node
	 * @return distance^2
	 */
	public double distance2(SmNode n) {
		return (x - n.x)*(x - n.x) + (y - n.y)*(y - n.y);
	}
	
	/**
	 * Calculate distance between two nodes.
	 *
	 * @param n another node
	 * @return the distance
	 */
	public double distance(SmNode n) {
		return Math.sqrt(distance2(n));
	}
}
