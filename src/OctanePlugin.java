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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.HashMap;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.io.FileInfo;
import ij.plugin.PlugIn;

import javax.swing.UIManager;

import edu.uchc.octane.Browser;
import edu.uchc.octane.OptionDlg;
import edu.uchc.octane.PeakFinderDialog;
import edu.uchc.octane.Prefs;


public class OctanePlugin implements PlugIn, ImageListener, WindowListener {

	String cmd_;

	class D {
		public PeakFinderDialog finderDlg;
		public Browser browser;
	}

	HashMap<ImagePlus, D> dict_;
	
	public OctanePlugin() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			Prefs.loadPrefs();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ImagePlus.addImageListener(this);
		dict_ = new HashMap<ImagePlus,D>();
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		D d = dict_.get(imp);		
		if (d != null) {
			if (d.browser != null) {
				d.browser.dispose();
			}
			if (d.finderDlg != null) {
				d.finderDlg.dispose();
			}
			dict_.remove(imp);
		}
	}

	@Override
	public void imageOpened(ImagePlus arg0) {}

	@Override
	public void imageUpdated(ImagePlus imp) {
		D d = dict_.get(imp);
		if (d != null) {
			if (d.finderDlg != null) {
				d.finderDlg.setImageProcessor(imp.getProcessor());
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
		
		ImagePlus imp = WindowManager.getCurrentImage();
		String path;
		if (imp == null || imp.getStack().getSize() < 2) {
			IJ.showMessage("No open image stack");
			return;
		}
		FileInfo fi = imp.getOriginalFileInfo();
		if (fi != null) {
			path = fi.directory; 
		} else {
			IJ.showMessage("Can't find image's disk location. You must save the data under a unique folder.");
			return;
		}
		
		D d = new D();
		d.browser = null;
		d.finderDlg = null;
		dict_.put(imp, d);
		imp.getWindow().addWindowListener(this);
		File file = new File(path + File.separator + "analysis" + File.separator + "dataset");
		if (! file.exists()) {
			d.finderDlg = new PeakFinderDialog(imp);
			d.finderDlg.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					PeakFinderDialog dlg = (PeakFinderDialog) e.getSource();
					dlg.removeWindowListener(this);
					if (dlg.wasOKed()) {
						D d = dict_.get(dlg.getImp());
						d.finderDlg = null;
						d.browser = new Browser(dlg.getImp());
						d.browser.setVisible(true);
					}
					super.windowClosed(e);
				}							
			});
			d.finderDlg.showDialog();
		} else {
			d.browser = new Browser(imp);
			d.browser.setVisible(true);
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowClosed(WindowEvent e) {
		ImageWindow iw = (ImageWindow) e.getSource();
		ImagePlus imp = iw.getImagePlus();
		imageClosed(imp);		
	}

	@Override
	public void windowClosing(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {
		ImageWindow iw = (ImageWindow) e.getSource();
		ImagePlus imp = iw.getImagePlus();
		
		D d=dict_.get(imp);
		if (d!= null) {
			if (d.browser != null) {
				d.browser.setVisible(true);
			}
			if (d.finderDlg != null) {
				d.finderDlg.setVisible(true);
			}
		}		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		ImageWindow iw = (ImageWindow) e.getSource();
		ImagePlus imp = iw.getImagePlus();
		
		D d=dict_.get(imp);
		if (d!= null) {
			if (d.browser != null) {
				d.browser.setVisible(false);
			}
			if (d.finderDlg != null) {
				d.finderDlg.setVisible(false);
			}
		}		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {}
}
