package structure.linker;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;

import structure.config.kg.EnumModelType;

public abstract class AbstractLinkerURLPOST extends AbstractLinkerURL implements LinkerURLPOST {

	public AbstractLinkerURLPOST(EnumModelType KG) {
		super(KG);
	}

	protected void setupRequestMethod(final HttpURLConnection conn) throws ProtocolException {
		conn.setRequestMethod("POST");
	}

	@Override
	protected URI makeURI() throws URISyntaxException {
		return makeURI(null);
	}

}
