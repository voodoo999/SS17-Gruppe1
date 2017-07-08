package milestone4;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A simple SPARQL Connector that will use the {@link SPARQLInterface} to get Querys
 * and update accordingly.
 * @author Sven Andresen
 */
public class SPARQLConnector extends Thread {
	private static Logger LOG = Logger.getLogger(SPARQLConnector.class.getName());

	private List<SPARQLInterface> sparqlQueries = new ArrayList<SPARQLInterface>();
	private List<SPARQLInterface> sparqlList2 = new ArrayList<SPARQLInterface>();
	private String ip = "141.83.151.196";
	private String port = "8080";
	private String accept = "text/csv";
	
	/**
	 * Construct the SPARQLConnector tu use the ip 141.83.151.196 and
	 * the port 8080.
	 */
	public SPARQLConnector() {}
			
	/**
	 * Contruct the SPARQLConnector to use a specific ip and port.
	 * @param ip
	 * @param port
	 */
	public SPARQLConnector(String ip, String port) {
		this.ip = ip;
		this.port = port;
	}
	
	/**
	 * Gets the result of the Sparql Query form the endpoint.
	 * @return Linked list of the strings as results.
	 * @throws Exception
	 */
	public LinkedList<String> getResult(String query) throws Exception {
		
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("http://" + this.ip + ":" + this.port + "/services/sparql-endpoint");
		
		post.addHeader("Accept", this.accept);
		post.addHeader("Content-Type", "multipart/form-data; boundary=DATA");

		String data = "--DATA\n" +
				"Content-Disposition: form-data; name=\"query\"\n\n" +
				query + "\n" +
				"--DATA--";
		post.setEntity(new StringEntity(data));
		
		HttpResponse response = client.execute(post);
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		LinkedList<String> lines = new LinkedList<String>();
		String line = "";
		while ((line = rd.readLine()) != null) {
			lines.add(line.trim());
		}
		return lines;
	}	
	
	public void run() {
		LOG.info("Starting connection to SPARQL Endpoint: " + ip);
		while(true) {
			for(SPARQLInterface clazz : sparqlQueries) {
				LinkedList<String> ll;
				try {
					ll = getResult(clazz.getQuery());
					if(ll.isEmpty())
						LOG.error("Nothing returned from SPARQL");
					else if(ll.size() == 1) {
						HashMap<String,Object> result =
						        new ObjectMapper().readValue(ll.removeFirst(), HashMap.class);
						Pattern pattern = Pattern.compile(clazz.getRegex());
						String results = (String) result.get("results");
//						result = new ObjectMapper().readValue(results, HashMap.class);
//						results = (String) result.get("bindings");
						Matcher matcher = pattern.matcher(results);
						if(matcher.find()) { //found a new Lux Value
							String newValue = matcher.group(0);
							updateListener(clazz, newValue);
						} else {
							LOG.info("No match found in the SPARQL Query!");
							updateListener(clazz, null);
						}
					} else 
						LOG.warn("Received more than just one value on the SPARQL Query!");
				} catch (Exception e) {
					LOG.error(e);
				}
			}
			
			try {
				updateSparqlList();
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	
	
	private void updateSparqlList() {
		sparqlQueries.addAll(sparqlList2);
		sparqlList2.clear();
	}

	private void updateListener(SPARQLInterface clazz, String newValue) {
		try {
			clazz.updateSPARQLValue(newValue);
		} catch (Exception e) {
			LOG.error("Exception updating "+ clazz, e);
		}
	}
	
	/**
	 * Register a {@link SPARQLInterface} at this class to update or insert a 
	 * SPARQL Request.
	 * @param clazz SPARQLInterface
	 */
	public void registerAtSPARQLConnector(SPARQLInterface clazz) 
			throws IllegalArgumentException {
		if(sparqlQueries.contains(clazz))
			throw new IllegalArgumentException("Class already registered");
		this.sparqlList2.add(clazz);
	}
}
