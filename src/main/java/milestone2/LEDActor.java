package milestone2;

import org.apache.log4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import milestone4.SPARQLInterface;

/**
 * Class that handles the LED on a specific GPIO on the PI.
 * @author Sven Andresen
 *
 */
public class LEDActor extends Thread implements SparqlListener, SPARQLInterface {
	private static Logger LOG = Logger.getLogger(LEDActor.class.getName());

	private GpioController gpio;
	private GpioPinDigitalOutput pinLed;
	private String sparql = "PREFIX pit: <https://pit.itm.uni-luebeck.de/>\n"+
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"+
			"\n"+
			"SELECT (AVG(xsd:float(?x)) AS ?lux) WHERE {\n"+
			"?comp pit:isType \"LDR\"^^xsd:string.\n"+
			"?comp pit:hasStatus ?status.\n"+
			"?status pit:hasScaleUnit \"Lux\"^^xsd:string.\n"+
			"?status pit:hasValue ?x\n"+
			"}\n";
	
	private double averageValue = 10000.0;
	
	/**
	 * Create a new instance of the LED Actor that will init the LED on GPIO 29.
	 */
	public LEDActor() {
		gpio = GpioFactory.getInstance();
		// init LED
		pinLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29, "MyLed", PinState.LOW);
		pinLed.setShutdownOptions(true, PinState.LOW);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		LOG.info("Starting LED Actor");
		while(true) {
			if(averageValue < 200) {
				pinLed.high();
			} else
				pinLed.low();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOG.error(e);
			}
		}
		
	}
	
	public void updateSparqlListener(double newValue) {
		averageValue = newValue;
	}

	@Override
	public String getQuery() {
		return sparql;
	}

	@Override
	public String getRegex() {
		return "\\d+[.\\d+]*";
	}

	@Override
	public void updateSPARQLValue(String newValue) {
		try {
			averageValue = Double.parseDouble(newValue);
		} catch (NumberFormatException e) {
			LOG.error(newValue + " can not be parsed!", e);
		}
	}
}
