package structure.linker;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map.Entry;

import structure.config.kg.EnumModelType;

public abstract class AbstractLinkerURLGET extends AbstractLinkerURL implements LinkerURLGET {

	public AbstractLinkerURLGET(EnumModelType KG) {
		super(KG);
	}
	
	/**
	 * For GET requests, we have to inject parameters prior to opening connection
	 * 
	 * @return created URI
	 * @throws URISyntaxException
	 */
	public URI makeURI() throws URISyntaxException {
		final String query = injectParams();
		return makeURI(query);
	}

	/**
	 * Takes the map of parameters and injects them appropriately
	 * 
	 * @return
	 */
	public String injectParams() {
		final StringBuilder sb = new StringBuilder();
		if (this.params.size() < 1) {
			return null;
		}

		// Get the first element bc it doesn't need an ampersand (&)
		final Iterator<Entry<String, String>> it = this.params.entrySet().iterator();
		Entry<String, String> e = it.next();
		sb.append(e.getKey());
		sb.append("=");
		sb.append(e.getValue());
		while (it.hasNext()) {
			e = it.next();
			// Using this scheme of popping first and continuing w/ rest due to ampersand
			// (&)
			sb.append("&");
			sb.append(e.getKey());
			sb.append("=");
			sb.append(e.getValue());

		}
		return sb.toString();
	}

	protected void setupRequestMethod(final HttpURLConnection conn) throws ProtocolException {
		conn.setRequestMethod("GET");
	}
}
