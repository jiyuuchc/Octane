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

/**
 * Class NodesTable displays the individual nodes (positon, frame etc) of a trajectory.
 */
public class NodesTable extends JTable{

	private static String[] ColumnNames_ = { "F", "X", "Y", "Z", "I","Q"};
	private static Class<?> [] ColumnClasses_ = {Integer.class, Double.class, Double.class, Double.class, Integer.class, Double.class};
	
	private Trajectory traj_ = null;
	private Model model_;
	
	/**
	 * Instantiates a new nodes table.
	 *
	 * @param traj the trajectory
	 */
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
		//getColumnModel().getColumn(0).setPreferredWidth(70);
		//getColumnModel().getColumn(1).setPreferredWidth(70);
		//getColumnModel().getColumn(2).setPreferredWidth(70);		
	}

	/**
	 * Sets the trajectory data.
	 *
	 * @param traj the new trajectory data
	 */
	public void setData(Trajectory traj) {
		traj_ = traj;
		clearSelection();
		model_.fireTableDataChanged();
		//setRowSelectionInterval(0,0);
	}

	/**
	 * Gets the data.
	 *
	 * @return the trajectory data
	 */
	public Trajectory getData() {
		return traj_;
	}

	/**
	 * Returns the current selected molecule node (position) in node table.
	 *
	 * @return the current node
	 */
	public SmNode getCurrentNode() {
		int row = getSelectedRow();
		if (row >=0 && getData() != null) {
			int index = convertRowIndexToModel(row);
			return getData().get(index);
		} else {
			return null;
		}				
	}
	
	protected class CellRenderer extends DefaultTableCellRenderer {

		protected void setValue(Object obj) {
			if (obj.getClass() == Double.class) {
				setText(String.format("%4.3f", (Double) obj));
			}
		}
	}

	protected class Model extends AbstractTableModel {

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return ColumnNames_.length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
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
				return node.z;
			case 4:
				return node.height;
			case 5:
				return node.residue;
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
