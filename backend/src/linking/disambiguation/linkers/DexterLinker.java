/**
 * This file is part of General Entity Annotator Benchmark.
 *
 * General Entity Annotator Benchmark is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * General Entity Annotator Benchmark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with General Entity Annotator Benchmark.  If not, see <http://www.gnu.org/licenses/>.
 */
package linking.disambiguation.linkers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.BiFunction;

import org.apache.jena.riot.RiotException;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.linker.AbstractLinkerURL;
import structure.linker.AbstractLinkerURLPOST;
import structure.linker.LinkerNIF;
import structure.utils.LinkerUtils;

public class DexterLinker extends AbstractLinkerURLPOST implements LinkerNIF {
	private final String keywordContent = "content";
	public Number defaultScore = 0.5d;// 1.0d// getWeight()
	;

	public DexterLinker(EnumModelType KG) {
		super(EnumModelType.DBPEDIA_FULL);
		init();
	}

	@Override
	public boolean init() {
		// http://dexterdemo.isti.cnr.it:8080/dexter-webapp/api/nif/annotate
		http();
		// url("dexterdemo.isti.cnr.it");
//		suffix("/dexter-webapp/api/nif/annotate");
		url("dexter.isti.cnr.it");
		suffix("/demo");
		// port(8080);
		return true;
	}

	@Override
	public Number getWeight() {
		return 1.0f;
	}

	@Override
	public BiFunction<Number, Mention, Number> getScoreModulationFunction() {
		return null;
	}

	@Override
	public AbstractLinkerURL setText(String inputText) {
		final String nifInput = createNIF(inputText);
		this.params.put(this.keywordContent, nifInput);
		return this;
	}

	@Override
	public String getText() {
		return this.params.get(this.keywordContent);
	}

	@Override
	protected HttpURLConnection openConnection(final String input) throws URISyntaxException, IOException {
		// final String confidence = Float.toString(this.confidence);
		// final String query = textKeyword + "=" + input + "&" + confidenceKeyword +
		// "=" + confidence;
		// -----------------------------------
		// Transform input into NIF input!
		// -----------------------------------

		final String nifInput = createNIF(input);
		setParam(this.keywordContent, nifInput);
		final HttpURLConnection conn = openConnectionWParams();
		return conn;

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
	public Collection<Mention> textToMentions(String annotatedText) {
		// Transform nif to another format
		try {
			Collection<Mention> mentions = LinkerUtils.nifToMentions(annotatedText, defaultScore);
			return mentions;
		}
		catch (RiotException exc)
		{
			System.out.println(annotatedText);
		}
		return null;
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
}
