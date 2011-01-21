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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class TrajsTable extends JTable {
	private static final long serialVersionUID = 8080339334223890218L;

	private static String[] ColumnNames_ = { "Frame", "Len", "Marked","Notes"};
	private static Class<?> [] ColumnClasses_ = {Integer.class, Integer.class, Boolean.class, String.class};
	
	private Vector<Trajectory> data_ = null;
	private NodesTable nodesTable_ = null;
	private ImagePlus imp_ = null;
	private Model model_;
	private Animator animator_ = null;

	public TrajsTable(Vector<Trajectory> data) {
		super();

		data_=data;
		model_ = new Model();
		setModel(model_);
		setColumnSelectionAllowed(false);
		setFont(new Font("", Font.PLAIN, 10));
		setRowSelectionAllowed(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setAutoCreateRowSorter(true);
		getColumnModel().getColumn(0).setPreferredWidth(50);
		
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && imp_ != null) {
					animate();
				}
			}
		});
		
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

	public void SetImp(ImagePlus imp) {
		if (imp_ == null) {
			imp_ = imp;
		} 
	}

	public void populateNodesTable() {
		int row = getSelectedRow();
		if (row >=0) {
			int index = convertRowIndexToModel(row);
			nodesTable_.setData(data_.get(index));
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
		imp_.setOverlay(path, Color.yellow, new BasicStroke(1f));			
	}
	
	public void animate() {
		if (animator_ == null) {
			animator_ = new Animator(imp_);
			animator_.setLoop(true);
		}
		
		int row = getSelectedRow();
		if (row >=0) {
			int index = convertRowIndexToModel(row);
			animator_.animate(data_.get(index));
		}
		
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
				return traj.get(0).frame;
			case 1:
				return traj.size();
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
}
