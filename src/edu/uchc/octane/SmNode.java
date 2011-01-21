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

public class SmNode implements Serializable {
	private static final long serialVersionUID = 4784457159470706657L;
	double x;
	double y;
	int frame;
	double quality;

	public SmNode(double x, double y, int f) {
		this(x, y, f, 0.0);
	}

	public SmNode(double x, double y, int f, double q) {
		this.x = x;
		this.y = y;
		frame = f;
		quality = q;
	}

	public SmNode(String line) {
		String[] items = line.split(",");
		x = Double.parseDouble(items[0]);
		y = Double.parseDouble(items[1]);
		frame = Integer.parseInt(items[2].trim());
		if (items.length > 3) {
			quality = Double.parseDouble(items[3]);
		}
	}

	public String toString() {
		return (x + ", " + y + ", " + frame + ", " + quality);
	}
}
