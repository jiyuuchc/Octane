//FILE:          HPFilter.java
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
package edu.uchc.octane.hpfilter;

import java.awt.AWTEvent;

import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;

public class HPFilter implements ExtendedPlugInFilter, DialogListener {

	double weight = 10.0;
	boolean previewing = true;
	int flags = DOES_16 | PARALLELIZE_STACKS | FINAL_PROCESSING | KEEP_PREVIEW;
	
	void MinMaxHPFilter(ImageProcessor ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		short [] pixels = (short []) ip.getPixels();
		short [] pixles2 = new short[width * height];

		for (int x = 1; x < width - 1 ; x++) {
			for (int y = 1; y < height - 1; y++ ) {
				int m = 65536;
				for (int x0 = x-1; x0 <= x+1 ; x0 ++) {
					for (int y0 = y-1; y0 <= y+1; y0++) {
						m = Math.min(m, pixels[y0*width + x0] & 0xffff);
						}
					}
				
				for (int x0 = x-1; x0 <= x+1 ; x0 ++) {
					for (int y0 = y-1; y0 <= y+1; y0++) {
						pixles2[y0*width + x0] = (short) Math.max( pixles2[y0*width + x0]&0xffff, m);
						}
					}
				
				}
			}
		
		//treat border differently
		for (int x = 1; x < width - 1; x++) {
			pixles2[x] = pixles2[x + width];
			pixles2[x + width * (height - 1)] = pixles2[x + width * (height -2 )]; 
		}
		for (int y = 0; y < height ; y++) {
			pixles2[y*width] = pixles2[y*width + 1];
			pixles2[y*width + width -1] = pixles2[y*width + width -2];
		}

		BackgroundSubtracter bs = new BackgroundSubtracter();
		ImageProcessor ip1 = ip.duplicate();
		bs.rollingBallBackground(ip1, 50, false, false, false, true, true);
		short [] pixels3 = (short []) ip1.getPixels();
		for (int p = 0; p < width*height; p++) {
			double offset = (pixels[p] - Math.min(pixles2[p], pixels[p])) * weight / 10;
			pixels[p] = (short)(pixels3[p] + ((int)offset)&0xffff);
		}
	}

	@Override
	public void setNPasses(int npass) {
	}

	@Override
	public int showDialog(ImagePlus imp, String cmd, PlugInFilterRunner pfr) {
        GenericDialog gd = new GenericDialog(cmd);
        gd.addSlider("Filter weight:", 0.0, 50, weight);
        //gd.addNumericField("Filter weight:", weight, 1);
        gd.addPreviewCheckbox(pfr);
        gd.addDialogListener(this);
        gd.getPreviewCheckbox().setState(true);
        previewing = true;
        gd.showDialog();
        previewing = false;
        if (gd.wasCanceled()) return DONE;
        IJ.register(this.getClass());       //protect static class variables (filter parameters) from garbage collection

        return IJ.setupDialog(imp, flags);  //ask whether to process all slices of stack (if a stack)
    }

	@Override
	public void run(ImageProcessor ip) {
		MinMaxHPFilter(ip);
        if (previewing && (ip instanceof ShortProcessor)) {
            ip.resetMinAndMax();
        }
	}

	@Override
	public int setup(String cmd, ImagePlus imp) {
		if (cmd.equals("final")) {
			imp.getProcessor().resetMinAndMax();
			return DONE;
		}
		return flags;
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		weight = gd.getNextNumber();
		return true;
	}

}
