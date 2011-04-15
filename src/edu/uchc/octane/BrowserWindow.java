//FILE:          BrowserWindow.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 4/14/11
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

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class BrowserWindow extends JFrame {
	String path_;
	Browser browser_;
	TrajsTable trajsTable_;
	NodesTable nodesTable_;

	public BrowserWindow(Browser b) {
		super();
		if (IJ.isLinux()) setBackground(ImageJ.backgroundColor);
		browser_ = b;
		SetupWindow();
	}

	JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		JMenu viewMenu = new JMenu("View");
		editMenu.setMnemonic(KeyEvent.VK_V);
		JMenu processMenu = new JMenu("Process");
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(processMenu);

		JMenuItem item; 
		JCheckBoxMenuItem cbItem;

		item = new JMenuItem("Save");
		fileMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browser_.saveDataset(); 
			}			
		});
		
		item = new JMenuItem("Rebuild");
		fileMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) { 
				browser_.rebuildTrajectories();
			}			
		});

		item = new JMenuItem("Delete");
		editMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browser_.deleteSelectedTrajectories();
				trajsTable_.clearSelection();
				trajsTable_.tableDataChanged();
			}
		});
		
		item = new JMenuItem("Hide Unmarked");
		viewMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trajsTable_.hideUnmarked();
			}
		});
		
		item = new JMenuItem("Show All");
		viewMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trajsTable_.showAll(); 
			}
		});
		
		viewMenu.addSeparator();

		cbItem = new JCheckBoxMenuItem("Show Overlay", Prefs.showOverlay_);
		viewMenu.add(cbItem);
		cbItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				JCheckBoxMenuItem cb = (JCheckBoxMenuItem) e.getSource();
				Prefs.showOverlay_ = cb.getState();
			}
		});

		item = new JMenuItem("Flow Map");
		processMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				browser_.constructFlowMap();
			}
		});

		item = new JMenuItem("Mobility Map");
		processMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				browser_.constructMobilityMap();
			}
		});

		item = new JMenuItem("PALM");
		processMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				browser_.constructPalm();
			}
		});

		item = new JMenuItem("IFS");
		processMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				browser_.constructIFS();
			}
		});
		
		processMenu.addSeparator();
		
		item = new JMenuItem("Trajectory Length Histogram");
		processMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				browser_.showLengthHistogram();
			}
		});

		item = new JMenuItem("Residue Histogram");
		processMenu.add(item);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e){
				browser_.showResidueHistogram();
			}
		});

		return menuBar;
	}
	
	JPanel createButtonBox() {
		final JPanel buttonBox = new JPanel(); // button box
		JButton button;

		buttonBox.setLayout(new BoxLayout(buttonBox,BoxLayout.LINE_AXIS));
		buttonBox.setBorder(BorderFactory.createEmptyBorder(0,10,5,5));
		buttonBox.add(Box.createHorizontalGlue());
		button = new JButton("SelectRoi");
		buttonBox.add(button);

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trajsTable_.getSelectionModel().setValueIsAdjusting(true);
				browser_.selectTrajectoriesWithinRoi();
				trajsTable_.getSelectionModel().setValueIsAdjusting(false);
			}
		});
		
		buttonBox.add(Box.createRigidArea(new Dimension(10,0)));

		button = new JButton("Mark");
		buttonBox.add(button);
		
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trajsTable_.reverseMarkOfSelected();
			}
		});

		buttonBox.add(Box.createRigidArea(new Dimension(10,0)));

		button = new JButton("Export");
		buttonBox.add(button);

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browser_.copySelectedTrajectories();
			}
		});

		return buttonBox;
	}
	
	
	void Layout(){
		
		setBounds(100, 100, 660, 495);
		
		setJMenuBar(createMenuBar());
		
		JPanel mainPanel = new JPanel();
			
		final GridBagLayout layout = new GridBagLayout();
		mainPanel.setLayout(layout);
		
		GridBagConstraints constraints;
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = 0; 
		constraints.gridy = 0;
		constraints.weightx =0;
		constraints.weighty = 0;
		mainPanel.add(new JLabel("List of Traces:"), constraints);

		constraints.gridx = 1; 
		constraints.gridy = 0;
		constraints.weightx =0;
		constraints.weighty = 0;
		mainPanel.add(new JLabel("Current Trace:"), constraints);
				
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(5,5,5,5);		
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weightx = 0.3;
		constraints.weighty = 1.0;
		final JScrollPane trajsPane = new JScrollPane();
		trajsTable_ = new TrajsTable(browser_.getData());
		trajsTable_.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 ) {
					browser_.animate();
				}
			}
		});
		trajsTable_.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				browser_.drawOverlay();
				browser_.getImp().killRoi();
				if (nodesTable_ != null) {
					populateNodesTable();
				}
			}
		});
		trajsPane.setViewportView(trajsTable_);
		mainPanel.add(trajsPane, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.weightx = 0.7;
		constraints.weighty = 1.0;
		final JScrollPane nodesPane = new JScrollPane();
		nodesTable_ = new NodesTable(null);
		nodesTable_.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				browser_.drawBox();
			}
		} );
		nodesPane.setViewportView(nodesTable_);		
		mainPanel.add(nodesPane, constraints);

		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.weightx = 0;
		constraints.weighty = 0;		
		mainPanel.add(createButtonBox(), constraints);
		
		add(mainPanel);
		
		ImagePlus imp = browser_.getImp();
		if (imp != null) {
			setTitle(imp.getTitle() + " Trajectories");
//			trajsTable_.SetImp(imp_);
//			nodesTable_.SetImp(imp_);
		} else {
			setTitle("Trajectories");
		}
	}
	
	void SetupWindow() {
		Layout();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		ImagePlus imp = browser_.getImp();
		if (imp != null ) {
			imp.getWindow().addWindowListener(new WindowAdapter() {
				@Override
				public void windowIconified(WindowEvent e) {
					setVisible(false);
				}
				
				@Override
				public void windowDeiconified(WindowEvent e) {
					setVisible(true);
				}
				
				@Override
				public void windowClosed(WindowEvent e) {
					dispose();
				}
			});
		}
	}
	
	void populateNodesTable() {
		if (trajsTable_.getSelectedRowCount() > 0) {
			int index = getSelectedTrajectoryIndex();
			nodesTable_.setData(browser_.getData().getTrjectoryByIndex(index));
		} else {
			nodesTable_.setData(null);
		}
		if (trajsTable_.getSelectedRowCount() == 1) { 
			nodesTable_.setRowSelectionInterval(0,0);
		}
	}

	public int getSelectedTrajectoryIndex() {
		int row = trajsTable_.getSelectedRow();
		if (row >= 0) {
			return trajsTable_.convertRowIndexToModel(row);
		} else {
			return -1;
		}
	}
	
	public int[] getSelectedTrajectories() {
		int [] selected = trajsTable_.getSelectedRows();
		for (int i = 0; i < selected.length; i++) {
			selected[i] = trajsTable_.convertRowIndexToModel(selected[i]);
		}
		return selected;				
	}
	
	public int[] getSelectedTrajectoriesOrAll() {
		if (trajsTable_.getSelectedRowCount() <= 1) {
			int [] selected = new int[trajsTable_.getRowCount()];
			for (int i = 0; i < selected.length; i++) {
				selected[i] = i;
			}
			return selected;
		} else {
			return getSelectedTrajectories();
		}		
	}
	
	public void selectTrajectoriesByIndex(int index) {
		if (index < 0) {
			trajsTable_.clearSelection();
		}
		int row = trajsTable_.convertRowIndexToView(index);
		trajsTable_.setRowSelectionInterval(row,row);
		Rectangle r = trajsTable_.getCellRect(row, 0, true);
		trajsTable_.scrollRectToVisible(r);
	}
	
	public void addTrajectoriesToSelection(int index) {
		int row = trajsTable_.convertRowIndexToView(index);
		trajsTable_.addRowSelectionInterval(row,row);
	}

	public SmNode getCurrentNode() {
		int row = nodesTable_.getSelectedRow();
		if (row >=0 && nodesTable_.getData() != null) {
			int index = nodesTable_.convertRowIndexToModel(row);
			return nodesTable_.getData().get(index);
		} else {
			return null;
		}				
	}

	public void updateNewData() {
		trajsTable_.setData(browser_.getData());
	}
	
	@Override
	public void dispose() {
		browser_.saveDataset();
		Prefs.savePrefs();
		super.dispose();
	}

}
