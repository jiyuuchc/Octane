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

import ij.ImagePlus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.GeneralPath;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class TrajsTable extends JTable {
	private static final long serialVersionUID = 8080339334223890218L;

	private static String[] ColumnNames_ = { "Frame", "Len"};
	private static Class<?> [] ColumnClasses_ = {Integer.class, Integer.class};
	
	private Vector<Trajectory> data_ = null;
	private boolean isFirstTable_;
	private NodesTable nodesTable_ = null;
	private TableRowSorter<Model> sorter_;
	private ImagePlus imp_ = null;
	
	public TrajsTable(Vector<Trajectory> data, boolean isFirstTable) {
		super();
		
		data_=data;
		isFirstTable_ = isFirstTable;
		setModel(new Model());
		setColumnSelectionAllowed(false);
		setFont(new Font("", Font.PLAIN, 10));
		setRowSelectionAllowed(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		sorter_ = new TableRowSorter<Model>((Model) getModel());
		sorter_.setRowFilter(new Filter());
		setRowSorter(sorter_);
		getColumnModel().getColumn(0).setPreferredWidth(50);
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (nodesTable_ != null) {
					populateNodesTable();
				}
				if (imp_ != null) {
					drawOverlay();
				}
			}
			
		});
	}

	public void setNodesTable(NodesTable nodesTable) {
		nodesTable_ = nodesTable;
	}

	public void populateNodesTable() {
		int row = getSelectedRow();
		if (row >=0) {
			int index = convertRowIndexToModel(row);
			nodesTable_.setData(data_.get(index));
		}		
	}
	
	public void drawOverlay() {
		GeneralPath path = new GeneralPath();
		int [] rows = getSelectedRows();
		for (int i = 0; i < rows.length; i++) {
			if (rows[i] < 0) {
				return;
			}
			int index = convertRowIndexToModel(rows[i]);
			Trajectory v = data_.get(index);
	
			path.moveTo(v.getX(0), v.getY(0));
			for (int j = 1; j < v.size(); j++) {
				path.lineTo(v.getX(j), v.getY(j));
			}
		}
		imp_.setOverlay(path, Color.yellow, new BasicStroke(1.5f));			
	}
	
	public void reSort() {
		sorter_.sort();
	}

	public void SetImp(ImagePlus imp) {
		imp_ = imp;
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
