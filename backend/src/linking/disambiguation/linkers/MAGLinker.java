package linking.disambiguation.linkers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Span;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.linker.AbstractLinkerURL;
import structure.linker.AbstractLinkerURLPOST;
import structure.utils.LinkerUtils;

public class MAGLinker extends AbstractLinkerURLPOST {
	
	final String keywordType = "type";
	final String keywordText = "text";
	
	final String paramType = "agdistis";
	private Number defaultScore = 1.0d;
	
	public MAGLinker() {
		this(EnumModelType.DEFAULT);
	}

	public MAGLinker(EnumModelType KG) {
		super(KG);
		init();
	}

	@Override
	public boolean init() {
		http();
		url("akswnc9.informatik.uni-leipzig.de");
		port(8113);
		suffix("/AGDISTIS");
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
	protected HttpURLConnection openConnection(String input) throws URISyntaxException, IOException {
		setParam(keywordType, paramType);
		setParam(keywordText, input);
		final HttpURLConnection conn = openConnectionWParams();
		System.out.println(conn.getURL());
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
		
		
//		Reader in = null;
//		try {
//			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//        StringBuilder sb = new StringBuilder();
//        try {
//			for (int c; (c = in.read()) >= 0;)
//			    sb.append((char)c);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        String response = sb.toString();
//
//        System.out.println(response);
	}
	
	/**
	 * Performs candidate generation and entity disambiguation based on the
	 * result of the mention detection
	 * @param document Document after mention detection
	 * @return Disambiguated text
	 * @throws IOException
	 */
	public Collection<Mention> performD2KB(final Document document)
			throws IOException {
		final String textWithTags = LinkerUtils.textMarkingsToMAG(
				document.getText(), document.getMarkings(Span.class));
		Collection<Mention> mentions = annotateMentions(textWithTags);
		return mentions;
		
		// Alternatively with GERBIL:
//		try {
//			AgdistisAnnotator linker = new AgdistisAnnotator();
//			Collection<MeaningSpan> res = linker.getAnnotations(textWithTags);
//			Collection<Mention> mentions = LinkerUtils.convertMeaningSpanToMention(
//					document.getText(), res);
//			linker.close();
//			return mentions;
//		} catch (GerbilException e) {
//			e.printStackTrace();
//			return null;
//		}
	}
	
	public String inputToTags(String text, List<Span> mentions) {
		String textWithTags = LinkerUtils.textMarkingsToMAG(text, mentions);
		return textWithTags;
	}

	@Override
	public Collection<Mention> textToMentions(String annotatedText) {
		final String inputText = this.params.get(keywordText);
		if (inputText == null)
		{
			System.err.println("No input defined");
		}
		return LinkerUtils.magJSONtoMentions(annotatedText, inputText, defaultScore);
	}

	@Override
	protected String injectParams() {
		final String equalSymbol = "=";
		final String ampersandSymbol = "&";
		
		final String paramText = injectParam(keywordText);
		if (paramText == null) {
			getLogger().error("No input text passed to MAG");
			return null;
		}
		
		String paramTextEncoding;
        try {
            paramTextEncoding = URLEncoder.encode(paramText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            getLogger().error("Couldn't encode input text", e);
            return null;
        }

		StringBuilder params = new StringBuilder();
		params.append(keywordType + equalSymbol + paramType);
		params.append(ampersandSymbol);
		params.append(keywordText + equalSymbol + paramTextEncoding);

		return params.toString();
	}

	@Override
	public AbstractLinkerURL setText(String inputText) {
		this.params.put(this.keywordText, inputText);
		return this;
	}

	@Override
	public String getText() {
		return injectParam(this.keywordText);
	}

}
