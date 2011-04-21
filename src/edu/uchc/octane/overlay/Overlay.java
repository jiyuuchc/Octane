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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.awt.Choice;

import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.process.Blitter;
import ij.process.ColorProcessor;
//import ij.process.ImageProcessor;
import ij.process.ImageProcessor;

public class Overlay implements PlugIn, DialogListener, ImageListener{
	//int [] wList;
	ArrayList<ImagePlus> implist;
	String [] titles;
	ImagePlus overlayImp;
	ImagePlus firstImp;
	ImagePlus[] overlayImps = new ImagePlus[3];
	ColorProcessor ip0cache;
	ColorProcessor [] ipcache = new ColorProcessor[3];
	double [] xoffset = new double[3], yoffset = new double[3];
	NonBlockingClosableDialog gd;
	
	@Override
	public void run(String cmd) {
		overlayImp = null;
		implist = null;
		showDialog();
	}
	
	void buildImpList() {
		int [] wList = WindowManager.getIDList();
		if (wList == null) {
			implist = null;
			return;
		}
		implist = new ArrayList<ImagePlus>();
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp != null && imp.isVisible() && imp != this.overlayImp) {
				implist.add(imp);
			} 
		}
	}
	
	void getTitles() {
		if (implist == null || implist.size() == 0 ) {
			titles = null;
			return;
		}
		titles = new String[implist.size() + 1];
		for (int i = 0; i < implist.size(); i++) {
			ImagePlus imp = implist.get(i);
			titles[i+1] = imp.getTitle();
		}
		titles[0] = "None";
	}
	
	void showDialog() {
		buildImpList();
		getTitles();
		if (titles == null) {
			IJ.noImage();
			return;
		}
		
		gd = new NonBlockingClosableDialog("Overlay");
		gd.addChoice("Image1:", Arrays.copyOfRange(titles,1,titles.length), titles[1]);
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
		overlayImp = new ImagePlus("Overlay");
		ImagePlus.addImageListener(this);
		gd.showDialog();
		ImagePlus.removeImageListener(this);		
		if (!gd.wasOKed()) {
			if (overlayImp != null)
				overlayImp.close();
		}
		gd.dispose();
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

        if (implist.get(index0)!= firstImp) { //first image changed
        	firstImp = implist.get(index0);
        	ip0cache = (ColorProcessor) firstImp.getProcessor().duplicate().convertToRGB();
        }
        
        //ip = (ColorProcessor) ip0cache.duplicate();
        //ColorProcessor ip = (ColorProcessor) ip1.convertToRGB();
		for (int k = 0; k < 3; k++) {
			if (index[k] != 0) {
				if (implist.get(index[k]-1) != overlayImps[k]) {
					overlayImps[k] = implist.get(index[k]-1);
					ipcache[k] = (ColorProcessor) overlayImps[k].getProcessor().duplicate().convertToRGB();
				}
			} else {
				overlayImps[k] = null;
			}
		}
        
        updateImage();
		return true;
	}

	void updateImage() {
        ColorProcessor ip = (ColorProcessor) ip0cache.duplicate();
        for (int k = 0; k < 3; k++) {
			if (overlayImps[k]  != null) {
				ImageProcessor ip2 = ipcache[k];
				ip.copyBits(ip2.convertToRGB(), (int) xoffset[k], (int) yoffset[k], Blitter.MAX);
			}
		}

        overlayImp.setProcessor("Overlay", ip);
        overlayImp.show();
	}
	
	@Override
	public void imageClosed(ImagePlus imp) {
		if (imp == this.overlayImp)
			return;

		Vector<Choice> choices = gd.getChoices();

		if (implist.size() == 1 || imp == this.overlayImp) {
			// last image closed.
			gd.setVisible(false);
			gd.dispose();
			this.overlayImp.close();
			if (implist.size() == 1) {
				IJ.noImage();
			}
			ImagePlus.removeImageListener(this);
			return;
		}
		buildImpList();
		getTitles();
		if (imp == firstImp) {
			firstImp = implist.get(0);
			ip0cache = (ColorProcessor) firstImp.getProcessor().duplicate().convertToRGB();
		}
		for (int k = 0; k < 3; k++) {
			if (overlayImps[k] == imp) {
				overlayImps[k] = null;
			}
		}
		for (int k = 0; k < 4; k++) {
			choices.get(k).removeAll();
		}
		for (int k = 1; k < 4; k++) {
			choices.get(k).add(titles[0]);
		}
		for (int k = 0; k < 4; k++) {
			for (int l = 1; l< titles.length; l++) {
				choices.get(k).add(titles[l]);
			}
		}
		choices.get(0).select(firstImp.getTitle());
		for (int k = 1; k < 4; k++) {
			if (overlayImps[k-1] != null) {
				choices.get(k).select(overlayImps[k-1].getTitle());
			} else {
				choices.get(k).select(0);
			}
		}
		updateImage();
	}		
	
	@Override
	public void imageOpened(ImagePlus imp) {
		//titles = getTitles();
		if (imp == this.overlayImp)
			return;
		Vector<Choice> choices = gd.getChoices();
		for (int i = 0; i < choices.size(); i++) {
			Choice c = choices.get(i);
			c.add(imp.getTitle());
		}
		implist.add(imp);	
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp == this.overlayImp)
			return;
		boolean update = false;
		if (imp == firstImp ) {
			update = true;
			ip0cache = (ColorProcessor) firstImp.getProcessor().duplicate().convertToRGB();
		}
		
		for (int i = 0; i < 3; i++) {
			if (imp == overlayImps[i]) {
				update = true;
				ipcache[i] = (ColorProcessor) imp.getProcessor().duplicate().convertToRGB();
			}
		}
		if (update) {
			updateImage();
		}
	}
}
