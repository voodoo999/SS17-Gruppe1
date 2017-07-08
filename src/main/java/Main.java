import milestone1.Milestone1;
import milestone1.Milestone1Arduino;
import milestone2.Milestone2;
import milestone3.Milestone3;
import milestone4.Milestone4;

/**
 * Main class to be started at execution.
 * @author Sven Andresen
 *
 */
public class Main {
	/**
	 * Main function
	 * @param args a list of parameter that decides what milestone to start
	 */
	public static void main(String[] args) {
		if(args.length == 0) {
			showHelp();
			System.exit(0);
		}
		// configure logging
		try {
			LoggingConfiguration.configureDefaultLogging();
		} catch (Exception e1) {
			System.out.println("Unable to initialize logging");
			e1.printStackTrace();
			System.out.println("Exiting...");
			System.exit(-1);
		}
		if(args[0].equals("1")) {
			if(args.length > 1 && args[1].equals("--PiOnly"))
				new Milestone1();
			else {
				new Milestone1Arduino();
			}
		} else if(args[0].equals("2")){
			new Milestone2();
		} else if(args[0].equals("3")) {
			new Milestone3();
		} else if(args[0].equals("4")) {
			new Milestone4();
		} else {
			System.out.println("Unknown Parameter: " + args[0]);
			showHelp();
			System.exit(0);
		}
	}

	/**
	 * Shows help by wrong usage
	 */
	private static void showHelp() {
		System.out.println("------- USAGE -------");
		System.out.println("For Milestone 1 set first Argument to: 1");
		System.out.println("\t if run only on PI add argument: --PiOnly");
		System.out.println("For Milestone 2 set first Argument to: 2");
		System.out.println("for Milestone 3 set first argument to: 3");
		System.out.println("For Milestone 4 set first argument to: 4");
	}
}
