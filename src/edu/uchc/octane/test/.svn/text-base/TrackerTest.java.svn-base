package edu.uchc.octane.test;
import java.io.File;
import java.io.IOException;

import edu.uchc.octane.Tracker;



public class TrackerTest {
	public static void main(String [] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Gimma a file name!");
			System.exit(0);
		}
		
		File f = new File(args[0]);
		
		Tracker tracker = new Tracker(f, 1, 5);
		
		tracker.doTracking();
		tracker.toDisk(new File(args[0] + ".tracked"));
	}
}
