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

import java.awt.Font;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class NodesTable extends JTable{
	private static final long serialVersionUID = 1691643945428279661L;

	private static String[] ColumnNames_ = { "Frame", "X", "Y", "Q"};
	private static Class<?> [] ColumnClasses_ = {Integer.class, Double.class, Double.class,Double.class};
	
	private Trajectory traj_;

	public NodesTable(Trajectory traj) {
		super();

		traj_ = traj;
		setModel(new Model());
		setColumnSelectionAllowed(false);
		setFont(new Font("", Font.PLAIN, 10));
		setRowSelectionAllowed(true);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setDefaultRenderer(Double.class, new CellRenderer());

		getColumnModel().getColumn(0).setPreferredWidth(100);
		getColumnModel().getColumn(1).setPreferredWidth(100);
		getColumnModel().getColumn(2).setPreferredWidth(100);
	}


	public void setData(Trajectory traj) {
		traj_ = traj;
		repaint();
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
			return traj_.size();
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
				return (int)node.quality;
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
