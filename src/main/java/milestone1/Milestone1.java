package milestone1;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * This class implements the basic toggle of an LED when a 
 * specific Threshold on an input is passed.
 * @author Sven Andresen
 *
 */
public class Milestone1 {

	private GpioController gpio;
	private GpioPinDigitalOutput pinLed;
	private GpioPin pinSensor;

	/**
	 * This will toggle the LED on GPIO 29 when the
	 * input on GPIO 27 changes.
	 */
	public Milestone1() {
		System.out.println("Milestone 1 running...");
		gpio = GpioFactory.getInstance();
		//init led and sensor
		pinLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29, "MyLed", PinState.LOW);
		pinLed.setShutdownOptions(true, PinState.LOW);
		pinSensor  = gpio.provisionDigitalInputPin(RaspiPin.GPIO_27, PinPullResistance.PULL_DOWN);
		pinSensor.setShutdownOptions(true);
		//add listener if input changes
		pinSensor.addListener(new GpioPinListenerDigital() {
			
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent arg0) {
				if(arg0.getState() == PinState.HIGH)
					pinLed.high();
				else
					pinLed.low();
			}
		});
			
		while(true)
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				
			}
	}
}
