package milestone2;
import java.util.concurrent.ScheduledExecutorService;

import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;

/**
 * This is a superclass of all the Ressources for the SSP. This is just so that we can handle the ressources
 * more independently.
 * @author Sven Andresen
 *
 */
public abstract class Ressource<T> extends ObservableWebresource<T> {

	/**
	 * Create a {@link ObservableWebresource}.
	 * @param uriPath The path to the WebRessource
	 * @param initialStatus initial Status of the Ressource
	 * @param lifetime The lifetime of this Ressource
	 * @param executor an {@link ScheduledExecutorService}
	 */
	protected Ressource(String uriPath, T initialStatus, long lifetime, ScheduledExecutorService executor) {
		super(uriPath, initialStatus, lifetime, executor);
	}
	
	/**
	 * Create a {@link ObservableWebresource}.
	 * @param uriPath The path to the WebRessource
	 * @param initialStatus initial Status of the Ressource
	 * @param executor an {@link ScheduledExecutorService}
	 */
	protected Ressource(String uriPath, T initialStatus, ScheduledExecutorService executor) {
		super(uriPath, initialStatus, executor);
	}
}
