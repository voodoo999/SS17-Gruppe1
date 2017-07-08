package milestone2;

/**
 * Interface that will implment a simple listener for the Lux Value.
 * @author Sven Andresen
 *
 */
public interface LuxValueListener {
	
	/**
	 * Indicate that a new Lux Value has been read.
	 * @param newValue
	 */
	void updateLuxValue(double newValue);
}
