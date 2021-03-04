package linking.disambiguation.linkers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import structure.config.constants.Strings;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.interfaces.CandidateGeneratorDisambiguator;
import structure.interfaces.pipeline.AnnotationPipelineItem;
import structure.interfaces.pipeline.FullAnnotator;
import structure.interfaces.pipeline.MentionDetector;
import structure.linker.AbstractLinkerURL;
import structure.linker.AbstractLinkerURLGET;
import structure.utils.FunctionUtils;
import structure.utils.LinkerUtils;

public class BabelfyLinker extends AbstractLinkerURLGET implements FullAnnotator, MentionDetector,
		CandidateGeneratorDisambiguator {

	final String keywordText = "text";
	final String keywordLang = "lang";
	final String keywordKey = "key";

	final String paramLang = "EN";
	final String paramKey = Strings.BABELFY_KEY.val;

	public BabelfyLinker() {
		this(EnumModelType.WORDNET);
	}

	public BabelfyLinker(EnumModelType KG) {
		super(KG);
		init();
	}

	@Override
	public boolean init() {
		https();
		url("babelfy.io");
		suffix("/v1/disambiguate");
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
	protected HttpURLConnection openConnection(String input) throws URISyntaxException, IOException {
		// Set the GET parameters
		setParam(keywordKey, paramKey);
		setParam(keywordLang, paramLang);
		setParam(keywordText, input);
		final HttpURLConnection conn = openConnectionWParams();
		//System.out.println("URL: " + conn.getURL());
		return conn;
	}

	@Override
	protected void setupConnectionDetails(HttpURLConnection conn) throws ProtocolException {
		// conn.setRequestProperty("Accept-Encoding", "gzip");
		conn.setRequestProperty("Accept-Encoding", "application/json");
	}

	@Override
	public Collection<Mention> textToMentions(String annotatedText) {
		final String inputText = this.params.get(keywordText);
		if (inputText == null)
		{
			System.err.println("No input defined");
		}
		return LinkerUtils.babelfyJSONtoMentions(annotatedText, inputText);
	}

	@Override
	public String getText() {
		return this.params.get(this.keywordText);
	}

	@Override
	public AbstractLinkerURL setText(final String inputText) {
		this.params.put(this.keywordText, inputText);
		return this;
	}

	@Override
	// NOT provided by Babelfy API, using workaround
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
		//MentionUtils.displayMentions(mentions);
		return mentions;
	}

	@Override
	public List<Mention> detect(String input, String source) {
		return detect(input);
	}

	@Override
	// NOT provided by Babelfy API, using workaround
	public Collection<Mention> generateDisambiguate(String inputText, Collection<Mention> mentions) {
		// Workaround: Asking Babelfy to disambiguate texts that contain only mentions that were already detected in the MD
		Collection<Mention> mentionsWithEntityCandidates = new ArrayList<>();
		for (Mention mention : mentions) {
			String mentionText = mention.getOriginalMention();
			try {
				Collection<Mention> res = annotateMentions(mentionText);
				for (Mention mentionWithEntityCandidate : res) {
					// Use only the original mention from Babelfy (not possibly others, esp. substrings)
					if (mentionWithEntityCandidate.getMention().equals(mentionText)) {
						mentionWithEntityCandidate.setOffset(mention.getOffset());
						mentionsWithEntityCandidates.add(mentionWithEntityCandidate);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//MentionUtils.displayMentions(mentionsWithEntityCandidates);
		return (List<Mention>) mentionsWithEntityCandidates;
	}

	@Override
	public Collection<Collection<Mention>> execute(AnnotationPipelineItem callItem,
			String text) {
		// TODO Auto-generated method stub
		return null;
	}
}
