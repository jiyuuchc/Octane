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

package edu.uchc.octane;

import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.plugin.PlugIn;

/**
 * The PlugIn adaptor.
 *
 */
public class OctanePlugin implements PlugIn{

	ImagePlus imp_;
	OctaneWindowControl ctl_;

	protected static HashMap<ImagePlus, OctanePlugin> dict_ = new HashMap<ImagePlus,OctanePlugin>();

	/**
	 * Constructor
	 */
	public OctanePlugin() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	void openWindow(TrajDataset dataset) {
		ctl_ = new OctaneWindowControl(imp_);
		
		ctl_.setup(dataset);
		dict_.put(imp_, this); // keep the reference to the plugin alive

		imp_.getWindow().addWindowListener(new WindowAdapter() {

			boolean wasVisible;

			@Override
			public void windowIconified(WindowEvent e) {
				wasVisible = ctl_.getWindow().isVisible();
				ctl_.getWindow().setVisible(false);
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
				ctl_.getWindow().setVisible(wasVisible);
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
				ctl_.getWindow().dispose();
				dict_.remove(imp_);
			}
		});
	}

	void startImageAnalysis() {
		DeflationAnalysisDialog dlg = new DeflationAnalysisDialog(imp_);

		dlg.showDialog();

		if (dlg.wasOKed()) {
			SmNode [][] nodes = dlg.processAllFrames();
			
			if ( TrackingParameters.openDialog() ) { //wasOKed ?

				TrajDataset data = TrajDataset.createDatasetFromNodes(nodes);
				openWindow(data);
				
				return;
			}
		}
		
		// if the action is cancelled
		dict_.remove(imp_);
	}

	TrajDataset readDataset(File f) throws IOException, ClassNotFoundException {
		TrajDataset dataset;
		IJ.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		IJ.showStatus("Loading data ...");
		dataset = TrajDataset.loadDataset(f);
		IJ.showStatus("");
		IJ.getInstance().setCursor(Cursor.getDefaultCursor());
		return dataset;
	}

	/* (non-Javadoc)
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String cmd) {
		String path;

		if (!IJ.isJava16()) {
			IJ.error("Octane requires Java version 1.6 or higher. Please upgrade the JVM.");
			return;
		}

		imp_ = WindowManager.getCurrentImage();		

		if (imp_ == null || imp_.getStack().getSize() < 2) {
			IJ.error("This only works on an opened image stack.");
			return;
		}

		FileInfo fi = imp_.getOriginalFileInfo();
		if (fi != null) {
			path = fi.directory; 
		} else {
			IJ.error("Can't find image's disk location. You must save the data on disk first.");
			return;
		}

		if (dict_.containsKey(imp_)) { // window already open
			OctanePlugin plugin = dict_.get(imp_);
			if (plugin != null) {
				plugin.ctl_.getWindow().setVisible(true);
			} else {
				// do nothing
			}
			return;
		}
			
		
		if (cmd.equals("browser")) {
			dict_.put(imp_, null);
			startImageAnalysis();
			return;
		}


		if (cmd.equals("load")){					
			assert(path != null); 
			File file = new File(path + File.separator + imp_.getTitle() + ".dataset");
			if (file.exists()) { 
				try { 
					openWindow(readDataset(file));
				} catch (IOException e) {
					IJ.error("An IO error occured reading file: " + file.getName() + "\n " 
							+ e.getLocalizedMessage());
				} catch (ClassNotFoundException e) {
					IJ.error("Can't recognize the file format: " + file.getName() + "\n" 
							+ e.getLocalizedMessage());
				}
			} else {
				IJ.error("Can't find previous analysis results." 
						+ " It needs to be saved in the same folder as your image data.");
			}
			return;
		}

		if (cmd.equals("import")) { 
			JFileChooser fc = new JFileChooser();
			if (fc.showOpenDialog(IJ.getApplet()) == JFileChooser.APPROVE_OPTION) {
				try {
					openWindow(TrajDataset.importDatasetFromText(fc.getSelectedFile()));
				} catch (IOException e) {
					IJ.error("An IO error occured reading file: " + fc.getSelectedFile().getName());
				}
			}
			return;
		}
	}
}
