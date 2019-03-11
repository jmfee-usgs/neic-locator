package gov.usgs.locator;

import java.io.IOException;

import gov.usgs.traveltime.*;

/**
 * Test driver for the locator.
 *
 * @author Ray Buland
 *
 */
public class LocMain {

	/**
	 * Main program for testing the locator.
	 * 
	 * @param args Command line arguments (not used)
	 */
	public static void main(String[] args) {

		if (args == null || args.length == 0) {
			System.out
					.println("Usage: neic-locator" + 
						" <modelPath> <eventPath>");
		}

		// Set up the earth model.
		String earthModel = "ak135";
		// Set up the earthquake file.
//	String eventID = "Baja_1";
		String eventID = "1000010563_23";
		// Objects we'll need.

		// get model path
		String modelPath = null;
		if (args != null && args.length >= 1) {
			modelPath = args[0];
		}

		// get event path
		String eventPath = null;
		if (args != null && args.length >= 2) {
			eventPath = args[1];
		}		

		LocInput in = null;
		LocOutput out = null;
		AuxLocRef auxLoc = null;

		Event event = null;
		Locate loc;
		LocStatus status = null;
		TTSessionLocal ttLocal = null;
		
		// Set the debug level.
		LocUtil.deBugLevel = 1;
		
		// If travel times are local, set up the manager.
		if(!LocUtil.server) {
			try {
				ttLocal = new TTSessionLocal(true, true, true, modelPath);
			} catch (IOException e) {
				System.out.println("Unable to read travel-time auxiliary data.");
				e.printStackTrace();
				System.exit(LocStatus.BAD_READ_AUX_DATA.status());
			}
		}
		
		// Read the Locator auxiliary files.
		try {
			auxLoc = new AuxLocRef(modelPath);
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Unable to read Locator auxiliary data.");
			e.printStackTrace();
			System.exit(LocStatus.BAD_READ_AUX_DATA.status());
		}
		
		// If server, get external event input here.
		if(LocUtil.server) {
			in = new LocInput();
			earthModel = in.getModel();
		}
		
		// Set up the event.
		event = new Event(earthModel);
		if(LocUtil.server) {
			// In server mode, use what we've already read in.
			event.serverIn(in);
		} else {
			// In local mode, read a Hydra style event input file.
			if(event.readHydra(eventID, eventPath)) {
				if(LocUtil.deBugLevel > 3) event.printIn();

			} else {
				System.out.println("Unable to read event.");
				System.exit(LocStatus.BAD_EVENT_INPUT.status());
			}
		}
		
		// Do the location.
		loc = new Locate(event, ttLocal, auxLoc);
		status = loc.doLoc();
		event.setExitCode(status);
		
		// Wrap up.
		if(LocUtil.server) {
			out = event.serverOut();		// JSON output
			out.printNEIC();
		} else {
			event.printHydra();
//		event.printNEIC();
		}
		
		// Exit.
		System.exit(event.exitCode);

	}
}