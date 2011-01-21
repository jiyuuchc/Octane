//FILE:          NodesTable.java
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

import ij.ImagePlus;
import ij.gui.Roi;
import java.lang.Math;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class NodesTable extends JTable{
	private static final long serialVersionUID = 1691643945428279661L;

	private static String[] ColumnNames_ = { "Frame", "X", "Y", "Q"};
	private static Class<?> [] ColumnClasses_ = {Integer.class, Double.class, Double.class, Double.class};
	
	private Trajectory traj_ = null;
	private ImagePlus imp_ = null;
	private Model model_;
	public NodesTable(Trajectory traj) {
		super();

		traj_ = traj;
		model_ = new Model(); 
		setModel(model_);
		setColumnSelectionAllowed(false);
		setFont(new Font("", Font.PLAIN, 10));
		setRowSelectionAllowed(true);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setDefaultRenderer(Double.class, new CellRenderer());

		getColumnModel().getColumn(0).setPreferredWidth(100);
		getColumnModel().getColumn(1).setPreferredWidth(100);
		getColumnModel().getColumn(2).setPreferredWidth(100);
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (imp_ != null && traj_ != null) {
					drawBox();
				}
			}
		} );
				
	}

	public void drawBox() {
		int row = getSelectedRow();
		if (row >=0) {
			int index = convertRowIndexToModel(row);
			int x = (int) Math.round(traj_.getX(index));
			int y = (int) Math.round(traj_.getY(index));
			int f = traj_.getFrame(index);
			imp_.setSlice(f);
			imp_.setRoi(new Roi(x - 5, y - 5, 11, 11));
		}				
	}
	
	public void setData(Trajectory traj) {
		traj_ = traj;
		model_.fireTableDataChanged();
		setRowSelectionInterval(0,0);
	}
	
	public void SetImp(ImagePlus imp) {
		imp_ = imp;
	}

	class CellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1539239439156768797L;

		protected void setValue(Object obj) {
			setText(String.format("%4.2f", (Double) obj));
		}
	}

	class Model extends AbstractTableModel {
		private static final long serialVersionUID = 4995248712107810654L;

		@Override
		public int getColumnCount() {
			return ColumnNames_.length;
		}

		@Override
		public int getRowCount() {
			if (traj_ != null) {
				return traj_.size();
			} else {
				return 0;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int colIndex) {
			SmNode node = traj_.get(rowIndex);
			switch (colIndex) {
			case 0:
				return node.frame;
			case 1:
				return node.x;
			case 2:
				return node.y;
			case 3:
				return node.quality;
			}
			return null;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return ColumnNames_[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return ColumnClasses_[columnIndex];
		}
	}

}
