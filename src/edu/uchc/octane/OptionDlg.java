//FILE:          OptionDlg.java
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
package edu.uchc.octane;

import ij.gui.GUI;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class OptionDlg extends JDialog {

	private static final long serialVersionUID = 8082293900073714561L;
	JRadioButton ctlPointIndicator_;
	JRadioButton ctlAreaIndicator_;
	JCheckBox ctlShowIndicator_;
	//JCheckBox ctlUseVirtualStack_;
	JCheckBox ctlRefinePeak_;
	JCheckBox ctlOmitSingleFrameTraj_;
	JFormattedTextField ctlPalmRatio_;
	JFormattedTextField ctlPalmPSDWidth_;
	JFormattedTextField ctlPalmThreshold_;
	JFormattedTextField ctlMaxDisplacement_; 
	JFormattedTextField ctlBlinking_;

	public OptionDlg() {
		super();
		setTitle("Options");
		
		Container pane = new Panel() {
			private static final long serialVersionUID = -5807122858559364680L;
			public Insets getInsets() {
				return new Insets(10, 10, 10, 10);
			}
		};

		BoxLayout layout = new BoxLayout(pane, BoxLayout.Y_AXIS);
		pane.setLayout(layout);
		getContentPane().add(pane, BorderLayout.NORTH);

//		ctlPointIndicator_ = new JRadioButton("Point Indicator");
//		ctlAreaIndicator_ = new JRadioButton("Squre indicator");
//		pane.add(ctlPointIndicator_);
//		pane.add(ctlAreaIndicator_);
//		ButtonGroup bg = new ButtonGroup();
//		bg.add(ctlPointIndicator_);
//		bg.add(ctlAreaIndicator_);
//		ctlPointIndicator_.setSelected(Prefs.pointIndicator_);
//		ctlAreaIndicator_.setSelected(!Prefs.pointIndicator_);
//		ctlPointIndicator_.setAlignmentX(0);
//		ctlAreaIndicator_.setAlignmentX(0);
//
//		ctlShowIndicator_ = new JCheckBox("Show Indicator");
//		pane.add(ctlShowIndicator_);
//		ctlShowIndicator_.setSelected(Prefs.showIndicator_);
//		ctlShowIndicator_.setAlignmentX(0);

		//ctlUseVirtualStack_ = new JCheckBox("Use Virtual Stack");
		//pane.add(ctlUseVirtualStack_);
		//ctlUseVirtualStack_.setSelected(Prefs.virtualStack_);
		//ctlUseVirtualStack_.setAlignmentX(0);

//		ctlRefinePeak_ = new JCheckBox("Refine Peak Position");
//		pane.add(ctlRefinePeak_);
//		ctlRefinePeak_.setSelected(Prefs.refinePeak_);
//		ctlRefinePeak_.setAlignmentX(0);

//		ctlOmitSingleFrameTraj_ = new JCheckBox("Omit Single Frame Trajectories");
//		pane.add(ctlOmitSingleFrameTraj_);
//		ctlOmitSingleFrameTraj_.setSelected(Prefs.omitSingleFrameTrajs_);
//		ctlOmitSingleFrameTraj_.setAlignmentX(0);

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JLabel label = new JLabel("PALM Ratio");
		label.setAlignmentX(0);
		panel.add(label);
		ctlPalmRatio_ = new JFormattedTextField(new Double(Prefs.palmRatio_));
		ctlPalmRatio_.setColumns(10);
		panel.add(ctlPalmRatio_);
		pane.add(panel);
		panel.setAlignmentX(0);

		panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		label = new JLabel("Palm PSD Width");
		label.setAlignmentX(0);
		panel.add(label);
		ctlPalmPSDWidth_ = new JFormattedTextField(new Double(Prefs.palmPSDWidth_));
		ctlPalmPSDWidth_.setColumns(10);
		panel.add(ctlPalmPSDWidth_);
		pane.add(panel);
		panel.setAlignmentX(0);

		panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		label = new JLabel("Palm Threshold");
		label.setAlignmentX(0);
		panel.add(label);
		ctlPalmThreshold_ = new JFormattedTextField(new Double(Prefs.palmThreshold_));
		ctlPalmThreshold_.setColumns(10);
		panel.add(ctlPalmThreshold_);
		pane.add(panel);
		panel.setAlignmentX(0);

		panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		label = new JLabel("Max displacement");
		label.setAlignmentX(0);
		panel.add(label);
		ctlMaxDisplacement_ = new JFormattedTextField(new Double(Prefs.trackerMaxDsp_));
		ctlMaxDisplacement_.setColumns(10);
		panel.add(ctlMaxDisplacement_);
		pane.add(panel);
		panel.setAlignmentX(0);
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		label = new JLabel("Blinking time");
		label.setAlignmentX(0);
		panel.add(label);
		ctlBlinking_ = new JFormattedTextField(new Integer(Prefs.trackerMaxBlinking_));
		ctlBlinking_ .setColumns(10);
		panel.add(ctlBlinking_);
		pane.add(panel);
		panel.setAlignmentX(0);

		panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		JButton okButton = new JButton("Ok");
		JButton cancelButton = new JButton("Cancel");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Prefs.pointIndicator_ = ctlPointIndicator_.isSelected();
				//Prefs.showIndicator_ = ctlShowIndicator_.isSelected();
				//Prefs.virtualStack_ = ctlUseVirtualStack_.isSelected();
				//Prefs.refinePeak_ = ctlRefinePeak_.isSelected();
				//Prefs.omitSingleFrameTrajs_ = ctlOmitSingleFrameTraj_.isSelected();
				Prefs.palmRatio_ = (Double) ctlPalmRatio_.getValue();
				Prefs.palmPSDWidth_ = (Double) ctlPalmPSDWidth_.getValue();
				Prefs.palmThreshold_ = (Double) ctlPalmThreshold_.getValue();
				Prefs.trackerMaxDsp_ = (Double) ctlMaxDisplacement_.getValue();
				Prefs.trackerMaxBlinking_ = (Integer) ctlBlinking_.getValue();
				Prefs.savePrefs();
				dispose();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		panel.add(okButton);
		panel.add(cancelButton);

		pack();
		GUI.center(this);
	}
}
