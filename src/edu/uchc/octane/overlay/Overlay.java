//FILE:          Overlay.java
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

import java.awt.AWTEvent;

import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.process.Blitter;
import ij.process.ColorProcessor;
//import ij.process.ImageProcessor;

public class Overlay implements PlugIn, DialogListener, ImageListener{
	int [] wList;
	String [] titles;
	ImagePlus imp;
	int index0cache;
	ColorProcessor ip0cache;
	int [] indexcache;
	ColorProcessor [] ipcache;
	double [] xoffset = new double[3], yoffset = new double[3];
	GenericDialog gd;
	
	@Override
	public void run(String cmd) {
		//overlayImp = new ImagePlus("overlay");
		ImagePlus.addImageListener(this);
		imp = null;
		indexcache = new int[3];
		ipcache = new ColorProcessor[3];
		showDialog();
	}
	
	boolean showDialog() {
		wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.noImage();
			return false;
		}
		titles = new String[wList.length + 1];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp != null) {
				titles[i+1] = imp.getTitle();
			} else {
				titles[i+1] = "" + wList[i];
			}
		}
		titles[0] = "None";
		
		gd = new GenericDialog("Overlay");
		gd.addChoice("Image1:", titles, titles[1]);
		gd.addChoice("Image2:", titles, titles[0]);
		gd.addSlider("XOffset:", -100, 100, 0);
		gd.addSlider("YOffset:", -100, 100, 0);
		gd.addChoice("Image3:", titles, titles[0]);
		gd.addSlider("XOffset:", -100, 100, 0);
		gd.addSlider("YOffset:", -100, 100, 0);
		gd.addChoice("Image4:", titles, titles[0]);
		gd.addSlider("XOffset:", -100, 100, 0);
		gd.addSlider("YOffset:", -100, 100, 0);
		gd.addDialogListener(this);
		gd.setModal(false);
		gd.showDialog();

		//imp.close();
		
		//if(gd.wasCanceled()) {
		//	return false;
		//}
		return true;
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        int index0 = gd.getNextChoiceIndex();
        int [] index = new int[3];    
        index[0] = gd.getNextChoiceIndex();
        xoffset[0] = gd.getNextNumber();
        yoffset[0] = gd.getNextNumber();
        index[1] = gd.getNextChoiceIndex();
        xoffset[1] = gd.getNextNumber();
        yoffset[1] = gd.getNextNumber();
        index[2] = gd.getNextChoiceIndex();
        xoffset[2] = gd.getNextNumber();
        yoffset[2] = gd.getNextNumber();
        
        //ColorProcessor ip;

        if (index0 == 0) {
        	return true;
        }

        if (index0 != index0cache) {
        	index0cache = index0;
        	if (imp != null) {
        		imp.close();
        		imp = null;
        	}
            ImagePlus imp = WindowManager.getImage(wList[index0 - 1]);        	
        	ip0cache = (ColorProcessor) imp.getProcessor().convertToRGB();
        }
        
        //ip = (ColorProcessor) ip0cache.duplicate();
        
    	//ColorProcessor ip = (ColorProcessor) ip1.convertToRGB();
		for (int k = 0; k < 3; k++) {

			if (index[k] != 0) {
				if (index[k] != indexcache[k]) {
					indexcache[k] = index[k];
					ImagePlus imp = WindowManager.getImage(wList[index[k] - 1]);
					ipcache[k] = (ColorProcessor) imp.getProcessor()
							.convertToRGB();
				}
			}
		}
        
        updateImage();
		return true;
	}

	void updateImage() {
        ColorProcessor ip = (ColorProcessor) ip0cache.duplicate();		
        for (int k = 0; k < 3; k++) {
			if (indexcache[k] != 0) {
				ip.copyBits(ipcache[k], (int) xoffset[k], (int) yoffset[k],
						Blitter.MAX);
			}
		}
        if (imp == null || imp.getWindow() == null) {
			imp = new ImagePlus("Overlay", ip);
		} else {
			imp.setProcessor("Overlay", ip);
		}
		imp.show();
	}
	
	@Override
	public void imageClosed(ImagePlus arg0) {}		
	@Override
	public void imageOpened(ImagePlus arg0) {}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp.getID() == wList[index0cache - 1] ) {
			ip0cache = (ColorProcessor) imp.getProcessor().convertToRGB();
			updateImage();
			return;
		}
		
		for (int i = 0; i < 3; i++) {
			if (imp.getID() == wList[indexcache[i] - 1]) {
				ipcache[i] = (ColorProcessor) imp.getProcessor().convertToRGB();
				updateImage();
				return;
			}
		}
	}
	
}
