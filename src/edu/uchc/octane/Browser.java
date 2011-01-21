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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import ij.ImagePlus;

public class Browser extends JFrame{
	private static final long serialVersionUID = -967387866057692460L;
	
	private ImagePlus imp_ = null;
	private TrajDataset dataset_ = null;
	TrajsTable trajsTable1_, trajsTable2_;
	NodesTable nodesTable1_;
	JButton button_l_, button_r_;
	
	private void Layout(){
		final GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		
		final JScrollPane trajsPane1 = new JScrollPane();
		final JScrollPane trajsPane2 = new JScrollPane();
		final JScrollPane nodesPane1 = new JScrollPane();
		final JPanel panel = new JPanel();
		button_l_ = new JButton("<-");
		button_r_ = new JButton("->");
		
		panel.setLayout(new BoxLayout(panel,BoxLayout.PAGE_AXIS));
		panel.add(button_l_);
		panel.add(Box.createRigidArea(new Dimension(0,10)));
		panel.add(button_r_);
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		button_l_.setAlignmentX(CENTER_ALIGNMENT);
		button_r_.setAlignmentX(CENTER_ALIGNMENT);

		GridBagConstraints constraints;
		
		constraints = new GridBagConstraints();

		constraints.insets = new Insets(5,5,5,5);
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.weightx = 0;
		constraints.weighty = 0.7;		
		add(panel, constraints);

		constraints.fill = GridBagConstraints.BOTH;
		constraints.ipadx = 10;
		constraints.ipady = 10;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.weighty = 0.7;
		add(trajsPane1, constraints);
		trajsTable1_ = new TrajsTable(dataset_.getTrajectories(), true);
		trajsPane1.setViewportView(trajsTable1_);
		
		constraints.gridx = 2;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.weighty = 0.7;		
		add(trajsPane2, constraints);
		trajsTable2_ = new TrajsTable(dataset_.getTrajectories(), false);
		trajsPane2.setViewportView(trajsTable2_);

		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 3;
		constraints.weightx = 1.0;
		constraints.weighty = 0.3;
		add(nodesPane1, constraints);
		nodesTable1_ = new NodesTable(null);
		nodesPane1.setViewportView(nodesTable1_);
		
		setBounds(100, 100, 460, 595);
		if (imp_ != null) {
			setTitle(imp_.getTitle() + " Trajectories");
			trajsTable1_.SetImp(imp_);
			trajsTable2_.SetImp(imp_);
			nodesTable1_.SetImp(imp_);
		} else {
			setTitle("Trajectories");
		}
	}
	
	private void ConnectSignals() {
		trajsTable1_.setNodesTable(nodesTable1_);
		trajsTable2_.setNodesTable(nodesTable1_);
		
		button_r_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int [] rows = trajsTable1_.getSelectedRows();
				for (int i = 0; i < rows.length; i++) {
					int index = trajsTable1_.convertRowIndexToModel(rows[i]);
					Trajectory v = dataset_.getTrajectories().get(index);
					v.setEnable(false);
				}
				trajsTable1_.clearSelection();
				trajsTable1_.reSort();
				trajsTable2_.reSort();
			}
		});
		
		button_l_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int [] rows = trajsTable2_.getSelectedRows();
				for (int i = 0; i < rows.length; i++) {
					int index = trajsTable2_.convertRowIndexToModel(rows[i]);
					Trajectory v = dataset_.getTrajectories().get(index);
					v.setEnable(true);
				}
				trajsTable2_.clearSelection();
				trajsTable1_.reSort();
				trajsTable2_.reSort();
			}
		});

	}

	private void SetupWindow() {
		Layout();
		ConnectSignals();
	}
	
	public Browser(TrajDataset data) {
		super();
		dataset_ = data;
		SetupWindow();
	}

	public Browser(ImagePlus imp) {
		super();
		imp_ = imp;
		dataset_ = new TrajDataset(imp);
		SetupWindow();
	}
	
}
