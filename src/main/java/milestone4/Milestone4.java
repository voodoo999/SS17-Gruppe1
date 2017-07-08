package milestone4;

import java.net.URISyntaxException;
import org.apache.log4j.Logger;

import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import milestone1.Milestone1Arduino;
import milestone2.LEDActor;
import milestone2.LuxRessource;
import milestone2.SSPServer;
import milestone2.TimeRessource;
import milestone3.AirConditioner;
import milestone3.Arduino;
import milestone3.FanActor;
import milestone3.HumidityRessource;
import milestone3.Milestone3;
import milestone3.TemperatureRessource;

/**
 * Milestone 4 will connect to an SSP and send all the information of the sensors.
 * Also this will start an air conditioner as is in {@link Milestone3}. What
 * is new that we also ask other groups for theire values in a SPARQL Query and
 * depending onthat information turn the AC on or off or load Profiles.
 * @author Sven Andresen
 *
 */
public class Milestone4 {
	
    private static Logger LOG = Logger.getLogger(Milestone4.class.getName());
	
	private LuxRessource luxRessource;
	private TimeRessource timeRessource;
	private Arduino arduino;
	private AirConditioner ac;

	private SPARQLConnector connector;
	private LEDActor led;
	private TemperatureRessource tempRessource;
	private HumidityRessource humidRessource;
	private FanActor fanActor;
	
	/**
	 * This will initiate the Milestone 4. Starting with a simple connection to the SSP we will
	 * bound Ressources to the SSP that will send the time and will send the Lux Value as
	 * well as other values read by the Arduino and passed to the PI by a serial connection
	 * (also see: {@link Milestone1Arduino}). Also we will ask the SSP for data so that we
	 * can decide between who is in the room and set the temperature accordingly.
	 */
	public Milestone4() {
		//set up arduino and start capturing
		arduino = new Arduino(9600);
		arduino.start();
		//set up Sensor and start capturing
//		sensor = new PhotoSensor(9600);
//		sensor.start();
		//set up SPARQL Connector
		connector = new SPARQLConnector();
		connector.start();
		//set up LED
		led = new LEDActor();
		connector.registerAtSPARQLConnector(led);
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
		connector.registerAtSPARQLConnector(ac);
		LOG.info("Registering at SSP");
		try {
			server.registerAtSSP();
		} catch (URISyntaxException e1) {
			LOG.error(e1);
		} 
	}
}
