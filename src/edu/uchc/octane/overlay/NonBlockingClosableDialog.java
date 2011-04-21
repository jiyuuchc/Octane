//FILE:          NonBlockingClosableDialog.java
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

package edu.uchc.octane.overlay;

import java.awt.event.WindowEvent;

import ij.IJ;
import ij.gui.NonBlockingGenericDialog;

public class NonBlockingClosableDialog extends NonBlockingGenericDialog {
	
	public NonBlockingClosableDialog(String title) {
		super(title);
	}

	@Override
	public synchronized void windowClosing(WindowEvent e) {
		super.windowClosing(e);
		notify();
	}
}
