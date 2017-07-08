package milestone2;

/**
 * Interface that will implment a simple listener for the SPARQL lux values.
 * @author Sven Andresen
 *
 */
public interface SparqlListener {
	
	/**
	 * Indicate that a new Lux Value has been read from the SPARQL Endpoint.
	 * @param newValue
	 */
	void updateSparqlListener(double newValue);
}
