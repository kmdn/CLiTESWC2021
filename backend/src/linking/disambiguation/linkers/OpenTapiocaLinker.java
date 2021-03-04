package linking.disambiguation.linkers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.BiFunction;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.linker.AbstractLinkerURL;
import structure.linker.AbstractLinkerURLPOST;
import structure.linker.LinkerNIF;
import structure.utils.FunctionUtils;
import structure.utils.LinkerUtils;

public class OpenTapiocaLinker extends AbstractLinkerURLPOST implements LinkerNIF {
	public Number defaultScore = 0.5d;// 1.0d// getWeight()
	;

	public OpenTapiocaLinker() {
		super(EnumModelType.WIKIDATA);
		init();
	}

	private final String keywordContent = "content";

	@Override
	public boolean init() {
		https();
		url("opentapioca.org");
		suffix("/api/nif");
		// suffix("/api/annotate");
		return true;
	}

	@Override
	public Number getWeight() {
		return 1.0f;
	}

	@Override
	public BiFunction<Number, Mention, Number> getScoreModulationFunction() {
		return FunctionUtils::returnScore;
	}

	@Override
	public HttpURLConnection openConnection(final String input) throws URISyntaxException, IOException {
		// final String confidence = Float.toString(this.confidence);
		// final String query = textKeyword + "=" + input + "&" + confidenceKeyword +
		// "=" + confidence;
		// -----------------------------------
		// Transform input into NIF input!
		// -----------------------------------

		final String nifInput = createNIF(input);
		setParam(keywordContent, nifInput);
		// setParam(paramContent, input);
		// setParam(confidenceKeyword, confidence);
		final HttpURLConnection conn = openConnectionWParams();
		return conn;

//		final String urlParameters  = "param1=a&param2=b&param3=c";
//		final byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
//		final int    postDataLength = postData.length;
//		final String request        = "http://example.com/index.php";
//		final URL    url            = new URL( request );
//		final HttpURLConnection conn= (HttpURLConnection) url.openConnection();           
//		conn.setDoOutput( true );
//		conn.setInstanceFollowRedirects( false );
//		conn.setRequestMethod( "POST" );
//		conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
//		conn.setRequestProperty( "charset", "utf-8");
//		conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
//		conn.setUseCaches( false );
//		try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
//		   wr.write( postData );
//		}
//		
//		super.openConnection(url);
//		return null;

	}

	@Override
	public Collection<Mention> textToMentions(String annotatedText) {
		// Transform nif to another format
		return LinkerUtils.nifToMentions(annotatedText, defaultScore);
	}

	@Override
	protected String injectParams() {
		// POST-parameter-wise injection of details
		if (this.params.size() > 1) {
			getLogger().error("ERROR - OpenTapioca only handles a single parameter (namely the content)");
		}

		final String nifContent = injectParam(keywordContent);
		if (nifContent != null) {
			return nifContent;
		}

		getLogger().error("No parameter passed to POST request...");
		return null;
	}

	@Override
	protected void setupConnectionDetails(HttpURLConnection conn) throws ProtocolException {
		// Add connection-type-specific stuff, for POST add the contents
		// For GET w/e may be needed
		conn.setRequestProperty("accept", "application/x-turtle");
		conn.setRequestProperty("Content-Type", "application/x-turtle");
		// conn.setRequestProperty("Accept-Encoding", "gzip");
		try {
			final String postDataStr = injectParams();
			final byte[] postData = postDataStr.getBytes(StandardCharsets.UTF_8);
			final int postDataLength = postData.length;
			conn.setRequestProperty("Content-Length", String.valueOf(postDataLength));

			// Outputs the data
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
		} catch (IOException e) {
			getLogger().error(e.getLocalizedMessage());
		}
	}

	@Override
	public String getText() {
		return this.params.get(this.keywordContent);
	}

	@Override
	public AbstractLinkerURL setText(final String inputText) {
		this.params.put(this.keywordContent, inputText);
		return this;
	}

	@Override
	public int hashCode() {
		int ret = 0;
		ret += nullHash(this.keywordContent, 2);
		ret += nullHash(getClass(), 4);
		ret += nullHash(getKG(), 8);
		ret += nullHash(getScoreModulationFunction(), 16);
		ret += nullHash(getUrl(), 32);
		ret += nullHash(getTimeout(), 64);
		ret += nullHash(this.params, 128);
		ret += nullHash(this.getUrl(), 256);
		return ret + super.hashCode();
	}

}
