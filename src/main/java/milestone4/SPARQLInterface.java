package milestone4;

/**
 * An interface to create Sparql requests. The {@link SPARQLConnector} is using
 * the information provided in this Interface.
 * @author Sven Andresen
 */
public interface SPARQLInterface {

	/**
	 * Get the Query that should be asked.
	 * @return query as String
	 */
	public String getQuery();
	
	/**
	 * Get the regex on which we find the values in the answer.
	 * @return regex as String
	 */
	public String getRegex();
	
	/**
	 * Let the {@link SPARQLConnector} call this method to 
	 * update the values returned by the SPARQL Endpoint.
	 * @param newValue
	 */
	public void updateSPARQLValue(String newValue);
}
