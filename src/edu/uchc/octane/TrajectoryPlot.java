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
	
	private Element generatePathElement(Document doc, Trajectory t)
	{
		Element path = doc.createElement("path");
		
		String d = "M";
		//d = String.format("M %.3f %.3f", t.get(0).x, t.get(0).y);
		for (int i = 0; i < t.size(); i ++) {
			
			SmNode node = t.get(i);
			d += String.format("%.3f %.3f ", node.x, node.y);
		}
		
		path.setAttribute("d", d);

		
		path.setAttribute("fill", "none");
		path.setAttribute("stroke", "black");
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
	 * Generate svg.
	 *
	 * @param selected the selected
	 */
	public void generateSVG (int [] selected, Rectangle rect, File file) {
		
		try {
			DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = df.newDocumentBuilder();

			Document doc = db.newDocument();
			Element root = doc.createElement("svg");
			doc.appendChild(root);
			
			rect_ = rect;

			root.setAttribute("viewbox", String.format("%i %i %i %i", 
					rect_.x, rect_.y, rect_.width, rect_.height));
			root.setAttribute("xmlns", "http://www.w3.org/2000/svg");
					
			for (int i = 0; i < selected.length; i++) {
				
				Trajectory traj = dataset_.getTrajectoryByIndex(selected[i]);
				
				root.appendChild(generatePathElement(doc, traj));
			}

			writeSVGToFile(doc, file);

		} catch (ParserConfigurationException pce) {
			IJ.error(pce.getLocalizedMessage());
		} catch (TransformerException te) {
			IJ.error(te.getLocalizedMessage());
		}
	}

}
