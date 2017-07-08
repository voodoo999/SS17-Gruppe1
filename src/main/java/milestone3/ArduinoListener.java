package milestone3;

/**
 * A simple Listener Interface to connect to the Arduino.
 * @author Sven Andresen
 *
 */
public interface ArduinoListener {
	
	/**
	 * If the Arduino has an update an read any Strings it will appear as newValue 
	 * in this method. Every value is seperated by a ':' (eg. <code>TEMP:24.30
	 * </code>). Everyone how wants to listen to a specific value or sensor should 
	 * implement this interface and should be registered in the {@link Arduino} as
	 * a listener.
	 * @param newValue the new read sensor value (eg.<code>TEMP:24.30</code>).
	 */
	public abstract void updateArduino(String newValue);
}
