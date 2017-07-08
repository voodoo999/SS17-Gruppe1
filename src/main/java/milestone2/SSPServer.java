package milestone2;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import de.uzl.itm.ncoap.application.endpoint.CoapEndpoint;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;

/**
 * This class represents a simple SSP Server that is a COAP Endpoint.
 * 
 * @author Sven Andresen
 */
public class SSPServer extends CoapEndpoint {
    private static Logger LOG = Logger.getLogger(SSPServer.class.getName());
	
	private String SSP_HOST = "141.83.151.196";
	private int SSP_PORT = 5683;
	
	public void registerRessource(Ressource<?> ressource) {
		this.registerWebresource(ressource);
	}

    public SSPServer(BlockSize block1Size, BlockSize block2Size) {
        super(block1Size, block2Size);
    }
    
    public void registerAtSSP(String host, int port) throws URISyntaxException {
    	this.SSP_HOST = host;
    	this.SSP_PORT = port;
    	registerAtSSP();
    }

    public void registerAtSSP() throws URISyntaxException {
    	
        URI resourceURI = new URI ("coap", null, SSP_HOST, SSP_PORT, "/registry", null, null);
        LOG.info("Connecting to SSP on " + SSP_HOST + " on Port " + SSP_PORT);
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, resourceURI);
        InetSocketAddress remoteSocket = new InetSocketAddress(SSP_HOST, SSP_PORT);

        SimpleCallback callback = new SimpleCallback();
        this.sendCoapRequest(coapRequest, remoteSocket, callback);
    }   
}
