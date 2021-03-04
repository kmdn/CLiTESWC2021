package api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.ScoredNamedEntity;
import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.internal.Lists;

import linking.candidategeneration.CandidateGeneratorMap;
import linking.disambiguation.DisambiguatorAgnos;
import linking.mentiondetection.InputProcessor;
import linking.mentiondetection.StopwordsLoader;
import linking.pruning.MentionPruner;
import linking.pruning.ThresholdPruner;
import structure.config.constants.Comparators;
import structure.config.constants.EnumEmbeddingMode;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.interfaces.Executable;
import structure.interfaces.pipeline.CandidateGenerator;
import structure.interfaces.pipeline.MentionDetector;
import structure.utils.DetectionUtils;
import structure.utils.Stopwatch;
import structure.utils.TextUtils;

/**
 * Class handling annotation tasks for GERBIL
 * 
 * @author Kristian Noullet
 *
 */
public class NIFAPIAnnotator implements Executable {

	private final EnumModelType KG;

	private final String chooserWatch = "Scorer (Watch)";
	private final String detectionWatch = MentionDetector.class.getName();
	private final String linking = "Linking (Watch)";
	private final boolean REMOVE_OVERLAP = false;

	private static final boolean detailed = false;
	private static int docCounter = 0;
	private final boolean preRestrictToMarkings = false;
	private final boolean postRestrictToMarkings = true;
	private final boolean preRestrictToCapitalFirstLetter = true;
	// No touchy
	private Boolean init = false;
	private final Comparator<Mention> offsetComparator = Comparators.mentionOffsetComp;

	private Set<String> stopwords = null;
	private MentionDetector md = null;
	private CandidateGenerator candidateGenerator = null;
	private DisambiguatorAgnos disambiguator = null;
	private MentionPruner pruner = null;
	private final EnumEmbeddingMode embeddingMode;

	public NIFAPIAnnotator(final EnumModelType KG) {
		this(KG, EnumEmbeddingMode.DEFAULT.val);
	}

	public NIFAPIAnnotator(final EnumModelType KG, final EnumEmbeddingMode embeddingMode) {
		this.KG = KG;
		this.embeddingMode = embeddingMode;
	}

	@Override
	@SuppressWarnings("unused")
	public synchronized void init() {
		synchronized (this.init) {
			if (this.init)
				return;
		}
		// Load all the necessary stuff
		// such as embeddings, LSH sparse vectors and hashes
		try {
			getLogger().info("Initializing Framework Structures");
			getLogger().info("Loading mention possibilities...");
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			this.stopwords = stopwordsLoader.getStopwords();
			final Map<String, Collection<String>> map = DetectionUtils.loadSurfaceForms(this.KG, stopwordsLoader);
			final InputProcessor inputProcessor = new InputProcessor(stopwords);
			// ########################################################
			// Mention Detection
			// ########################################################
			this.md = DetectionUtils.setupMentionDetection(KG, map, inputProcessor);

			// ########################################################
			// Candidate Generator
			// ########################################################
			this.candidateGenerator = new CandidateGeneratorMap(map);
			Stopwatch.endOutputStart(getClass().getName());
			// Initialise AssignmentChooser
			Stopwatch.start(chooserWatch);

			this.disambiguator = new DisambiguatorAgnos(this.KG, this.embeddingMode);
			Stopwatch.endOutput(chooserWatch);
			this.pruner = new ThresholdPruner(1.0d);
			init = true;
		} catch (Exception exc) {
			getLogger().error("Exception during init", exc);
		}
	}

	/**
	 * Do not change unless you have changed the call on the WEB API
	 * 
	 * @param inputStream NIFInputStream
	 * @return
	 */
	public String annotate(final InputStream inputStream) {
		if (false) {
			try (final BufferedReader brIn = new BufferedReader(new InputStreamReader(inputStream))) {
				String line = null;
				getLogger().info("Input from GERBIL - START:");
				while ((line = brIn.readLine()) != null) {
					getLogger().error(line);
				}
				getLogger().info("Input from GERBIL - END");
			} catch (IOException e) {
				getLogger().error("IOException", e);
			}
			return "";
		}

		// 1. Generate a Reader, an InputStream or a simple String that contains the NIF
		// sent by GERBIL
		// 2. Parse the NIF using a Parser (currently, we use only Turtle)
		final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		final Document document;
		try {
			document = parser.getDocumentFromNIFStream(inputStream);
		} catch (Exception e) {
			getLogger().error("Exception while reading request.", e);
			return "";
		}

		return annotateDocument(document);
	}

	public String annotateNIF(final String nifInput) {
		// Parse the NIF using a Parser (currently, we use only Turtle)
		final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		final Document document;
		try {
			document = parser.getDocumentFromNIFString(nifInput);
		} catch (Exception e) {
			getLogger().error("Exception while reading request.", e);
			return "";
		}
		return annotateDocument(document);
	}

	public String annotateDocument(final Document nifDocument) {
		// In case it hasn't been initialised yet
		init();
		final int MIN_MARKINGS = 1;

		final String text = nifDocument.getText();

		// Ordered markings required to reconciliate the markings' detected mentions'
		// offsets
		final List<Marking> orderedMarkings = getSortedMarkings(nifDocument);

		getLogger().info("Input [plain text]:" + TextUtils.smallText(text));

		// 3. use the text and maybe some Markings sent by GERBIL to generate your
		// Markings
		// (a.k.a annotations) depending on the task you want to solve
		// 4. Add your generated Markings to the document
		try {
			nifDocument.setMarkings(new ArrayList<Marking>(annotatePlainText(text, orderedMarkings)));
			// Not really necessary methinks?
			// nifDocument.setText(text);
			// nifDocument.setDocumentURI("http://informatik.uni-freiburg.de/document#" +
			// docCounter++);
		} catch (InterruptedException ie) {
			getLogger().error("Exception while annotating.", ie);
			return "";
		}
		// 5. Generate a String containing the NIF and send it back to GERBIL
		final TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
		final String retNifDocumentStr = creator.getDocumentAsNIFString(nifDocument);
		return retNifDocumentStr;
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
	 * 
	 * @param inputText plain string text to be annotated
	 * @param markings  markings that are wanted
	 * @return annotations
	 * @throws InterruptedException
	 */
	private Collection<? extends Marking> annotatePlainText(final String inputText, final List<Marking> markings)
			throws InterruptedException {
		final List<Marking> retList = Lists.newArrayList();
		// new ScoredNamedEntity(startPosition, length, uris, confidence);
		// new Mention()... transform mention into a scored named entity

		Collection<Mention> mentions = linking(inputText, markings);

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

	private Collection<Mention> restrictMentionsToMarkings(Collection<Mention> mentions, List<Marking> markings,
			String origText) {
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

	private Collection<Mention> linking(final String text, List<Marking> markings) throws InterruptedException {
		Collection<Mention> mentions = null;

		Stopwatch.start(linking);
		Stopwatch.start(detectionWatch);

		// ########################################################
		// Mention Detection
		// ########################################################
		mentions = md.detect(text);

		if (preRestrictToMarkings && markings != null && markings.size() != 0) {
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

		candidateGenerator.generate(mentions);

		// displaySimilarities(mentions);

		if (REMOVE_OVERLAP) {
			removeOverlapping(mentions);
		}

		// ########################################################
		// Disambiguation
		// ########################################################
		Stopwatch.start(chooserWatch);
		disambiguator.disambiguate(text, mentions);
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
		mentions = this.pruner.prune(mentions);
		final int postPruningSize = mentions.size();
		if (priorPruningSize != postPruningSize) {
			getLogger().info(beforePruning);
			getLogger().info("[PRUNE] After Pruning(" + mentions.size() + "):" + mentions);
		} else {
			getLogger().info("[PRUNE] No pruning done (" + mentions.size() + ")");
		}

		return mentions;
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
	private void removeOverlapping(Collection<Mention> inMentions) {
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

}
