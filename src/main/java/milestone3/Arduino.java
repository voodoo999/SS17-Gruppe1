package milestone3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.dennis_boldt.RXTX;

/**
 * Implements a connection to an Arduino.
 * @author Sven Andresen
 *
 */
public class Arduino extends Thread implements Observer {
    private static Logger LOG = Logger.getLogger(Arduino.class.getName());
	private RXTX rxtx;
	private String buffer = "";
	private List<ArduinoListener> listeners;
	
	protected String regex = "\\w+:(\\d*.\\d+|\\w+)";
	protected Pattern pattern = Pattern.compile(regex);
	protected String port;
	
	/**
	 * Creates a new instance of the Arduino on the specified baudrate.
	 * @param baudrate
	 */
	public Arduino(int baudrate, String port) {
		listeners = new ArrayList<ArduinoListener>();
		rxtx = new RXTX(baudrate);
		this.port = port;
	}
	
	public Arduino(int baudrate) {
		listeners = new ArrayList<ArduinoListener>();
		rxtx = new RXTX(baudrate);
		this.port = null;
	}
	
	public void run() {
		try {
			//start RXTX with null as USBport to find it on its own
			rxtx.start(port, "/usr/lib/jni", this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		while(true)
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	/**
	 * On update we will read the message in the serial interface and check wether we get a message that
	 * is seperated with two delimeters($). 
	 */
	@Override
	public void update(Observable o, Object arg) {
		if(arg instanceof byte[]) {
			try {
				byte[] array = (byte[]) arg;
				//parse to string
				String arrayString = new String(array);
				//append to buffer
				buffer += arrayString;
				if(buffer.length() <= 0 || buffer.indexOf('$') == -1)
					return;
				String help = buffer.substring(buffer.lastIndexOf('$'));
				buffer = buffer.substring(buffer.indexOf('$'), buffer.lastIndexOf('$'));
				Matcher matcher = pattern.matcher(buffer);
				while(matcher.find()) {
					notifyListeners(matcher.group(0));
//					for (int i = 1; i <= matcher.groupCount(); i++) {
//				        notifyListeners(matcher.group(i));
//				    }
				}
				buffer = help; // clear the buffer to last entry
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Add a Listener.
	 * @param listener
	 */
	public void addListener(ArduinoListener listener) {
		this.listeners.add(listener);
	}
	
	/**
	 * Remove a Listener.
	 * @param listener
	 */
	public void removeListener(ArduinoListener listener) {
		this.listeners.remove(listener);
	}
	
	/**
	 * Notify all the Listeners.
	 * @param newValue
	 */
	private void notifyListeners(String newValue) {
		for(ArduinoListener listener : listeners) {
			listener.updateArduino(newValue);
		}
	}
	
	/**
	 * Attempts to write a String to an Arduino. 
	 * The string given should be without Delimeter ($)!
	 * @param value the string to be written.
	 * @throws IllegalArgumentException if the argument contains delimeter
	 */
	public void writeToArduino(String value) throws IllegalArgumentException {
		if(value.contains("$")) 
			throw new IllegalArgumentException("The string: \n" + value + "\ncontains delimeter: $");
		//concat delimeter
		value = value + "$";
		try {
			rxtx.write(value.getBytes());
		} catch (IOException e) {
			LOG.error(e);
		}
	}
}
