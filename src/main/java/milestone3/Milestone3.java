package milestone3;

import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import milestone2.LEDActor;
import milestone2.LUXConnector;
import milestone2.LuxRessource;
import milestone2.SSPServer;
import milestone2.TimeRessource;

/**
 * Milestone 3 will connect to an SSP and send all the information of the sensors.
 * Also this will start an air conditioner.
 * @author Sven Andresen
 *
 */
public class Milestone3 {
	
    private static Logger LOG = Logger.getLogger(Milestone3.class.getName());
	
	private LuxRessource luxRessource;
	private TimeRessource timeRessource;
	private Arduino arduino;
	private AirConditioner ac;

	private LUXConnector connector;
	private LEDActor led;
	private TemperatureRessource tempRessource;
	private HumidityRessource humidRessource;
	private FanActor fanActor;
	
	/**
	 * This will initiate the Milestone 3. Starting with a simple connection to the SSP we will
	 * bound Ressources to the SSP that will send the time and will send the Lux Value read by
	 * the Arduino and passed to the PI by a serial connection (also see: {@link milestone1.Milestone1Arduino})
	 */
	public Milestone3() {
		//set up arduino and start capturing
		arduino = new Arduino(9600);
		arduino.start();
		//set up Sensor and start capturing
//		sensor = new PhotoSensor(9600);
//		sensor.start();
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
		arduino.addListener(luxRessource);
		server.registerRessource(luxRessource);
		timeRessource = new TimeRessource("/utc-time", 1, server.getExecutor());
		server.registerRessource(timeRessource);
		tempRessource = new TemperatureRessource("/temp", 5, server.getExecutor());
		arduino.addListener(tempRessource);
		server.registerRessource(tempRessource);
		humidRessource = new HumidityRessource("/humid", 5, server.getExecutor());
		arduino.addListener(humidRessource);
		server.registerRessource(humidRessource);
		fanActor = new FanActor("/fan", 5, server.getExecutor());
		arduino.addListener(fanActor);
		server.registerRessource(fanActor);
		ac = new AirConditioner(arduino, 23.0);
		arduino.addListener(ac);
		ac.start();
		LOG.info("Registering at SSP");
		try {
			server.registerAtSSP();
		} catch (URISyntaxException e1) {
			LOG.error(e1);
		} 
	}
}
