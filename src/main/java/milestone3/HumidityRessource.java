package milestone3;

import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.CT;
import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.IF;
import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.RT;
import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.SZ;
import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.TITLE;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.SettableFuture;

import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import milestone2.LuxRessource;
import milestone2.Ressource;

/**
 * This will represent a Ressource that handles and updates the humidity value.
 * @author Sven Andresen
 *
 */
public class HumidityRessource extends Ressource<Long> implements ArduinoListener {

	public static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    private static Logger LOG = Logger.getLogger(LuxRessource.class.getName());

    private static HashMap<Long, String> payloadTemplates = new HashMap<Long, String>();
    static{
        //Add template for plaintext UTF-8 payload
        payloadTemplates.put(
                ContentFormat.TEXT_PLAIN_UTF8,
                "The current Humidity is %.2f."
        );

        //Add template for XML payload
        payloadTemplates.put(
                ContentFormat.APP_XML,
                "<humid>%.2f</humid>"
        );
        
        payloadTemplates.put(
	        ContentFormat.APP_TURTLE,
	        "@prefix itm: <http://gruppe01.pit.itm.uni-luebeck.de/>\n" +
	        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
	        "@prefix rds: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
	        "@prefix pit: <https://pit.itm.uni-luebeck.de/>" +
	        "\n" + 
	        "itm:ourPi pit:hasLabel \"SuperPi\"^^xsd:string .\n" +
	        "itm:ourPi pit:hasGroup \"PIT_01-SS17\"^^xsd:string .\n" +
	        "itm:ourPi pit:hasIP \"141.83.175.252\"^^xsd:string .\n" +
	        "itm:ourPi pit:hasComponent itm:humidSensor .\n" +
	        "itm:humidSensor rds:type pit:Component .\n" +
	        "itm:humidSensor pit:isType \"HUMID\"^^xsd:string .\n" +
	        "itm:humidSensor pit:isActor \"false\"^^xsd:boolean .\n" +
	        "itm:humidSensor pit:hasDescription \"Sensor that gets the current Humdity value\"^^xsd:string .\n" +
	        "itm:humidSensor pit:hasURL \"coap://141.83.175.252:5683/humid\"^^xsd:anyURI .\n" +
	        "itm:humidSensor pit:hasStatus itm:humidSensorState .\n" +
	        "itm:humidSensorState pit:hasValue \"%.2f\"^^xsd:string .\n" +
	        "itm:humidSensor pit:lastModified \"%s\"^^xsd:dateTime .\n" +
	        "itm:humidSensorState pit:hasScaleUnit \"Percent\"^^xsd:string .\n"
        );        
    }

    private ScheduledFuture<?> periodicUpdateFuture;
    private int updateInterval;
    
    private double lastReadHumidValue = -1.0; //as default indicating none has been read yet

    // This is to handle whether update requests are confirmable or not (remoteSocket -> MessageType)
    private HashMap<InetSocketAddress, Integer> observations = new HashMap<InetSocketAddress, Integer>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/**
	 * Create a Humidity Ressource.
	 * @param path
	 * @param updateInterval
	 * @param executor
	 */
	public HumidityRessource(String path, int updateInterval, ScheduledExecutorService executor) {
		super(path, System.currentTimeMillis(), executor);

        //Set the update interval, i.e. the frequency of resource updates
        this.updateInterval = updateInterval;
        schedulePeriodicResourceUpdate();

        Set<Long> keys = payloadTemplates.keySet();
        Long[] array = keys.toArray(new Long[keys.size()]);
        
        // Convert to "1 3 45"
        String[] values = new String[keys.size()];
        for (int i = 0; i < array.length; i++) {
        	values[i] = array[i].toString();
        }
        
        //Sets the link attributes for supported content types ('ct')
        String ctValue = "\"" + String.join(" ", values) + "\"";
        this.setLinkParam(LinkParam.createLinkParam(CT, ctValue));

        //Sets the link attribute to give the resource a title
        String title = "\"Humid Value (updated every " + updateInterval + " seconds)\"";
        this.setLinkParam(LinkParam.createLinkParam(TITLE, title));

        //Sets the link attribute for the resource type ('rt')
        String rtValue = "\"humid\"";
        this.setLinkParam(LinkParam.createLinkParam(RT, rtValue));

        //Sets the link attribute for max-size estimation ('sz')
        this.setLinkParam(LinkParam.createLinkParam(SZ, "" + 100L));

        //Sets the link attribute for interface description ('if')
        String ifValue = "\"GET only\"";
        this.setLinkParam(LinkParam.createLinkParam(IF, ifValue));
	}

	@Override
	public byte[] getEtag(long contentFormat) {
		return Longs.toByteArray(getResourceStatus() | (contentFormat << 56));
	}

	@Override
	public void updateEtag(Long resourceStatus) {
		//nothing to do		
	}
	
	private void schedulePeriodicResourceUpdate() {
        this.periodicUpdateFuture = this.getExecutor().scheduleAtFixedRate(new Runnable() {

            public void run() {
                try{
                    setResourceStatus(System.currentTimeMillis(), updateInterval);
                    LOG.info("New status of resource " + getUriPath() + ": " + getResourceStatus());
                } catch(Exception ex) {
                    LOG.error("Exception while updating actual time...", ex);
                }
            }
        }, updateInterval, updateInterval, TimeUnit.SECONDS);
    }

	@Override
	public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
			InetSocketAddress remoteSocket) throws Exception {
		try{
            if (coapRequest.getMessageCode() == MessageCode.GET) {
                processGet(responseFuture, coapRequest, remoteSocket);
            } else {
                CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(),
                        MessageCode.METHOD_NOT_ALLOWED_405);
                String message = "Service does not allow " + coapRequest.getMessageCodeName() + " requests.";
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
                responseFuture.set(coapResponse);
            }
        }
        catch(Exception ex) {
            responseFuture.setException(ex);
        }
	}

	@Override
	public byte[] getSerializedResourceStatus(long contentFormat) {
		LOG.debug("Try to create payload (content format: " + contentFormat + ")");

        String template = payloadTemplates.get(contentFormat);
        if (template == null) {
            return null;
        } else {
            return String.format(template, lastReadHumidValue, fmt.format(new Date())).getBytes(CoapMessage.CHARSET);
        }
	}
	
	@Override
    public void shutdown() {
        // cancel the periodic update task
        LOG.info("Shutdown service " + getUriPath() + ".");
        boolean futureCanceled = this.periodicUpdateFuture.cancel(true);
        LOG.info("Future canceled: " + futureCanceled);
    }

	@Override
	public void updateArduino(String newValue) {
		if(newValue.contains("HUMIDITY:")) {
			this.lastReadHumidValue = Double.parseDouble(newValue.substring(newValue.indexOf(":") + 1));
		}
	}

	@Override
	public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteSocket) {
		try {
            this.lock.readLock().lock();
            if (!this.observations.containsKey(remoteSocket)) {
                LOG.error("This should never happen (no observation found for \"" + remoteSocket + "\")!");
                return false;
            } else {
                return this.observations.get(remoteSocket) == MessageType.CON;
            }
        } finally {
            this.lock.readLock().unlock();
        }
	}

	@Override
	public void removeObserver(InetSocketAddress remoteAddress) {
		try {
            this.lock.writeLock().lock();
            if (this.observations.remove(remoteAddress) != null) {
                LOG.info("Observation canceled for remote socket \"" + remoteAddress + "\".");
            } else {
                LOG.warn("No observation found to be canceled for remote socket \"remoteAddress\".");
            }
        } finally {
            this.lock.writeLock().unlock();
        }
	}

	 private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
             InetSocketAddress remoteAddress) throws Exception {

		//create resource status
		WrappedResourceStatus resourceStatus;
		if (coapRequest.getAcceptedContentFormats().isEmpty()) {
			resourceStatus = getWrappedResourceStatus(DEFAULT_CONTENT_FORMAT);
		} else {
			resourceStatus = getWrappedResourceStatus(coapRequest.getAcceptedContentFormats());
		}
		
		CoapResponse coapResponse;
		
		if (resourceStatus != null) {
			//if the payload could be generated, i.e. at least one of the accepted content formats (according to the
			//requests accept option(s)) is offered by the Webservice then set payload and content format option
			//accordingly
			coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);
			coapResponse.setContent(resourceStatus.getContent(), resourceStatus.getContentFormat());
			
			coapResponse.setEtag(resourceStatus.getEtag());
			coapResponse.setMaxAge(resourceStatus.getMaxAge());
			
			// this is to accept the client as an observer
			if (coapRequest.getObserve() == 0) {
				coapResponse.setObserve();
				try {
					this.lock.writeLock().lock();
					this.observations.put(remoteAddress, coapRequest.getMessageType());
				} catch(Exception ex) {
					LOG.error("This should never happen!");
				} finally {
					this.lock.writeLock().unlock();
				}
			}
		} else {
			//if no payload could be generated, i.e. none of the accepted content formats (according to the
			//requests accept option(s)) is offered by the Webservice then set the code of the response to
			//400 BAD REQUEST and set a payload with a proper explanation
			coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.NOT_ACCEPTABLE_406);
			
			StringBuilder payload = new StringBuilder();
			payload.append("Requested content format(s) (from requests ACCEPT option) not available: ");
			for(long acceptedContentFormat : coapRequest.getAcceptedContentFormats())
				payload.append("[").append(acceptedContentFormat).append("]");
			
			coapResponse.setContent(payload.toString().getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
		}
	//Set the response future with the previously generated CoAP response
	responseFuture.set(coapResponse);
	}
}
