package milestone3;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import milestone4.SPARQLInterface;

/**
 * Class of an Air Conditioner. This is extending Thread and once its 
 * started we will update the Arduino Display and the Fans PWM.
 * @author Sven Andresen
 *
 */
public class AirConditioner extends Thread implements ArduinoListener, SPARQLInterface {
    private static Logger LOG = Logger.getLogger(AirConditioner.class.getName());
    
    protected String query = "PREFIX pit: <https://pit.itm.uni-luebeck.de/>\n"+
    						 "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"+
    						 "SELECT ?x WHERE {\n" +
    						 "?comp pit:isType \"Rfid\"^^xsd:string.\n" +
    						 "?comp pit:hasStatus ?status.\n" +
    						 "?status pit:hasScaleUnit \"Name\".\n" +
    						 "?status pit:hasValue ?x\n"+
    						 "}\n";
	protected double lastReadTempValue = 0.0;
	protected double threshold = 25.0;
	protected int lastReadFanValue;
	protected Arduino arduino;
	protected double lastReadHumidValue;
	protected int fanSpeed = 0;
	protected File database = new File("database.txt");
	private boolean acRunning;

	private String activeGroup;
	
	/**
	 * Constructs a new Air Conditioner. We need the connection to 
	 * the Arduino and also an initial threshold.
	 * @param arduino the arduino instance to write to (serial connection)
	 * @param threshold the initial Threshold (desired temp)
	 */
	public AirConditioner(Arduino arduino, double threshold) {
		this.arduino = arduino;
		this.threshold = threshold;
		try {
			database.createNewFile();
		} catch (IOException e) {
			LOG.error("Can not create File \"database.txt\"", e);
		}
	}

	@Override
	public void run() {
		while(true) {
			if(acRunning) {
				if(lastReadTempValue < threshold) {
					fanSpeed = 0;			
				} else if(lastReadTempValue < (threshold + 0.1))
					fanSpeed = 1;			
				else if(lastReadTempValue < (threshold + 0.2))
					fanSpeed = 2;
				else if(lastReadTempValue < (threshold + 0.3))
					fanSpeed = 3;
				else if(lastReadTempValue < (threshold + 0.4))
					fanSpeed = 4;
				else if(lastReadTempValue < (threshold + 0.5))
					fanSpeed = 5;
				else if(lastReadTempValue < (threshold + 0.6))
					fanSpeed = 6;
				else if(lastReadTempValue < (threshold + 0.7))
					fanSpeed = 7;
				else if(lastReadTempValue < (threshold + 0.8))
					fanSpeed = 8;
				else //full power
					fanSpeed = 9;
			}
			else 
				fanSpeed = 0;
			updateArduino();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Write Information to the Arduino.
	 */
	protected void updateArduino() {
		arduino.writeToArduino(String.format("PWM:%d", fanSpeed));
		if(acRunning)
			arduino.writeToArduino(String.format("DISPLAY2:%2.2f@%d@%d", threshold, fanSpeed, lastReadFanValue));
		else 
			arduino.writeToArduino(String.format("DISPLAY2:%5s@%d@%d", "OFF", fanSpeed, lastReadFanValue));
		arduino.writeToArduino(String.format("DISPLAY1:%2.2f C @ %.2f%%", lastReadTempValue, lastReadHumidValue));
	}

	@Override
	public void updateArduino(String newValue) {
		if(newValue.contains("TEMP:")) {
			this.lastReadTempValue = Double.parseDouble(newValue.substring(newValue.indexOf(":") + 1));
		} else if(newValue.contains("RPM:")) {
			this.lastReadFanValue = Integer.parseInt(newValue.substring(newValue.indexOf(":") + 1));
		} else if(newValue.contains("HUMIDITY:")) {
			this.lastReadHumidValue = Double.parseDouble(newValue.substring(newValue.indexOf(":") + 1 ));
		} else if(newValue.contains("BUTTONUP:")) {
			if(newValue.substring(newValue.indexOf(":") + 1 ).equals("ON"))
				if(acRunning) {
					threshold = threshold + 0.5;
					updateNewThresholdForGroup();
				}
		} else if(newValue.contains("BUTTONDOWN:")) {
			if(newValue.substring(newValue.indexOf(":") + 1 ).equals("ON"))
				if(acRunning) {
					threshold = threshold - 0.5;
					updateNewThresholdForGroup();
				}
		}
	}

	@Override
	public String getQuery() {
		return query;
	}

	@Override
	public String getRegex() {
		return "Gruppe\\d";
	}

	@Override
	public void updateSPARQLValue(String newValue) {
		if(newValue == null) {
			threshold = 25.0;
			activeGroup = null;
			acRunning = false;
		}
		else
			handleSparqlResponse(newValue);
	}
	
	/**
	 * Handle the reponse of the SPARQL Query and look into the
	 * database file wether the group that entered the room exists.
	 * If the group is found in the database we will start the AC
	 * and set it to the desired temperature otherwise if that group
	 * is not yet registered in the database we will set the intial
	 * temperatur to 21 degrees. If no group is in the room then
	 * the ac will be turned off.
	 * @param groupname
	 */
	protected void handleSparqlResponse(String groupname) {
		System.out.println(groupname);
		if(groupname.equals(this.activeGroup))
			return; //nothing to do since we already have this group logged in.
		this.activeGroup = groupname;
		if(groupname == null || groupname.equals("0")) {
			acRunning = false;
			return;
		}
		try {
			List<String> lines = FileUtils.readLines(database, "UTF-8");
			boolean found = false;
			for(String line : lines) {
				String[] s = line.split(":");
				if(s[0].trim().equals(groupname.trim())) {
					threshold = Double.parseDouble(s[1].trim());
					found = true;
					break;
				}
			}
			if(!found) {
				threshold = 21.0;
				FileUtils.writeStringToFile(database, groupname + ":" + threshold, "UTF-8", true);
			}
			acRunning = true;
		} catch (IOException e) {
			LOG.error("Can not get the File with saved Information", e);
			return;
		}
	}

	/**
	 * Update a new Threshold for an active group.
	 */
	protected void updateNewThresholdForGroup() {
		if(!acRunning) //abort if no group is there
			return;
		try {
			List<String> lines = FileUtils.readLines(database, "UTF-8");
			String newValue = "";
			int i = 0;
			for(String line : lines) {
				String[] s = line.split(":");
				if(s[0].trim().equals(activeGroup)) {
					s[1] = Double.toString(threshold);
					newValue= s[0] + ":" + s[1];
					break;
				}
				i++;
			}
			lines.remove(i);
			lines.add(newValue);
			FileUtils.writeLines(database, lines);
		} catch (IOException e) {
			LOG.error("Can not write to Database!", e);
		}
	}
}
