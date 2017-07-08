package milestone2;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import de.dennis_boldt.RXTX;

/**
 * This is a simple Photosensor that is read by the Arduino and the Arduino 
 * send it by serial Port.
 * @author Sven Andresen
 *
 */
public class PhotoSensor extends Thread implements Observer {

	private RXTX rxtx;
	private String buffer = "";
	private List<LuxValueListener> listeners;

	public PhotoSensor(int baudrate) {
		listeners = new ArrayList<LuxValueListener>();
		rxtx = new RXTX(baudrate);
	}

	public void run() {
		try {
			//start RXTX with null as USBport to find it on its own
			rxtx.start(null, "/usr/lib/jni", this);
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
	public void update(Observable o, Object arg) {
		if(arg instanceof byte[]) {
			try {
				byte[] array = (byte[]) arg;
				//parse to string
				String arrayString = new String(array);
				//append to buffer
				buffer += arrayString;
				//find first occurence of Delimeter
				String xbuffer = buffer.substring(buffer.indexOf('$') + 1);
				if(xbuffer.contains("$")) { //If we find another Delimeter we have a full message
					xbuffer = xbuffer.substring(0, xbuffer.indexOf('$'));
				} else
					return;
				buffer = ""; // clear the buffer
				notifyListeners(Double.parseDouble(xbuffer));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add a Listener.
	 * @param listener
	 */
	public void addListener(LuxValueListener listener) {
		this.listeners.add(listener);
	}
	
	/**
	 * Remove a Listener.
	 * @param listener
	 */
	public void removeListener(LuxValueListener listener) {
		this.listeners.remove(listener);
	}
	
	/**
	 * Notify all the Listeners.
	 * @param newValue
	 */
	private void notifyListeners(double newValue) {
		for(LuxValueListener listener : listeners) {
			listener.updateLuxValue(newValue);
		}
	}
}
