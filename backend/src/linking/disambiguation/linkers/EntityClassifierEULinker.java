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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;

import com.beust.jcommander.internal.Lists;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.linker.AbstractLinkerURL;
import structure.linker.AbstractLinkerURLPOST;

public class EntityClassifierEULinker extends AbstractLinkerURLPOST {
	private final String textKeyword = "text";
	private final String apikeyKeyword = "apikey";
	private final String apiKey = "apikey";

	public EntityClassifierEULinker(EnumModelType KG) {
		super(EnumModelType.DBPEDIA_FULL);
		init();
	}

	@Override
	public boolean init() {
		// https://entityclassifier.eu/thd/api/v2/extraction?apikey=123456789&format=xml&provenance=thd&priority_entity_linking=true&entity_type=all
		https();
		url("entityclassifier.eu");
		// port(8080);
		suffix("/thd/api/v2/extraction");
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
		this.params.put(this.textKeyword, inputText);
		return this;
	}

	@Override
	public String getText() {
		return this.params.get(this.textKeyword);
	}

	@Override
	protected HttpURLConnection openConnection(final String input) throws URISyntaxException, IOException {
		final boolean containsMentions = true;
		final StringBuilder sbRequestContent = new StringBuilder();
		sbRequestContent.append(URLEncoder.encode(input, Consts.UTF_8.name()));
//		sbRequestContent.append("\n");
//		if (containsMentions) {
//			sbRequestContent.append("tag_mode=manual");
//		}

		setParam(this.textKeyword, sbRequestContent.toString());
		setParam(this.apikeyKeyword, apiKey);
		setParam(HttpHeaders.ACCEPT_ENCODING, Consts.UTF_8.name());
		// AIDA returns JSON, but it returns an error if we request it
		// request.addHeader(HttpHeaders.ACCEPT, "application/json");
		// We can ask for UTF-8 but it is not clear whether the service really
		// returns UTF-8

		final HttpURLConnection conn = openConnectionWParams();
		return conn;
	}

	@Override
	protected void setupConnectionDetails(HttpURLConnection conn) throws ProtocolException {
		String postDataStr = injectParams();
		byte[] postData = postDataStr.getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;

		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Content-Length", String.valueOf(postDataLength));

		try {
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
		} catch (IOException e) {
			getLogger().error(e.getLocalizedMessage());
		}

	}

	@Override
	public Collection<Mention> textToMentions(String annotatedText) {
		System.out.println("RETURN VALUE:" + annotatedText);
		final JSONObject jsonRet = new JSONObject(annotatedText);
		final List<Mention> mentions = Lists.newArrayList();

		return mentions;
	}

	@Override
	protected String injectParams() {
		final String paramText = injectParam(this.textKeyword);
		if (paramText == null) {
			getLogger().error("No input text passed to " + this.getClass().getName() + " Linker");
			return null;
		}

		final StringBuilder params = new StringBuilder();
		final Iterator<Map.Entry<String, String>> it = this.params.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> e = it.next();
			params.append(e.getKey() + equalSymbol + e.getValue());
			if (it.hasNext()) {
				params.append(ampersandSymbol);
			}
		}
		return params.toString();
	}
}
