package milestone1;
import java.util.Observable;
import java.util.Observer;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import de.dennis_boldt.RXTX;

/**
 * The Milestone 1 Exercise with the Arduino. This Class will handle a simple
 * incoming Serial input from the Arduino and will based on the input toogle 
 * and LED on GPIO 29.
 * @author Sven Andresen
 *
 */
public class Milestone1Arduino implements Observer {

	private GpioController gpio;
	private GpioPinDigitalOutput pinLed;

	/**
	 * Milestone 1
	 * Use the aduino to get the digital sensor input.
	 */
	public Milestone1Arduino() {
		System.out.println("Milestone 1 running Arduino...");
		gpio = GpioFactory.getInstance();
		// init LED
		pinLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29, "MyLed", PinState.LOW);
		pinLed.setShutdownOptions(true, PinState.LOW);
		//init RXTX
		RXTX rxtx;
		
		rxtx = new RXTX(9600);
		
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

	private String buffer = "";
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
				System.out.println(xbuffer); //output the message from the serial Port
				if(xbuffer.length() != 0 && Integer.parseInt(xbuffer) > 500) {//if greater than 500 turn led on
					pinLed.high();
				} else
					pinLed.low();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
