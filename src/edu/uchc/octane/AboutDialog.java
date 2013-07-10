//FILE:          AboutDialog.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 5/21/13
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

import javax.swing.JOptionPane;

public class AboutDialog {

	public static final String aboutMsg_ = 
			GlobalPrefs.PACKAGE_NAME + " v" + GlobalPrefs.VERSIONSTR + "\n" +
			"\n" +
			"Written by Ji Yu, jyu@uchc.edu\n" +
			"Copyright (c) 2009-2013 by Ji Yu\n" +
			"\n" +
			"Octane is free software, which is licensed to you under the\n" +
			"BSD License. Please see the file LICENSE for more details. \n" +
			"\n" +
			"This software is provided AS-IS, with ABSOLUTELY NO WARRANTY.\n" +
			"\n";

	static public void showDialog() {
		JOptionPane.showMessageDialog(null, aboutMsg_, "About Octane", JOptionPane.PLAIN_MESSAGE);
	}
}
