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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

public class TrajsTable extends JTable {

	private static String[] ColumnNames_ = { "Frame", "Len", "Marked","Notes"};
	private static Class<?> [] ColumnClasses_ = {Integer.class, Integer.class, Boolean.class, String.class};
	
	private Vector<Trajectory> data_ = null;
	private boolean [] isVisible_;
	private Model model_;

	class Model extends AbstractTableModel {

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
				return traj.get(0).frame;
			case 1:
				return traj.getLength();
			case 2:
				return traj.marked;
			case 3:
				return traj.note;
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
		
		@Override
		public boolean isCellEditable(int row, int col) {
			if (col == 2 || col == 3) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if (col == 3) {
				data_.get(row).note = (String) value;
			} else if (col == 2) {
				data_.get(row).marked = (Boolean)value;
			}
		}

	}

	public TrajsTable(Vector<Trajectory> data) {
		super();

		setData(data);
		
		model_ = new Model();
		setModel(model_);
		setColumnSelectionAllowed(false);
		setFont(new Font("", Font.PLAIN, 10));
		setRowSelectionAllowed(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		//setAutoCreateRowSorter(true);
		TableRowSorter<Model> sorter = new TableRowSorter<Model>(model_);
		setRowSorter(sorter);
		sorter.setRowFilter(new RowFilter<Model, Integer> () {
			@Override
			public boolean include(
					javax.swing.RowFilter.Entry<? extends Model, ? extends Integer> entry) {
				return isVisible_[entry.getIdentifier()];
			}
		});

		getColumnModel().getColumn(0).setPreferredWidth(30);
		getColumnModel().getColumn(1).setPreferredWidth(30);
		getColumnModel().getColumn(2).setPreferredWidth(30);
	}

	public void setData(Vector<Trajectory> data) {
		data_ = data;
		isVisible_ = new boolean[data.size()];
		Arrays.fill(isVisible_, true);
		clearSelection();
		if (model_ != null ) {
			model_.fireTableDataChanged();
		}
	}

	public void reverseMarkOfSelected() {
		boolean acted = false;
		int [] rows = getSelectedRows();
		for (int i = 0; i < rows.length; i++) {
			rows[i] = convertRowIndexToModel(rows[i]);
		}
		for (int i = 0; i < rows.length; i++) {		
			Trajectory v = data_.get(rows[i]);
			if (v.isMarked() == false) {
				v.mark(true);
				acted = true;
			}
		}
		if (!acted) {
			for (int i = 0; i < rows.length; i++) {
				data_.get(rows[i]).mark(false);
			}
		}
		model_.fireTableRowsUpdated(0,model_.getRowCount()-1);
	}

	public void deleteSelected() {
		int [] selected = getSelectedRows();
		Vector<Trajectory> toBeDeleted = new Vector<Trajectory>();
		for (int i = 0; i < selected.length; i++) {
			toBeDeleted.add(data_.get(convertRowIndexToModel(selected[i])));
		}
		data_.removeAll(toBeDeleted);
		clearSelection();
		model_.fireTableDataChanged();
	}
	
	public void hideUnmarked() {
		int cnt = 0;
		Iterator<Trajectory> itr = data_.iterator();
		while (itr.hasNext()) {
			isVisible_[cnt ++] = itr.next().marked; 
		}
		model_.fireTableDataChanged();
	}

	public void showAll() {
		Arrays.fill(isVisible_, true);
		model_.fireTableDataChanged();
	}
}
