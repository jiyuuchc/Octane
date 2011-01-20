//FILE:          OctanePlugin.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu 2/15/08
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

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import javax.swing.UIManager;

import edu.uchc.octane.OptionDlg;
import edu.uchc.octane.PeakFinderDialog;
import edu.uchc.octane.Prefs;
import edu.uchc.octane.TrajsBrowser;


public class OctanePlugin implements PlugIn, ImageListener {

	ImagePlus imp_;
	String cmd_;
	TrajsBrowser trajwin_;
	PeakFinderDialog finderDlg_;

	public OctanePlugin() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			Prefs.loadPrefs();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ImagePlus.addImageListener(this);
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		if (imp == imp_) {
			if (trajwin_ != null) {
				trajwin_.dispose();
				trajwin_ = null;
			}
			if (finderDlg_ != null) {
				finderDlg_.dispose();
				finderDlg_ = null;
			}
		}
	}

	@Override
	public void imageOpened(ImagePlus arg0) {}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp == imp_) {
			if (finderDlg_ != null) {
				finderDlg_.setImageProcessor(imp.getProcessor());
			}
		}
	}

	@Override
	public void run(String cmd) {
		if (cmd.equals("options")) {
			OptionDlg dlg = new OptionDlg();
			dlg.setVisible(true);
			return;
		}
		
		imp_ = WindowManager.getCurrentImage();
		if (imp_ == null || imp_.getStack().getSize() < 2) {
			IJ.showMessage("No open image stack");
			return;
		}
		if (cmd.equals("findpeaks")) {
			finderDlg_ = new PeakFinderDialog(imp_);
			finderDlg_.showDialog();			
		} else {
			trajwin_ = new TrajsBrowser(imp_);
			trajwin_.setVisible(true);
		}	
	}
}
