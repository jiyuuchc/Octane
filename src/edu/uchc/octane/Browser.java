//FILE:          Browser.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 1/16/11
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

import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.io.FileInfo;

public class Browser extends JFrame implements ClipboardOwner{
	private static final long serialVersionUID = -967387866057692460L;
	
	private ImagePlus imp_ = null;
	private TrajDataset dataset_ = null;
	TrajsTable trajsTable_;
	NodesTable nodesTable_;
	JButton buttonRoi_,buttonMark_, buttonExport_;
	
	private void Layout(){
		final GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		
		final JScrollPane trajsPane = new JScrollPane();
		final JScrollPane nodesPane = new JScrollPane();
		final JPanel panel = new JPanel();
		trajsTable_ = new TrajsTable(dataset_.getTrajectories());
		trajsPane.setViewportView(trajsTable_);
		nodesTable_ = new NodesTable(null);
		nodesPane.setViewportView(nodesTable_);
		
		buttonRoi_ = new JButton("SelectRoi");
		buttonMark_ = new JButton("Mark");
		buttonExport_ = new JButton("Copy");
		panel.setLayout(new BoxLayout(panel,BoxLayout.LINE_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(0,10,5,5));
		panel.add(Box.createHorizontalGlue());
		panel.add(buttonRoi_);
		panel.add(Box.createRigidArea(new Dimension(10,0)));
		panel.add(buttonMark_);
		panel.add(Box.createRigidArea(new Dimension(10,0)));
		panel.add(buttonExport_);

		GridBagConstraints constraints;
		constraints = new GridBagConstraints();
		
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = 0; 
		constraints.gridy = 0;
		constraints.weightx =0;
		constraints.weighty = 0;
		add(new JLabel("List of Traces:"), constraints);

		constraints.gridx = 1; 
		constraints.gridy = 0;
		constraints.weightx =0;
		constraints.weighty = 0;
		add(new JLabel("Current Trace:"), constraints);
				
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(5,5,5,5);		
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weightx = 0.3;
		constraints.weighty = 1.0;
		add(trajsPane, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.weightx = 0.7;
		constraints.weighty = 1.0;
		add(nodesPane, constraints);

		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.weightx = 0;
		constraints.weighty = 0;		
		add(panel, constraints);
		
		setBounds(100, 100, 660, 495);
		if (imp_ != null) {
			setTitle(imp_.getTitle() + " Trajectories");
			trajsTable_.SetImp(imp_);
			nodesTable_.SetImp(imp_);
		} else {
			setTitle("Trajectories");
		}
	}
	
	private void ConnectSignals() {
		trajsTable_.setNodesTable(nodesTable_);
		
		buttonRoi_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (imp_ != null) {
					selectRoi();
				}
			}
		});
		
		buttonMark_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trajsTable_.reverseMarkOfSelected();
			}
		});
		
		buttonExport_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (imp_ != null) {
					copy();
				}
			}
		});

		imp_.getCanvas().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && imp_ != null) {
					findMolecule();
				}
				
			}
		});	
	}

	private void SetupWindow() {
		Layout();
		ConnectSignals();
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	private void selectRoi() {
		Roi roi = imp_.getRoi();
		if (roi == null) {
			return;
		}
		trajsTable_.clearSelection();
		int firstSel = -1;
		for (int i = 0; i < dataset_.getTrajectories().size(); i++) {
			Trajectory t = dataset_.getTrajectories().get(i);
			for (int j = 0; j< t.size(); j++) {
				if (roi.contains( (int)t.getX(j), (int)t.getY(j) )) {
					int l = trajsTable_.convertRowIndexToView(i);
					trajsTable_.addRowSelectionInterval(l,l);
					if (firstSel < 0) {
						firstSel = l;					
					break;
					}
				}
			}
		}

		if (firstSel >=0) {
			Rectangle r = trajsTable_.getCellRect(firstSel, 0, true);
			trajsTable_.scrollRectToVisible(r);
		}
	}
	
	private void copy() {
		//Vector<Trajectory> trajs = dataset_.getTrajectories();
		StringBuilder buf = new StringBuilder();
		int [] selected = trajsTable_.getSelectedRows();
		Trajectory traj;
		for (int i = 0; i < selected.length; i++) {
			int index = trajsTable_.convertRowIndexToModel(selected[i]);
			traj = dataset_.getTrajectories().get(index);
			for (int j = 0; j < traj.size(); j++) {
				buf.append(String.format("%10.4f, %10.4f, %10d, %5d%n", traj.getX(j), traj.getY(j), traj.getFrame(j), i));
			}
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection contents = new StringSelection(buf.toString());
		clipboard.setContents(contents, this);		
	}

	private void findMolecule() {
		ImageCanvas canvas = imp_.getCanvas();
		Point p = canvas.getCursorLoc();
		int frame = imp_.getSlice();
		
		int index = 0;
		boolean found = false;
		int lastIndex = dataset_.getTrajectories().size();
		while (!found && index < lastIndex) {
			Trajectory t = dataset_.getTrajectories().get(index);
			if (t.getFrame(0) <= frame && t.getFrame(t.size()-1) >= frame) {
				int fi = frame - t.getFrame(0);
				if (fi >= t.size()) { 
					fi = t.size() - 1;
				}
				while (t.getFrame(fi) > frame) {
					fi --;
				}
				if (t.getFrame(fi) == frame) {
					if ( Math.abs(t.getX(fi) - p.x) < 2.5 && Math.abs(t.getY(fi) - p.y) < 2.5) {
						found = true;
					}
				}
			}
			index ++;
		}
		if (found) {
			index = trajsTable_.convertRowIndexToView(index-1);
			trajsTable_.setRowSelectionInterval(index,index);
			Rectangle r = trajsTable_.getCellRect(index, 0, true);
			trajsTable_.scrollRectToVisible(r);
		}
	}
	
	public Browser(TrajDataset data) {
		super();
		dataset_ = data;
		SetupWindow();
	}

	public Browser(ImagePlus imp) {
		super();
		imp_ = imp;
		String path;
		FileInfo fi = imp.getOriginalFileInfo();
		if (fi != null) {
			path = fi.directory; 
		} else {
			IJ.showMessage("Can't find trajectories location");
			return;
		}		
		ObjectInputStream in;
		FileInputStream fs;
		File file = new File(path + File.separator + "analysis" + File.separator + "dataset");
		if (file.exists()) {
			try {
			fs = new FileInputStream(path + File.separator + "analysis" + File.separator + "dataset");
			in = new ObjectInputStream(fs);
			dataset_ = (TrajDataset) in.readObject();
			in.close();
			fs.close();
			} catch (Exception e) {
				IJ.showMessage("Can't recover analysis results. Data corrupt?");
				IJ.showMessage(e.toString() + "\n" + e.getMessage());
				return;
			}
		} else {
			try {
				dataset_ = new TrajDataset();
				dataset_.buildDataset(path);
			} catch (Exception e) {
				IJ.showMessage("Can't build trajectory. Data corrupt?");
				IJ.showMessage(e.toString() + "\n" + e.getMessage());
				return;				
			}
		} 
		SetupWindow();
	}

	@Override
	public void dispose() {
		dataset_.saveDataset();
		super.dispose();
	}

	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// who cares
		
	}
}
