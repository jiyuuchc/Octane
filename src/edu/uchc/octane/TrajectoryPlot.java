//FILE:          TrajectoryPlot.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 1/6/13
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
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package edu.uchc.octane;

import ij.IJ;

import java.awt.Rectangle;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The Class TrajectoryPlot.
 *
 * @author Ji-Yu
 */

public class TrajectoryPlot {

	final private static String [] colors_ = new String[] {"black", "#2A4BD7", "#AD2323", "#FF9233"};
	
	OctaneWindowControl ctr_;
	
	TrajDataset dataset_;
	
	Rectangle rect_;
	
	/**
	 * Instantiates a new trajectory plot.
	 *
	 * @param dataset the dataset
	 */
	public TrajectoryPlot (TrajDataset data) 
	{
		dataset_ = data;
	}
	
	private Element generatePathElement(Document doc, Trajectory t, String color)
	{
		Element path = doc.createElement("path");
		
		String d = "M";
		//d = String.format("M %.3f %.3f", t.get(0).x, t.get(0).y);
		for (int i = 0; i < t.size(); i ++) {
			
			SmNode node = t.get(i);
			d += String.format("%.3f %.3f ", node.x - rect_.x, node.y - rect_.y);
		}

		path.setAttribute("d", d);

		
		path.setAttribute("fill", "none");
		path.setAttribute("stroke", color);
		path.setAttribute("stroke-width", "0.2");
		
		return path;
	}
	
	private void writeSVGToFile(Document doc, File file) throws TransformerException
	{
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		
		transformer = tf.newTransformer();
		
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		
		transformer.transform(source, result);
	}
	
	/**
	 * Generate svg with alternating color scheme.
	 *
	 * @param rect the selected ROI 
	 * @param file the output file 
	 */
	public void generateSVG (Rectangle rect, File file) {
		
		generateSVG(rect, file, true);
		
	}

	/**
	 * Generate svg.
	 *
	 * @param rect the selected ROI 
	 * @param file the output file
	 * @param bAlternatingColor whether to alternate stroke color for different trajectories 
	 */
	public void generateSVG (Rectangle rect, File file, boolean bAlternatingColor) {
		
		try {
			DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = df.newDocumentBuilder();

			Document doc = db.newDocument();
			Element root = doc.createElement("svg");
			doc.appendChild(root);
			
			rect_ = rect;

			root.setAttribute("width", String.format("%d", rect_.width));
			root.setAttribute("height", String.format("%d", rect_.height));
			root.setAttribute("xmlns", "http://www.w3.org/2000/svg");
			
			int colorIdx = 0;

			for (int i = 0; i < dataset_.getSize(); i ++) {
				
				Trajectory traj = dataset_.getTrajectoryByIndex(i);
				
				if ( traj.marked ) {

					root.appendChild(generatePathElement(doc, traj, colors_[colorIdx]));

					if (bAlternatingColor) {

						colorIdx ++;
						if (colorIdx == colors_.length) {
							colorIdx = 0;
						}
					}
				}
				
				IJ.showProgress(i, dataset_.getSize());
			}

			writeSVGToFile(doc, file);

			IJ.showProgress(1.0);
			
		} catch (ParserConfigurationException pce) {
			IJ.error(pce.getLocalizedMessage());
		} catch (TransformerException te) {
			IJ.error(te.getLocalizedMessage());
		}
	}

}
