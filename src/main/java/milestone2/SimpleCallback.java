package milestone2;


import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.message.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a very simple implementation of {@link ClientCallback} which does virtually nothing but log  internal events.
 *
 * @author Oliver Kleine
 */
public class SimpleCallback extends ClientCallback {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private AtomicBoolean responseReceived;
    private AtomicInteger transmissionCounter;
    private AtomicBoolean timedOut;



    public SimpleCallback() {
        this.responseReceived = new AtomicBoolean(false);
        this.transmissionCounter = new AtomicInteger(0);
        this.timedOut = new AtomicBoolean(false);
    }

    /**
     * Increases the reponse counter by 1, i.e. {@link #getResponseCount()} will return a higher value after
     * invocation of this method.
     *
     * @param coapResponse the response message
     */
    @Override
    public void processCoapResponse(CoapResponse coapResponse) {
        responseReceived.set(true);
        log.info("Received: {}", coapResponse);
    }

    /**
     * Returns the number of responses received
     * @return the number of responses received
     */
    public int getResponseCount() {
        return this.responseReceived.get() ? 1 : 0;
    }


    @Override
    public void processRetransmission() {
        int value = transmissionCounter.incrementAndGet();
        log.info("Retransmission #{}", value);
    }


    @Override
    public void processTransmissionTimeout() {
        log.info("Transmission timed out...");
        timedOut.set(true);
    }

    @Override
    public void processResponseBlockReceived(long receivedLength, long expectedLength) {
        log.info("Received {}/{} bytes.", receivedLength, expectedLength);
    }

    public boolean isTimedOut() {
        return timedOut.get();
    }
}
