//FILE:          TrajsBrowser.java
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

import ij.IJ;
//import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.awt.Toolkit;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SpringLayout;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
//import ij.gui.ImageCanvas;

public class TrajsBrowser extends JDialog implements ClipboardOwner{

	private static final long serialVersionUID = 3801770545632175583L;
	private ImagePlus imp_;
	private Animator animator_;
	private TrajDataset dataset_;
	private SpringLayout springLayout_;
	private Model model_;
	private JTable trajList_;
	private JCheckBox loopCheckBox_;
	private JButton playButton_, rebuildButton_, overlayButton_, filterButton_;
	private JButton copyButton_, palmButton_, subStackButton_, hideButton_, unhideButton_;
	private TableRowSorter<Model> sorter_;
	private Roi roi_;
	private JScrollPane scrollPane_;
	boolean notesSaved_;

	public TrajsBrowser(ImagePlus imp) {
		super();
		imp_ = imp; 
		setTitle(imp.getTitle() + " Trajectories");
		notesSaved_ = false;

		dataset_ = new TrajDataset(imp);
		animator_ = new Animator(imp);
		
		springLayout_ = new SpringLayout();
		getContentPane().setLayout(springLayout_);
		setBounds(100, 100, 460, 395);

		Container pane = getContentPane();
		
		scrollPane_ = new JScrollPane();
		pane.add(scrollPane_);
		
		Font font12 = new Font("", Font.PLAIN, 12);
		
		final JCheckBox loopCheckBox = new JCheckBox();
		loopCheckBox.setFont(font12);
		loopCheckBox.setText("Loop");
		pane.add(loopCheckBox);
		loopCheckBox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				animator_.setLoop(loopCheckBox_.isSelected());
			}
		});
		loopCheckBox_ = loopCheckBox;

		final JButton playButton = new JButton();
		playButton.setFont(font12);
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = trajList_.convertRowIndexToModel(trajList_.getSelectedRow());
				//dataWin_.plotTrajectory(wd_.getTrajectories().get(index));
				animator_.animate(dataset_.getTrajectories().get(index));
			}
		});
		playButton.setText("Play");
		pane.add(playButton);
		playButton_ = playButton;

		final JButton overlayButton = new JButton();
		overlayButton.setFont(font12);
		overlayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				overlay();
			}
		});
		overlayButton.setText("Overlay");
		pane.add(overlayButton);
		overlayButton_ = overlayButton;

		final JButton rebuildButton = new JButton();
		rebuildButton.setFont(font12);
		rebuildButton.setText("Rebuild");
		pane.add(rebuildButton);
		rebuildButton_ = rebuildButton;
		rebuildButton.setSelected(false);
		rebuildButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dataset_.rebuildTracks();
				trajList_.repaint();
			}
		});
		rebuildButton_ = rebuildButton;

		JButton filterButton = new JButton();
		filterButton.setText("Filter");
		filterButton.setFont(font12);
		pane.add(filterButton);
		filterButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				roi_ = imp_.getRoi();
				sorter_.sort();
			}
		});
		filterButton_ = filterButton;

		JButton copyButton = new JButton();
		copyButton.setText("Copy");
		copyButton.setFont(font12);
		pane.add(copyButton);
		copyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				copyTable();
			}
		});
		copyButton_ = copyButton;

		JButton palmButton = new JButton();
		palmButton.setText("PALM");
		palmButton.setFont(font12);
		pane.add(palmButton);
		palmButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				constructPalm();
			}
		});
		palmButton_ = palmButton;

		JButton subStackButton = new JButton();
		subStackButton.setText("SubStack");
		subStackButton.setFont(font12);
		pane.add(subStackButton);
		subStackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				subStack();
			}
		});
		subStackButton_ = subStackButton;

		JButton hideButton = new JButton();
		hideButton.setText("ToggleHide");
		hideButton.setFont(font12);
		pane.add(hideButton);
		hideButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleSelection();
			}
		});
		hideButton_ = hideButton;

		JButton unhideButton = new JButton();
		unhideButton.setText("Unhide All");
		unhideButton.setFont(font12);
		pane.add(unhideButton);
		unhideButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				unhideAll();
			}
		});
		unhideButton_ = unhideButton;

		setupTable();
		customLayout();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		imp_.getCanvas().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				animator_.stopAnimation();
			}
		});
	}
	
	class Filter extends RowFilter<Model, Integer> {
		@Override
		public boolean include(
				Entry<? extends TrajsBrowser.Model, ? extends Integer> entry) {
			int index = entry.getIdentifier();
			
			if (Prefs.omitSingleFrameTrajs_ && dataset_.getTrajectories().get(index).size() < 2 ) {
				return false;
			}
			if (dataset_.getTrajectories().get(index).isDisabled()) {
				return false;
			}
			if (roi_ == null)
				return true;
			Trajectory v = dataset_.getTrajectories().get(index);
			for (int i = 0; i < v.size(); i++) {
				if (!roi_.contains((int) v.getX(i), (int) v.getY(i)))
					return false;
			}
			return true;
		}
	}

	class CellRenderer extends DefaultTableCellRenderer {		
//		@Override
//		public Component getTableCellRendererComponent(JTable t, Object v, boolean b1, boolean b2, int row, int col) {
//			Component c = super.getTableCellRendererComponent(t,v,b1,b2,row,col);
//			int index = trajList_.convertRowIndexToModel(row);
//			Trajectory traj = dataset_.getTrajectories().get(index);
//			if (traj.isDisabled()) {
//				c.setForeground(Color.gray);
//			} else {
//				c.setForeground(Color.black);
//			}
//			return c;
//		}

		private static final long serialVersionUID = 462529243462989270L;

		@Override
		protected void setValue(Object obj) {
			setText(String.format("%4f", (Double) obj));
		}
	}

	class Model extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		String[] ColumnNames_ = { "Frame", "Length", "Mobility", "Velocity", "Quality", "Note"};
		Class<?> [] ColumnClasses_ = {Integer.class, Integer.class, Double.class,Double.class, Double.class, String.class};

		public int getRowCount() {
			if (dataset_.getTrajectories() != null)
				return dataset_.getTrajectories().size();
			else
				return 0;
		}

		public int getColumnCount() {
			return ColumnNames_.length;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Trajectory v = dataset_.getTrajectories().get(rowIndex);
			switch (columnIndex) {
			case 0:
				return v.getFrame(0);
			case 1:
				return v.size();
			case 2:
				return v.getMobility();
			case 3:
				return v.getVectorialMobility();
			case 4:
				return v.getQuality();
			case 5:
				return v.getNote();
			}
			return null;
		}

		public boolean isCellEditable(int row, int col) {
			if (col == ColumnNames_.length - 1) { // only notes is editable
				return true;
			} else {
				return false;
			}
		}

		public void setValueAt(Object value, int row, int col) {
			if (col == trajList_.getColumnCount() - 1) {
				Trajectory v = dataset_.getTrajectories().get(row);
				v.setNote((String)value);
			}
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

	void close() {
		animator_.stopAnimation();
		dispose();
	}

	void setupTable() {
		trajList_ = new JTable();
		trajList_.setFont(new Font("", Font.PLAIN, 10));
		model_ = new Model();

		trajList_.setModel(model_);
		trajList_.setColumnSelectionAllowed(false);
		trajList_.setRowSelectionAllowed(true);
		trajList_.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		// trajList_.setAutoCreateRowSorter(true);
		sorter_ = new TableRowSorter<Model>(model_);
		trajList_.setRowSorter(sorter_);
		sorter_.setRowFilter(new Filter());
		trajList_.setDefaultRenderer(Double.class, new CellRenderer());
		//trajList_.setDefaultRenderer(Integer.class, new CellRenderer());
		//trajList_.setDefaultRenderer(String.class, new CellRenderer());
		TableColumn column = null;
		for ( int i = 0; i < trajList_.getColumnCount(); i ++) {
			column = trajList_.getColumnModel().getColumn(i);
			if ( i == trajList_.getColumnCount() - 1 ) {
				column.setPreferredWidth(200);
			} else if ( i == 2 || i == 3){
				column.setPreferredWidth(100);
			} else {
				column.setPreferredWidth(50);
			}
		}
		scrollPane_.setViewportView(trajList_);
		trajList_.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					play();
				}
			}
		});		
	}
	
	void customLayout() {
		JScrollPane scrollPane = scrollPane_;
		Container pane = getContentPane();
		springLayout_.putConstraint(SpringLayout.SOUTH, scrollPane, -5,
				SpringLayout.SOUTH, pane);
		springLayout_.putConstraint(SpringLayout.NORTH, scrollPane, 5,
				SpringLayout.NORTH, pane);
		springLayout_.putConstraint(SpringLayout.WEST, scrollPane, 5,
				SpringLayout.WEST, pane);
		springLayout_.putConstraint(SpringLayout.EAST, scrollPane, -100,
				SpringLayout.EAST, pane);

		springLayout_.putConstraint(SpringLayout.NORTH, loopCheckBox_, 5,
				SpringLayout.NORTH, pane);
		springLayout_.putConstraint(SpringLayout.EAST, loopCheckBox_, -5,
				SpringLayout.EAST, pane);
		springLayout_.putConstraint(SpringLayout.WEST, loopCheckBox_, 5,
				SpringLayout.EAST, scrollPane);

		springLayout_.putConstraint(SpringLayout.NORTH, playButton_, 3,
				SpringLayout.SOUTH, loopCheckBox_);
		springLayout_.putConstraint(SpringLayout.WEST, playButton_, 5,
				SpringLayout.EAST, scrollPane);
		springLayout_.putConstraint(SpringLayout.EAST, playButton_, -5,
				SpringLayout.EAST, pane);

		springLayout_.putConstraint(SpringLayout.NORTH, overlayButton_, 5,
				SpringLayout.SOUTH, playButton_);
		springLayout_.putConstraint(SpringLayout.WEST, overlayButton_, 5,
				SpringLayout.EAST, scrollPane);
		springLayout_.putConstraint(SpringLayout.EAST, overlayButton_, -5,
				SpringLayout.EAST, pane);

		springLayout_.putConstraint(SpringLayout.NORTH, rebuildButton_, 20,
				SpringLayout.SOUTH, overlayButton_);
		springLayout_.putConstraint(SpringLayout.WEST, rebuildButton_, 5,
				SpringLayout.EAST, scrollPane);
		springLayout_.putConstraint(SpringLayout.EAST, rebuildButton_, -5,
				SpringLayout.EAST, pane);

		springLayout_.putConstraint(SpringLayout.NORTH, filterButton_, 5,
				SpringLayout.SOUTH, rebuildButton_);
		springLayout_.putConstraint(SpringLayout.EAST, filterButton_, -5,
				SpringLayout.EAST, pane);
		springLayout_.putConstraint(SpringLayout.WEST, filterButton_, 5,
				SpringLayout.EAST, scrollPane);

		springLayout_.putConstraint(SpringLayout.NORTH, copyButton_, 5,
				SpringLayout.SOUTH, filterButton_);
		springLayout_.putConstraint(SpringLayout.EAST, copyButton_, -5,
				SpringLayout.EAST, pane);
		springLayout_.putConstraint(SpringLayout.WEST, copyButton_, 5,
				SpringLayout.EAST, scrollPane);

		springLayout_.putConstraint(SpringLayout.NORTH, palmButton_, 5,
				SpringLayout.SOUTH, copyButton_);
		springLayout_.putConstraint(SpringLayout.EAST, palmButton_, -5,
				SpringLayout.EAST, pane);
		springLayout_.putConstraint(SpringLayout.WEST, palmButton_, 5,
				SpringLayout.EAST, scrollPane);

		springLayout_.putConstraint(SpringLayout.NORTH, subStackButton_, 5,
				SpringLayout.SOUTH, palmButton_);
		springLayout_.putConstraint(SpringLayout.EAST, subStackButton_, -5,
				SpringLayout.EAST, pane);
		springLayout_.putConstraint(SpringLayout.WEST, subStackButton_, 5,
				SpringLayout.EAST, scrollPane);

		springLayout_.putConstraint(SpringLayout.NORTH, hideButton_, 10,
				SpringLayout.SOUTH, subStackButton_);
		springLayout_.putConstraint(SpringLayout.EAST, hideButton_, -5,
				SpringLayout.EAST, pane);
		springLayout_.putConstraint(SpringLayout.WEST, hideButton_, 5,
				SpringLayout.EAST, scrollPane);

		springLayout_.putConstraint(SpringLayout.NORTH, unhideButton_, 5,
				SpringLayout.SOUTH, hideButton_);
		springLayout_.putConstraint(SpringLayout.EAST, unhideButton_, -5,
				SpringLayout.EAST, pane);
		springLayout_.putConstraint(SpringLayout.WEST, unhideButton_, 5,
				SpringLayout.EAST, scrollPane);		
	}
	
	void play() {
		int index = trajList_.convertRowIndexToModel(trajList_.getSelectedRow());
		animator_.animate(dataset_.getTrajectories().get(index));		
	}

	void overlay() {
		//ImageCanvas canvas = imp_.getCanvas();
		GeneralPath path = new GeneralPath();
		int [] selected = trajList_.getSelectedRows();
		Trajectory traj;
		for (int i = 0; i < selected.length; i++) {
			int index = trajList_.convertRowIndexToModel(selected[i]);
			traj = dataset_.getTrajectories().get(index);
			if (!traj.isDisabled()) {
				path.moveTo(traj.getX(0), traj.getY(0));
				for (int j = 1; j < traj.size(); j++) {
					path.lineTo(traj.getX(j), traj.getY(j));
			    }
			}
		}
		imp_.setOverlay(path, Color.red, new BasicStroke(1f));
	}

	void toggleSelection() {
		int [] rows = trajList_.getSelectedRows();
		for (int i = 0; i < rows.length; i++) {
			int index = trajList_.convertRowIndexToModel(rows[i]);
			//IJ.log("disable " + rows[i] + " " + index);
			Trajectory v = dataset_.getTrajectories().get(index);
			v.setEnable(v.isDisabled());
		}
		sorter_.sort();
		//trajList_.updateUI();
	}

	void unhideAll() {
		for (int i = 0; i < dataset_.getTrajectories().size(); i++) {
			dataset_.getTrajectories().get(i).setEnable(true);
		}
		sorter_.sort();
		//trajList_.updateUI();
	}
	
	void copyTable() {
		Vector<Trajectory> trajs = dataset_.getTrajectories();
		StringBuilder buf = new StringBuilder();
		int [] selected = trajList_.getSelectedRows();
		Trajectory traj;
		for (int i = 0; i < selected.length; i++) {
			int index = trajList_.convertRowIndexToModel(selected[i]);
			traj = trajs.get(index);
			if ( ! traj.isDisabled()) {
				for (int j = 0; j < traj.size(); j++) {
					buf.append(String.format("%10.4f, %10.4f, %10d, %5d%n", traj.getX(j), traj.getY(j), traj.getFrame(j), i));
				}
			}
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection contents = new StringSelection(buf.toString());
		clipboard.setContents(contents, this);
	}

	void constructPalm() {
		// sorter_.sort();
		Rectangle rect;
		if (roi_ == null) {
			rect = imp_.getProcessor().getRoi();
		} else {
			rect = roi_.getBounds();
		}
		FloatProcessor ip = new FloatProcessor((int) (rect.width * Prefs.palmRatio_) + 1, (int) (rect.height * Prefs.palmRatio_) + 1);
		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmRatio_;
		Vector<Trajectory> trajs = dataset_.getTrajectories();
		int nPlotted = 0;
		int nSkipped = 0;
		for ( int i = 0; i < trajs.size(); i ++) {
			//int rowIndex = trajList_.convertRowIndexToModel(i);
			Trajectory traj = trajs.get(i);
			if ( ! traj.isDisabled() ) {
				double xx = traj.getX(0);
				double yy = traj.getY(0);
				boolean converge = true;
				for (int j = 1; j < traj.size(); j++ ) {
					if (Math.abs(xx / j - traj.getX(j)) > Prefs.palmThreshold_ || Math.abs(yy / j - traj.getY(j)) > Prefs.palmThreshold_ ) {
						converge = false;
						break;
					}
					xx += traj.getX(j);
					yy += traj.getY(j);
				}
				if (converge) {
					xx /= traj.size();
					yy /= traj.size();
					double xs = (xx - rect.x) * Prefs.palmRatio_;
					double ys = (yy - rect.y) * Prefs.palmRatio_;
					//IJ.log(String.format("%5d%6d%6d", rowIndex, x, y));
					for (int x = Math.max(0, (int)(xs - 3*psdWidth)); x < Math.min(ip.getWidth(), (int)(xs + 3*psdWidth)); x ++) {
						for (int y = Math.max(0, (int)(ys - 3*psdWidth)); y < Math.min(ip.getHeight(), (int)(ys + 3*psdWidth)); y++) {
							double v = Math.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*psdWidth*psdWidth) );
							ip.setf(x, y, (float)v + ip.getf(x,y));
						}
					}
					nPlotted ++;
				} else {
					nSkipped ++;
				}
			}
		}
		ImagePlus img = new ImagePlus("PALM", ip);
		img.show();
		IJ.log(String.format("Plotted %d molecules, skipped %d molecules.", nPlotted, nSkipped));
	}

	void constructPalmStack()
	{		
		Rectangle rect;
		if (roi_ == null) {
			rect = imp_.getProcessor().getRoi();
		} else {
			rect = roi_.getBounds();
		}

		ImageStack stack = new ImageStack((int) (rect.width * Prefs.palmRatio_), (int) (rect.height * Prefs.palmRatio_));
		for (int i = 0; i < imp_.getStackSize(); i++){
			//IJ.log("setup stack" + i);
			stack.addSlice("Palm" + i, new FloatProcessor((int) (rect.width * Prefs.palmRatio_), (int) (rect.height * Prefs.palmRatio_)));
		}
		ImagePlus img = new ImagePlus("PALM",stack);

		double psdWidth0 = Prefs.palmPSDWidth_ * Prefs.palmRatio_;
		Vector<Trajectory> trajs = dataset_.getTrajectories();
		for ( int i = 0; i < trajs.size(); i ++) {
			Trajectory traj = trajs.get(i);
			if ( ! traj.isDisabled() ) {
				for (int j = 0; j < traj.size(); j++ ) {
					double xx = traj.getX(j);
					double yy = traj.getY(j);
					int frame = traj.getFrame(j);
					//IJ.log("plotting" + xx + "," + yy + "," + frame);
					double xs = (xx - rect.x) * Prefs.palmRatio_;
					double ys = (yy - rect.y) * Prefs.palmRatio_;
					FloatProcessor ip = (FloatProcessor)stack.getProcessor(frame);
					double psdWidth = psdWidth0 * Math.sqrt(traj.getQuality());
					for (int x = Math.max(0, (int)(xs - 3*psdWidth)); x < Math.min(ip.getWidth(), (int)(xs + 3*psdWidth)); x ++) {
						for (int y = Math.max(0, (int)(ys - 3*psdWidth)); y < Math.min(ip.getHeight(), (int)(ys + 3*psdWidth)); y++) {
							double v = Math.exp( -((x-xs) * (x-xs) + (y-ys)*(y-ys))/(2.0*psdWidth*psdWidth) ) / psdWidth;
							ip.setf(x, y, (float)v + ip.getf(x,y));
						}
					}

				}
			}
		}
		img.show();
	}
	
	FloatProcessor gaussianImage(ImageProcessor img) {
		int width = img.getWidth();
		int height = img.getHeight();
		double psdWidth = Prefs.palmPSDWidth_ * Prefs.palmRatio_;
		FloatProcessor ip = new FloatProcessor(width, height);
		for (int x = 0; x < width; x ++) {
			for (int y = 0; y < height; y++) {
				double v = Math.exp( -((x*2-width) * (x*2-width) + (y*2-height)*(y*2-height))/(8.0*psdWidth*psdWidth) );
				ip.setf(x, y, (float)v);
			}
		}

		return ip;
	}

	void subStack() {
		int index = trajList_.convertRowIndexToModel(trajList_.getSelectedRow());
		Trajectory v = dataset_.getTrajectories().get(index);

		try{
			int width = roi_ != null? roi_.getBounds().width : imp_.getWidth();
			int height = roi_ != null? roi_.getBounds().height : imp_.getHeight();

			ImageStack stack = imp_.getStack();
			ImageStack stack2 = new ImageStack(width, height, imp_.getProcessor().getColorModel());

			for (int i=v.getFrame(0); i<=v.getFrame(v.size()-1); i++) {
				ImageProcessor ip2 = stack.getProcessor(i);
				ip2.setRoi(roi_);
				stack2.addSlice(stack.getSliceLabel(i), ip2.crop());
			}

			ImagePlus impSubstack = imp_.createImagePlus();
			impSubstack.setStack("Substack", stack2);
			impSubstack.setCalibration(imp_.getCalibration());
			impSubstack.show();
		}
		catch(IllegalArgumentException e){
			IJ.error("Substack error." + e.toString());
		}
	}

	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// who cares
	}

	@Override
	public void dispose() {
		if (! notesSaved_ ) {
			dataset_.saveNotes();
			notesSaved_ = true;
		}
		super.dispose();
	}
}
