package milestone2;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;

import de.dennis_boldt.RXTX;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;

/**
 * Milestone 2 will connect to an SSP and send all the information of the sensors.
 * @author Sven Andresen
 *
 */
public class Milestone2 {
	
    private static Logger LOG = Logger.getLogger(Milestone2.class.getName());
	
	private LuxRessource luxRessource;
	private TimeRessource timeRessource;
	private PhotoSensor sensor;

	private LUXConnector connector;

	private LEDActor led;
	
	/**
	 * This will initiate the Milestone 2. Starting with a simple connection to the SSP we will
	 * bound Ressources to the SSP that will send the time and will send the Lux Value read by
	 * the Arduino and passed to the PI by a serial connection (also see: {@link milestone1.Milestone1Arduino})
	 */
	public Milestone2() {
		//set up Sensor and start capturing
		sensor = new PhotoSensor(9600);
		sensor.start();
		//set up SPARQL Connector
		connector = new LUXConnector();
		connector.start();
		//set up LED
		led = new LEDActor();
		connector.registerSparqlListener(led);
		led.start();
		//set up CoAP
		SSPServer server = new SSPServer(BlockSize.SIZE_64, BlockSize.SIZE_64);
		luxRessource = new LuxRessource("/lux", 5, server.getExecutor());
		sensor.addListener(luxRessource);
		server.registerRessource(luxRessource);
		timeRessource = new TimeRessource("/utc-time", 1, server.getExecutor());
		server.registerRessource(timeRessource);
		LOG.info("Registering at SSP");
		try {
			server.registerAtSSP();
		} catch (URISyntaxException e1) {
			LOG.error(e1);
		} 
	}
}
