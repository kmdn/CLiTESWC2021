package api;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.data.ScoredNamedEntity;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.beust.jcommander.internal.Lists;

import linking.candidategeneration.CandidateGeneratorMap;
import linking.disambiguation.DisambiguatorAgnos;
import linking.mentiondetection.InputProcessor;
import linking.mentiondetection.StopwordsLoader;
import linking.mentiondetection.exact.HashMapCaseInsensitive;
import linking.pruning.MentionPruner;
import linking.pruning.ThresholdPruner;
import structure.config.constants.Comparators;
import structure.config.constants.EnumEmbeddingMode;
import structure.config.constants.Strings;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.interfaces.Executable;
import structure.interfaces.pipeline.CandidateGenerator;
import structure.interfaces.pipeline.MentionDetector;
import structure.utils.DetectionUtils;
import structure.utils.Stopwatch;

/**
 * Class handling annotation tasks for GERBIL
 * 
 * @author Kristian Noullet
 *
 */
public class JSONAPIAnnotator implements Executable {

	private BufferedWriter log = null;
	private final String NEWLINE = Strings.NEWLINE.val;
	private final Map<String, EnumModelType> KGs = new HashMapCaseInsensitive<>();

	private final String chooserWatch = "Scorer (Watch)";
	private final String detectionWatch = MentionDetector.class.getName();
	private final String linking = "Linking (Watch)";
	private final boolean REMOVE_OVERLAP = true;

	private static final boolean detailed = false;
	private static int docCounter = 0;
	private final boolean preRestrictToMarkings = false;
	private final boolean postRestrictToMarkings = true;
	private final boolean preRestrictToCapitalFirstLetter = true;
	// No touchy
	private Map<String, Boolean> init = new HashMap<>();
	private final Comparator<Mention> offsetComparator = Comparators.mentionOffsetComp;

	private Set<String> stopwords = null;
	private Map<String, MentionDetector> mdMap = new HashMap<>();
	private Map<String, CandidateGenerator> candidateGeneratorMap = new HashMap<>();
	private Map<String, DisambiguatorAgnos> disambiguatorMap = new HashMap<>();
	private Map<String, MentionPruner> prunerMap = new HashMap<>();
	private final EnumEmbeddingMode embeddingMode;
	private final String outFilepath = "/vol2/kris/api_agnos.log";

	public JSONAPIAnnotator() {
		this(EnumEmbeddingMode.DEFAULT.val, EnumModelType.values());
	}

	public JSONAPIAnnotator(final EnumModelType... KG) {
		this(EnumEmbeddingMode.DEFAULT.val, KG);
	}

	public JSONAPIAnnotator(final EnumEmbeddingMode embeddingMode, final EnumModelType... KGs) {
		if (KGs != null && KGs.length > 0) {
			for (EnumModelType KG : KGs) {
				log("Constructor2(" + KG.name() + ")");
				addKG(KG);
			}
		}
		this.embeddingMode = embeddingMode;
		addKG("wd", EnumModelType.WIKIDATA);
		// addKG("dbp", EnumModelType.DBPEDIA_FULL);
	}

	private void addKG(EnumModelType KG) {
		this.KGs.put(KG.name(), KG);
	}

	private void addKG(final String key, EnumModelType KG) {
		this.KGs.remove(KG.name());
		this.KGs.put(key, KG);
	}

	@Override
	public synchronized void init() {
		for (Map.Entry<String, EnumModelType> e : this.KGs.entrySet()) {
			final EnumModelType KG = e.getValue();
			init(KG);
		}
	}

	@SuppressWarnings("unused")
	public synchronized void init(final EnumModelType KG) {
		log("JSON Annotator started init(" + KG.name() + ")");
		synchronized (this.init) {
			if (this.init.getOrDefault(KG.name(), false)) {
				return;
			}
		}
		// Load all the necessary stuff
		// such as embeddings, LSH sparse vectors and hashes
		try {
			getLogger().info("Initializing Framework Structures");
			getLogger().info("Loading mention possibilities...");
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			this.stopwords = stopwordsLoader.getStopwords();
			final Map<String, Collection<String>> map = DetectionUtils.loadSurfaceForms(KG, stopwordsLoader);
			final InputProcessor inputProcessor = new InputProcessor(stopwords);
			// ########################################################
			// Mention Detection
			// ########################################################
			this.mdMap.put(KG.name(), DetectionUtils.setupMentionDetection(KG, map, inputProcessor));

			// ########################################################
			// Candidate Generator
			// ########################################################
			this.candidateGeneratorMap.put(KG.name(), new CandidateGeneratorMap(map));
			Stopwatch.endOutputStart(getClass().getName());
			// Initialise AssignmentChooser
			Stopwatch.start(chooserWatch);

			final Set<String> wantedResources = new HashSet<>();
			for (Map.Entry<String, Collection<String>> e : map.entrySet()) {
				wantedResources.addAll(e.getValue());
			}

			final DisambiguatorAgnos disambiguator = new DisambiguatorAgnos(KG, this.embeddingMode, wantedResources);
			this.disambiguatorMap.put(KG.name(), disambiguator);
			Stopwatch.endOutput(chooserWatch);
			this.prunerMap.put(KG.name(), new ThresholdPruner(1.0d));
			// Add that it was initialised
			this.init.put(KG.name(), true);
			log("finished initializing JSON annotator");
		} catch (Exception exc) {
			getLogger().error("Exception during init", exc);
		}
	}

	private void log(String string) {
		if (this.log == null) {
			BufferedWriter logHelper = null;
			try {
				logHelper = new BufferedWriter(new FileWriter(new File(outFilepath), true));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.log = logHelper;
		}
		// ------------------------------------
		try {
			this.log.write(string + NEWLINE);
			this.log.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(string);
	}

	/**
	 * Do not change unless you have changed the call on the WEB API
	 * 
	 * @param inputStream NIFInputStream
	 * @return
	 */
	public String annotate(final InputStream inputStream) {

		// 1. Generate a Reader, an InputStream or a simple String that contains the NIF
		// sent by GERBIL
		// 2. Parse the JSON String using a Parser (currently, we use only Turtle)
		JSONObject jsonObject = null;
		try {
			final JSONParser jsonParser = new JSONParser();
			jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
		} catch (IOException | ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return annotateDocument(jsonObject);
	}

	/**
	 * Annotate a pre-defined JSON Document
	 * 
	 * @param jsonObj
	 * @return
	 */
	public String annotateDocument(final JSONObject jsonObj) {
		// In case it hasn't been initialised yet
		init();
		log("Annotate input document");

		// {"topk":false,"input":"hello","kg":"DBP","fuzzy":false,"mentiondetection":false}
		final int MIN_MARKINGS = 1;

		final String inputKey = "input";
		final String mdKey = "mentiondetection";
		final String topKKey = "topk";
		final String fuzzyKey = "fuzzy";
		final String retJSONKey = "retjson";
		final String kgKey = "kg";

		// Content of the text area
		final String text = jsonObj.optString(inputKey, null);
		if (text == null || text.length() <= 0) {
			return "";
		}
		// Whether to do MD
		final Boolean mentionDetection = jsonObj.optBoolean(mdKey, true);
		// Whether to output top K is just the best per mention
		final Boolean topK = jsonObj.optBoolean(topKKey, false);
		// Whether to apply fuzzy matching (just works w/ MD enabled)
		final Boolean fuzzy = jsonObj.optBoolean(fuzzyKey, false);
		// Whether to return a JSON object
		final Boolean returnJSON = jsonObj.optBoolean(retJSONKey, false);
		// Get which KG to do it for
		final String chosenKG = jsonObj.optString(kgKey, "dbpedia");

		log("[" + text + "]," + mentionDetection + "," + topK + "," + fuzzy + "," + returnJSON + "," + chosenKG);
		EnumModelType KG = findKG(chosenKG);
		log("Found KG:" + KG);
		if (KG == null) {
			getLogger().error("Could not find an available KG for '" + chosenKG + "'");
			KG = EnumModelType.
			// DEFAULT;
					DBPEDIA_FULL;
			// return "ERROR";
		} else {
			getLogger().info("Found KG: " + KG.name());
		}
		// 3. use the text and maybe some Markings sent by GERBIL to generate your
		// Markings
		// (a.k.a annotations) depending on the task you want to solve
		// 4. Add your generated Markings to the document
		try {
			log("Started annotating!");
			final Collection<? extends Marking> markings = annotatePlainText(KG, text, null);
			log("Finished annotating!");
			if (returnJSON) {
				log("Processing JSON to return");
				final JSONObject retJSONObj = new JSONObject();
				for (Marking m : markings) {
					if (m instanceof ScoredNamedEntity) {
						final ScoredNamedEntity sne = (ScoredNamedEntity) m;
						jsonObj.accumulate("mention",
								text.substring(sne.getStartPosition(), sne.getStartPosition() + sne.getLength()));
						jsonObj.accumulate("uri", sne.getUris().iterator().next());
						jsonObj.accumulate("score", sne.getConfidence());
					}
				}
				return retJSONObj.toString();

			} else {
				// Return HTML

				final boolean retHTMLPlain = true;
				final StringBuilder retSB = new StringBuilder();

				if (retHTMLPlain) {
					log("Processing PLAIN HTML to return");
					//
					int prevMarkStart = 0, prevMarkEnd = 0;
					for (Marking m : markings) {
						try {
							if (m instanceof ScoredNamedEntity) {
								final ScoredNamedEntity sne = (ScoredNamedEntity) m;
								final int markStart = sne.getStartPosition();
								final int markEnd = markStart + sne.getLength();
								// Get text between previous marking and current one
								retSB.append(text.substring(prevMarkEnd, markStart));
								final String uri = sne.getUris().iterator().next();
								// Add the Text for the marking as a link
								retSB.append(makeHTMLURL(text.substring(markStart, markEnd), uri));

								// retSB.append(sne.getConfidence());

								prevMarkStart = markStart;
								prevMarkEnd = markEnd;
							}
						} catch (Exception e) {
							log("ERROR - " + e.getMessage());
							log(m.toString());
						}
					}
					// Now from last marking to the end
					if (prevMarkEnd < text.length() && prevMarkEnd >= 0) {
						retSB.append(text.substring(prevMarkEnd));
					}

					log("processed markings...");
				} else {
					log("Processing Table HTML to return");

					if (markings.size() > 0) {
						retSB.append("<table style=\"width:100%\">");
						retSB.append("<tr>");
						retSB.append("<th>Mention</th>");
						retSB.append("<th>URI</th>");
						retSB.append("<th>Score</th>");
						retSB.append("</tr>");

						for (Marking m : markings) {
							if (m instanceof ScoredNamedEntity) {
								final ScoredNamedEntity sne = (ScoredNamedEntity) m;
								retSB.append("<tr>");
								retSB.append("<td>");
								retSB.append(text.substring(sne.getStartPosition(),
										sne.getStartPosition() + sne.getLength()));
								retSB.append("</td>");
								retSB.append("<td>");
								retSB.append(sne.getUris().iterator().next());
								retSB.append("</td>");
								retSB.append("<td>");
								retSB.append(sne.getConfidence());
								retSB.append("</td>");
								retSB.append("</tr>");
							}
						}
						retSB.append("</table>");
					} else {
						retSB.append("No results found.");
					}
				}
				log("Returning HTML String!");
				return retSB.toString();
			}
		} catch (InterruptedException ie) {
			getLogger().error("Exception while annotating.", ie);
			log("Error - returning empty string");
			return "";
		}
	}

	private Object makeHTMLURL(String text, String uri) {
		return "<a href=\"" + uri + "\">" + text + "</a>";
	}

	private EnumModelType findKG(String chosenKG) {
		EnumModelType KG = null;
		try {
			KG = EnumModelType.valueOf(chosenKG);
			return KG;
		} catch (IllegalArgumentException iae) {
			KG = null;
		}

		if ((KG = this.KGs.get(chosenKG)) != null) {
			return KG;
		}

		if (KG == null) {
			for (EnumModelType kg : EnumModelType.values()) {
				if (kg.findableName().toLowerCase().contains(chosenKG.toLowerCase())) {
					KG = kg;
					break;
				}
			}
		}
//		for (EnumModelType kg : EnumModelType.values()) {
//			if (kg.name().toLowerCase().contains(chosenKG.toLowerCase())) {
//				KG = kg;
//				break;
//			}
//		}

		return KG;
	}

	/**
	 * 
	 * @param inputText plain string text to be annotated
	 * @param markings  markings that are wanted
	 * @return annotations
	 * @throws InterruptedException
	 */
	private Collection<? extends Marking> annotatePlainText(final EnumModelType KG, final String inputText,
			final List<Marking> markings) throws InterruptedException {
		final List<Marking> retList = Lists.newArrayList();
		// new ScoredNamedEntity(startPosition, length, uris, confidence);
		// new Mention()... transform mention into a scored named entity

		Collection<Mention> mentions = linking(KG, inputText, markings);

		if (postRestrictToMarkings && markings != null) {
			// If we're just using markings, we need to readjust the offsets for the output
			// fixMentionMarkingOffsets(mentions, markings, origText);
			// Limit to just the wanted markings to increase precision...
			mentions = restrictMentionsToMarkings(mentions, markings, inputText);
		}

		// Transform mentions into GERBIL's wanted markings
		for (Mention mention : mentions) {
			retList.add(new ScoredNamedEntity(mention.getOffset(), mention.getOriginalMention().length(),
					mention.getAssignment().toString(), mention.getAssignment().getScore().doubleValue()));
		}

		return retList;
	}

	private Collection<Mention> linking(final EnumModelType KG, String text, List<Marking> markings)
			throws InterruptedException {
		Collection<Mention> mentions = null;

		Stopwatch.start(linking);
		Stopwatch.start(detectionWatch);

		// ########################################################
		// Mention Detection
		// ########################################################
		mentions = this.mdMap.get(KG.name()).detect(text);
		System.out.println("Found mentions:");
		System.out.println(mentions);
		if (preRestrictToMarkings && markings != null && markings.size() > 0) {
			mentions = restrictMentionsToMarkings(mentions, markings, text);
		}

		if (preRestrictToCapitalFirstLetter && markings != null && markings.size() != 0) {
			mentions = restrictMentionsToCapitalFirstLetter(mentions, markings, text);
		}

		getLogger().info("Detected " + mentions.size() + " mentions!");
		// ------------------------------------------------------------------------------
		// Change the offsets due to stopword-removal applied through InputProcessor
		// modifying the actual input, therewith distorting it greatly
		// ------------------------------------------------------------------------------
		// fixMentionOffsets(text, mentions);

		// getLogger().info("Detected [" + mentions.size() + "] mentions.");
		// getLogger().info("Detection duration: " +
		// Stopwatch.endDiffStart(detectionWatch) + " ms.");

		// ########################################################
		// Candidate Generation (update for mentions)
		// ########################################################
		Collections.sort((List<Mention>) mentions, offsetComparator);

		List<String> blacklistedCandidates = Lists.newArrayList();

		candidateGeneratorMap.get(KG.name()).generate(mentions);

		// displaySimilarities(mentions);

		if (REMOVE_OVERLAP) {
			removeOverlapping(mentions);
		}

		// ########################################################
		// Disambiguation
		// ########################################################
		Stopwatch.start(chooserWatch);
		disambiguatorMap.get(KG.name()).disambiguate(text, mentions);
		getLogger().info("Disambiguation duration: " + Stopwatch.endDiff(chooserWatch) + " ms.");
		Stopwatch.endOutput(getClass().getName());
		getLogger().info("#######################################################");
		// Display them
		// DetectionUtils.displayMentions(getLogger(), mentions, true);
		Stopwatch.endOutput(linking);

		// ########################################################
		// Pruning
		// ########################################################
		final String beforePruning = "Before Pruning(" + mentions.size() + "):" + mentions;
		final int priorPruningSize = mentions.size();
		mentions = this.prunerMap.get(KG.name()).prune(mentions);
		final int postPruningSize = mentions.size();
		if (priorPruningSize != postPruningSize) {
			getLogger().info(beforePruning);
			getLogger().info("[PRUNE] After Pruning(" + mentions.size() + "):" + mentions);
		} else {
			getLogger().info("[PRUNE] No pruning done (" + mentions.size() + ")");
		}

		return mentions;
	}

	private String markingsToText(final Document document, final List<Marking> markings) {
		final String text = document.getText();
		final StringBuilder sbInText = new StringBuilder();
		for (Marking mark : markings) {
			if (mark instanceof Span) {
				final Span span = ((Span) mark);
				sbInText.append(text.substring(span.getStartPosition(), span.getStartPosition() + span.getLength()));
				// Separate each by a space character
				sbInText.append(" ");
			}
		}
		return sbInText.toString();
	}

	private List<Marking> getSortedMarkings(Document document) {
		// Copy the markings so we can sort them
		List<Marking> markings = Lists.newArrayList(document.getMarkings());
		Collections.sort(markings, Comparators.markingsOffsetComp);
		return markings;
	}

	/**
	 * Fixes the mentions' offsets
	 * 
	 * @param text     input text
	 * @param mentions detected mentions
	 */
	private void fixMentionOffsets(final String text, final List<Mention> mentions) {
		Map<String, List<Integer>> multipleMentions = new HashMap<>();
		final String textLowercase = text.toLowerCase();
		for (Mention mention : mentions) {
			// If there's multiple, let the earlier offset be the earlier indexOf
			final String surfaceForm = mention.getOriginalMention().toLowerCase();
			final int index = textLowercase.indexOf(surfaceForm);
			if (textLowercase.indexOf(surfaceForm, index + 1) == -1) {
				// There is no other such surface form within the input, so just update with the
				// found index
				mention.updateOffset(index);
			} else {
				List<Integer> indices;
				if ((indices = multipleMentions.get(surfaceForm)) == null) {
					indices = Lists.newArrayList();
					multipleMentions.put(surfaceForm, indices);
				}
				indices.add(mention.getOffset());
			}
		}
		// Now go through the map for the multiple mentions
		for (Mention mention : mentions) {
			final String surfaceForm = mention.getOriginalMention().toLowerCase();
			final List<Integer> indices;
			if (surfaceForm.length() == 0) {
				continue;
			}

			if ((indices = multipleMentions.get(surfaceForm)) == null) {
				continue;
			}
			// Sorts it redundantly quite a few times... but myah, honestly, it's just a few
			// values
			Collections.sort(indices);

			final int rank = indices.indexOf(mention.getOffset());
			if (rank < 0) {
				getLogger().error("Failed logic on the offset rank[" + rank + "] for indices[" + indices + "]: mention["
						+ mention.getOriginalMention() + "]");
			}
			int currIndex = -1;
			for (int i = 0; i < rank;) {
				currIndex = textLowercase.indexOf(surfaceForm, currIndex + 1);
			}
			if (currIndex < 0) {
				getLogger().error("Failed logic on the mentions' offset updating");
			}
			mention.updateOffset(currIndex);
		}
	}

	/**
	 * Removes smallest overlapping mentions from the list of mentions
	 * 
	 * @param mentions list of mentions
	 */
	private void removeOverlapping(final Collection<Mention> inMentions) {
		// #####################################################################
		// Remove smallest conflicting mentions keeping just the longest ones
		// #####################################################################
		final List<Mention> mentions;
		if (inMentions instanceof List) {
			mentions = (List<Mention>) inMentions;
		} else {
			throw new IllegalArgumentException("Expected a list... too lazy to rewrite rn");
		}
		Set<Mention> toRemoveMentions = new HashSet<>();
		for (int i = 0; i < mentions.size(); ++i) {
			for (int j = i + 1; j < mentions.size(); ++j) {
				final Mention leftMention = mentions.get(i);
				final Mention rightMention = mentions.get(j);
				// If they conflict, add the shorter one to a list to be removed
				if (leftMention.overlaps(rightMention)) {
					// Remove smaller one
					final int mentionLenDiff = leftMention.getMention().length() - rightMention.getMention().length();
					final boolean sameFinalMention = rightMention.getMention().equals(leftMention.getMention());
					if (mentionLenDiff > 0) {
						// If they have the same mention, remove the longer one, otherwise the shorter
						// one
						if (sameFinalMention) {
							toRemoveMentions.add(leftMention);
						} else {
							// Left is bigger, so remove right
							toRemoveMentions.add(rightMention);
						}
					} else {
						// If they have the same mention, remove the longer one, otherwise the shorter
						// one -> idea is to remove noise/stopwords as much as possible
						if (sameFinalMention) {
							toRemoveMentions.add(rightMention);
						} else {
							// Right is bigger or EQUAL to left; if equal, it doesn't matter which...
							// -> remove left
							toRemoveMentions.add(leftMention);
						}
					}
				}
			}
		}
		final Set<String> removed = new HashSet<>();
		int counter = 0;
		for (Mention toRemove : toRemoveMentions) {
			mentions.remove(toRemove);
			removed.add(toRemove.getMention() + " - " + toRemove.getOriginalMention());
			counter++;
		}
		getLogger().info("Removed [" + counter + "/" + mentions.size() + "] mentions:'" + removed + "'");

	}

	@Override
	public String exec(Object... o) throws Exception {
		if (o != null && o.length > 0) {
			InputStream inputReader = null;
			for (Object obj : o) {
				if (obj instanceof InputStream) {
					inputReader = (InputStream) obj;
					break;
				}
			}
			if (inputReader != null) {
				return annotate(inputReader);
			}
		}
		return null;
	}

	@Override
	public boolean destroy() {
		// Tear down all the loaded data structures
		return false;
	}

	private Collection<Mention> restrictMentionsToMarkings(Collection<Mention> mentions, List<Marking> markings,
			String origText) {
		final Collection<Mention> retMentions = Lists.newArrayList();
		final Collection<Mention> copyMentions = Lists.newArrayList(mentions);
		for (Marking marking : markings) {
			final Span spanMarking = ((Span) marking);
			final int startPos = spanMarking.getStartPosition();
			if (startPos < 0) {
				continue;
			}
			final int length = spanMarking.getLength();
			// If a mention has the same startoffset and the same length, keep it
			final Iterator<Mention> it = copyMentions.iterator();
			while (it.hasNext()) {
				final Mention mention = it.next();
				if ((mention.getOffset() == startPos) && (length == mention.getOriginalMention().length())) {
					retMentions.add(mention);
					// Remove it since it gets added to the returned list
					it.remove();
				}
			}
		}
//		for (Mention m : copyMentions) {
//			getLogger().info("Not added to ResultSet (" + copyMentions.size() + "): " + m.getOffset() + "+["
//					+ m.getOriginalMention().length() + "] = " + m.getOriginalMention());
//		}
//
//		final boolean displayMarkingsDebug = false;
//		if (displayMarkingsDebug) {
//			for (Marking m : markings) {
//				final Span spanM = ((Span) m);
//				getLogger().info("Wanted markings: InputSet(" + markings.size() + "): " + spanM.getStartPosition()
//						+ "+[" + spanM.getLength() + "] = "
//						+ origText.substring(spanM.getStartPosition(), spanM.getStartPosition() + spanM.getLength()));
//			}
//			for (Mention m : retMentions) {
//				getLogger().info("Filtered FOUND mentions(" + mentions.size() + "->" + retMentions.size() + "):"
//						+ m.getOffset() + "+[" + m.getOriginalMention().length() + "] = " + m.getOriginalMention());
//			}
//		}
		return retMentions;
	}

	private Collection<Mention> restrictMentionsToCapitalFirstLetter(Collection<Mention> mentions,
			List<Marking> markings, String origText) {
		final List<Mention> retMentions = Lists.newArrayList();
		final List<Mention> copyMentions = Lists.newArrayList(mentions);
		for (Marking marking : markings) {
			final Span spanMarking = ((Span) marking);
			final int startPos = spanMarking.getStartPosition();
			if (startPos < 0) {
				continue;
			}
			final int length = spanMarking.getLength();
			// If a mention has the same startoffset and the same length, keep it
			final Iterator<Mention> it = copyMentions.iterator();
			while (it.hasNext()) {
				final Mention mention = it.next();
				if (Character.isUpperCase(mention.getOriginalMention().charAt(0))) {
					retMentions.add(mention);
				}
			}
		}
		return retMentions;
	}

	private void fixMentionMarkingOffsets(final List<Mention> mentions, final List<Marking> markings,
			final String origText) {
		final Map<String, List<Mention>> mentionsMap = new HashMap<>();
		// Adds mentions to a map by the original text surface form
		for (Mention mention : mentions) {
			List<Mention> mentionsSameSF;
			if ((mentionsSameSF = mentionsMap.get(StringUtils.stripEnd(mention.getOriginalMention(), null))) == null) {
				mentionsSameSF = Lists.newArrayList();
				mentionsMap.put(StringUtils.stripEnd(mention.getOriginalMention(), null), mentionsSameSF);
			}
			mentionsSameSF.add(mention);
		}

		for (Map.Entry<String, List<Mention>> e : mentionsMap.entrySet()) {
			int textIndex = 0;
			int counter = 0;
			while (// (textIndex < (origText.length() - 1)) &&
			(textIndex = origText.indexOf(e.getKey(), textIndex)) != -1) {
				if (e.getValue().size() <= counter) {
					// this can be the case when only one mention contains a marking (within a
					// string containing another such mention)

					// Incrementing counter for the ensuing error message logic
					counter++;
					break;
				}
				e.getValue().get(counter).setOffset(textIndex);
				// Advance it by one so it's not forever stuck on the same one
				textIndex++;
				counter++;
			}
			if (counter < e.getValue().size()) {
				// Didn't work out, possibly due to the combined detected mention not existing
				// in the original text, e.g. when only markings are used, it concatenates the
				// markings into a text... which can be meh
				getLogger().error("Missed mentions... Counter(" + counter + "): Value(" + e.getValue()
						+ "): Orig. Mention(" + e.getKey() + "): Text:" + origText);
			}
		}
	}

}
