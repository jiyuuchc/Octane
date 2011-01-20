//FILE:          TrajsTable.java
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
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;


public class TrajsTable extends JTable {
	private static final long serialVersionUID = 8080339334223890218L;

	private static String[] ColumnNames_ = { "FirstFrame", "Length"};
	private static Class<?> [] ColumnClasses_ = {Integer.class, Integer.class};
	
	private Vector<Trajectory> data_;
	private boolean isFirstTable_;
	
	public TrajsTable(Vector<Trajectory> data, boolean isFirstTable) {
		super();
		
		data_=data;
		isFirstTable_ = isFirstTable;
		setModel(new Model());
		setColumnSelectionAllowed(false);
		setFont(new Font("", Font.PLAIN, 10));
		setRowSelectionAllowed(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		TableRowSorter<Model> sorter = new TableRowSorter<Model>((Model) getModel());
		sorter.setRowFilter(new Filter());
		setRowSorter(sorter);
		getColumnModel().getColumn(0).setPreferredWidth(200);	
	}

	class Model extends AbstractTableModel {
		private static final long serialVersionUID = -1936221743708539850L;

		@Override
		public int getColumnCount() {
			return ColumnNames_.length;
		}

		@Override
		public int getRowCount() {
			return data_.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int colIndex) {
			Trajectory traj = data_.get(rowIndex);
			switch (colIndex) {
			case 0:
				return traj.getFrame(0);
			case 1:
				return traj.size();
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

	class Filter extends RowFilter<Model, Integer> {

		@Override
		public boolean include(javax.swing.RowFilter.Entry<? extends TrajsTable.Model, ? extends Integer> entry) {
			int index = entry.getIdentifier();
			return data_.get(index).isDisabled() != isFirstTable_;
		}

	}
}
