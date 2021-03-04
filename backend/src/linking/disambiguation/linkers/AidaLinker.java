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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import com.beust.jcommander.internal.Lists;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.CandidateGeneratorDisambiguator;
import structure.interfaces.pipeline.AnnotationPipelineItem;
import structure.interfaces.pipeline.FullAnnotator;
import structure.interfaces.pipeline.MentionDetector;
import structure.linker.AbstractLinkerURL;
import structure.linker.AbstractLinkerURLPOST;

/**
 * Client implementation of the web service of the AIDA annotator. The API is
 * described here: <a href=
 * "http://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/aida/webservice/">
 * http://www.mpi-inf.mpg.de/departments/databases-and-information-systems/
 * research/yago-naga/aida/webservice/</a>
 *
 */
public class AidaLinker extends AbstractLinkerURLPOST implements FullAnnotator, MentionDetector,
		CandidateGeneratorDisambiguator {

	private final String equalSymbol = "=";
	private final String ampersandSymbol = "&";
	private final String textKeyword = "text";

	public AidaLinker(EnumModelType KG) {
		super(EnumModelType.DBPEDIA_FULL);
		init();
	}

	@Override
	public boolean init() {
		// https://gate.d5.mpi-inf.mpg.de/aida/service/disambiguate
		https();
		url("gate.d5.mpi-inf.mpg.de");
		// port(8080);
		suffix("/aida/service/disambiguate");
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
		final JSONObject jsonRet = new JSONObject(annotatedText);
		final JSONArray arrMentions = jsonRet.getJSONArray("mentions");
		final List<Mention> mentions = Lists.newArrayList();

		for (int i = 0; i < arrMentions.length(); ++i) {
			final JSONObject allEntitiesJSONObj = arrMentions.getJSONObject(i);
			// final JSONArray entities = allEntitiesJSONObj.getJSONArray("allEntities");
			final int offset = allEntitiesJSONObj.getInt("offset");
			final String name = allEntitiesJSONObj.getString("name");
			final int length = allEntitiesJSONObj.getInt("length");
			final JSONObject bestEntity = allEntitiesJSONObj.getJSONObject("bestEntity");
			final String entity = bestEntity.getString("kbIdentifier");
			final Double disambiguationScore = bestEntity.getDouble("disambiguationScore");
			final PossibleAssignment possAss = new PossibleAssignment(entity, disambiguationScore);
			final Mention m = new Mention(name, possAss, offset, 1.0f, name, name);
			mentions.add(m);
		}
		return mentions;
	}

	@Override
	protected String injectParams() {
		final String paramText = injectParam(this.textKeyword);
		if (paramText == null) {
			getLogger().error("No input text passed to AIDA Linker");
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

	@Override
	// TODO Check if provided by AIDA API, currently using workaround
	public List<Mention> detect(String input) {
		ArrayList<Mention> mentions = null;
		try {
			mentions = (ArrayList<Mention>) annotateMentions(input);
			// Workaround: remove candidates manually, keep only the markings
			for (Mention mention : mentions) {
				mention.toMentionDetectionResult();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// MentionUtils.displayMentions(mentions);
		return mentions;
	}

	@Override
	public List<Mention> detect(String input, String source) {
		return detect(input);
	}

	@Override
	// TODO Check if provided by AIDA API, currently using workaround
	public Collection<Mention> generateDisambiguate(String inputText, Collection<Mention> mentions) {
		// Workaround: Asking Babelfy to disambiguate texts that contain only mentions
		// that were already detected in the MD
		Collection<Mention> mentionsWithEntityCandidates = new ArrayList<>();
		for (Mention mention : mentions) {
			String mentionText = mention.getOriginalMention();
			try {
				Collection<Mention> res = annotateMentions(mentionText);
				for (Mention mentionWithEntityCandidate : res) {
					// Use only the original mention from AIDA (not possibly others, esp.
					// substrings)
					if (mentionWithEntityCandidate.getMention().equals(mentionText)) {
						mentionWithEntityCandidate.setOffset(mention.getOffset());
						mentionsWithEntityCandidates.add(mentionWithEntityCandidate);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// MentionUtils.displayMentions(mentionsWithEntityCandidates);
		return (List<Mention>) mentionsWithEntityCandidates;
	}

	@Override
	public Collection<Collection<Mention>> execute(AnnotationPipelineItem callItem, String text) {
		final Collection<AnnotationPipelineItem> dependencies = callItem.getDependencies();
		if (dependencies.size() == 1) {
			final Collection<Collection<Mention>> multiMentions = dependencies.iterator().next().getResults();
			if (multiMentions.size() != 1) {
				throw new RuntimeException(
						"Invalid number of arguments passed: multiMentions size[" + multiMentions.size() + "]");
			}

			if (multiMentions.size() != 1) {
				throw new IllegalArgumentException(
						"Wrong amount of mentions: multiMentions[" + multiMentions.size() + "]");
			}
			final Collection<Mention> mentions = generateDisambiguate(text, multiMentions.iterator().next());
			final Collection<Collection<Mention>> retMentions = Lists.newArrayList();
			retMentions.add(mentions);
			return retMentions;
		} else {
			throw new IllegalArgumentException("Invalid number of dependencies for this component...");
		}
	}
}
