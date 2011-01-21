package edu.uchc.octane.test;

import edu.uchc.octane.Browser;
import edu.uchc.octane.TrajDataset;

public class BrowserTest {

	public static void main(String[] args) {
		String p= "C:\\Users\\Ji-Yu\\workspace\\Octane\\testdata\\eosactin-DIV8_1";
		TrajDataset data = new TrajDataset(p);
		Browser b = new Browser(data);
		b.setVisible(true);
	}
}
