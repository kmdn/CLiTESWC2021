package linking.mentiondetection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

import structure.config.constants.Numbers;
import structure.datatypes.EnumDetectionType;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.datatypes.TextOffset;

/**
 * Centralised processing class for input strings (also used for data structure
 * text processing
 * 
 * @author Kris
 *
 */
public class InputProcessor {
	final Collection<String> blacklist;

	public InputProcessor(final Collection<String> blacklist) {
		this.blacklist = blacklist;
	}

	public static void main(String[] args) {
		final String[] inStrings = new String[] { // "", " ", " I have ",
				"I have   a cat, !!that I like- playing with  " };
		// final EnumDetectionType detectionMode =
		// EnumDetectionType.BOUND_DYNAMIC_WINDOW;
		for (String inString : inStrings) {
			for (EnumDetectionType detectionMode : EnumDetectionType.values()) {
				System.out.println(detectionMode.name());
				final List<Mention> mentions = new InputProcessor(null).createMentions(inString, null, detectionMode);
				System.out.println(mentions.size() + "x Mentions");
				for (Mention m : mentions) {
					// System.out.println(m.getOriginalMention() + " - " + m.getOffset());
					// System.out.println("[" + m.getOriginalMention() + "]");
					System.out.println("[" + m.getOriginalWithoutStopwords() + "]");
				}
			}
			System.out.println("#################");
			System.out.println("inString:");
			System.out.println("[" + inString + "]");
			System.out.println("#######END#######");
		}
	}

	private static final String tokenSeparator = " ";// space
	// Greedy from the front, reluctant from the back
	// Take all the spaces away in the front
	private static final Pattern spacePunctPattern = Pattern.compile("(\\p{Punct}+?|\\p{Space}++)");
	// Perfect from the front, but takes too much in the back (aka. should stop
	// sooner): .compile("(\\p{Punct}|\\p{Space})++");

	public List<Mention> createMentions(final String input, final String source,
			final EnumDetectionType detectionMode) {
		return createMentions(input, source, detectionMode, this.blacklist);
	}

	/**
	 * Used to create mentions without too much trouble in regards to offsets when
	 * removing stopwords and whatnot
	 * 
	 * @param input
	 * @return
	 */
	public static List<Mention> createMentions(final String input, final String source,
			final EnumDetectionType detectionMode, final Collection<String> blacklist) {
		final List<Mention> retList = Lists.newArrayList();
		if (input == null || input.length() == 0) {
			return retList;
		}
		final List<TextOffset> tokens = process(input, detectionMode);
		for (TextOffset token : tokens) {
			final Mention mention = createMention(token.text, token.offset, blacklist);
			if (mention != null) {
				retList.add(mention);
			}
		}
		return retList;
	}

	/**
	 * Takes an original piece of text from the input and applies preprocessing
	 * measures to get a potentially different form of it (e.g. without unnecessary
	 * words etc), creating a mention that will attempt to be matched through
	 * mention detection
	 * 
	 * @param original   original piece of text from input
	 * @param startIndex which index it starts at
	 * @param blacklist  list of words that are to be removed from the
	 * @return
	 */
	private static Mention createMention(String original, int startIndex, Collection<String> blacklist) {
		// Preprocess the original input to get a version without stopwords
		final String processedInput = combineProcessedInput(processAndRemoveStopwords(original, blacklist));
		if (processedInput == null || processedInput.length() == 0) {
			return null;
		}
		return new Mention((String) null, (PossibleAssignment) null, startIndex, 0d, original, processedInput);
	}

	/**
	 * Turns a string input into an array of tokens
	 * 
	 * @param input
	 * @return
	 */
	public static String[] processToSingleWords(final String input) {
		final List<TextOffset> tokens = process(input, EnumDetectionType.SINGLE_WORD);
		final String[] retArr = new String[tokens.size()];
		for (int i = 0; i < tokens.size(); ++i) {
			final TextOffset token = tokens.get(i);
			retArr[i] = token.text;
		}
		return retArr;
	}

	/**
	 * Call to {@link #process(String, EnumDetectionType)} with
	 * EnumDetectionType.BOUND_DYNAMIC_WINDOW as the default detection mode
	 * 
	 * @param input
	 * @param detectionMode
	 * @return
	 */
	public static List<TextOffset> process(final String input) {
		return process(input, EnumDetectionType.BOUND_DYNAMIC_WINDOW);
	}

	/**
	 * Centralised splitting method (without stopword removal)
	 * 
	 * @param input
	 * @return
	 */
	public static List<TextOffset> process(final String input, final EnumDetectionType detectionMode) {
		// final String retStr = input.replaceAll("\\p{Punct}", "").toLowerCase();

		final Set<TextOffset> retSet = new HashSet<>();// Lists.newArrayList();
		// Splits on a space or punctuation
		final Matcher matcher = spacePunctPattern.matcher(input);

		final TreeSet<Integer> spacedIndicesUnique = new TreeSet<Integer>();
		// Add initial index so substring starts from beginning of text
		int counter = 0;
		spacedIndicesUnique.add(0);
		while (matcher.find()) {
			final String match = matcher.group();
			if (match.length() == 0) {
				continue;
			}
			final int groupLen = match.length();
			spacedIndicesUnique.add(matcher.start() + groupLen);
		}
		spacedIndicesUnique.add(input.length() + 1);

		final List<Integer> spacedIndices = Lists.newArrayList(spacedIndicesUnique);

//		System.out.println("Input:" + input);
//		System.out.println("Indices:" + spacedIndices);
//		System.out.println("Counter (same as indices.size()): " + counter);
		final int MIN_MENTION_LENGTH = Numbers.MENTION_MIN_SIZE.val.intValue();
		// Minimum additional word length required to be considered as a mention
		final int MIN_LENGTH_CONNECTING_WORD = Numbers.MENTION_MIN_WORD_VARIATION.val.intValue();

		switch (detectionMode) {
		case BOUND_DYNAMIC_WINDOW:
			final int windowSize = Numbers.MENTION_DETECTION_WINDOW_SIZE.val.intValue();
			final StringBuilder sbConcat = new StringBuilder();
			for (int i = 0; i < spacedIndices.size(); ++i) {
				sbConcat.setLength(0);
				int prevIndex = spacedIndices.get(i);
				int startIndex = spacedIndices.get(i);
				for (int j = 1; j <= windowSize && ((i + j) < spacedIndices.size()); ++j) {
					final int subStartIndex = prevIndex, endIndex = spacedIndices.get(i + j) - 1;
//					System.out.println("i(" + i + ")/j(" + j + "): START(" + startIndex + ")/END(" + endIndex + ")");
					final String subStr = input.substring(subStartIndex, endIndex).replaceAll("\\s+$", "");
					sbConcat.append(subStr);
					if (subStr.length() >= MIN_LENGTH_CONNECTING_WORD) {
						final String mentionText = sbConcat.toString();
						if (mentionText.length() >= MIN_MENTION_LENGTH) {
							retSet.add(new TextOffset().text(mentionText).offset(startIndex));
						}
					}
					prevIndex = endIndex;
				}
			}
			sbConcat.setLength(0);
//			for (int i = 0; i < spacedIndices.size(); ++i) {
//				for (int j = 1; j <= windowSize && ((i + j) < spacedIndices.size()); ++j) {
//					final int startIndex = spacedIndices.get(i), endIndex = spacedIndices.get(i + j) - 1;
//					System.out.println("i(" + i + ")/j(" + j + "): START(" + startIndex + ")/END(" + endIndex + ")");
//					retList.add(createMention(input.substring(startIndex, endIndex), startIndex, blacklist));
//				}
//			}
			break;
		case SINGLE_WORD:
			// Just Single words
			for (int i = 0; i < spacedIndices.size() - 1; ++i) {
				final int startIndex = spacedIndices.get(i), endIndex = spacedIndices.get(i + 1) - 1;
				final String mentionText = input.substring(startIndex, endIndex);
				if (mentionText.length() > MIN_MENTION_LENGTH && mentionText.length() > MIN_LENGTH_CONNECTING_WORD) {
					retSet.add(new TextOffset().text(mentionText).offset(startIndex));
				}
			}
			break;
		case UNBOUND_DYNAMIC_WINDOW:
			/*
			 * Example: Input: I have a cat Processed as: I; I have; I have a; I have a cat;
			 * have; have a; have a cat; ...
			 */
			int prevLength = 0;
			for (int i = 0; i < spacedIndices.size(); ++i) {
				for (int j = i + 1; j < spacedIndices.size(); ++j) {
					final int startIndex = spacedIndices.get(i), endIndex = spacedIndices.get(j) - 1;
					final String mentionText = input.substring(startIndex, endIndex);
					if (mentionText.length() > MIN_MENTION_LENGTH
							&& (mentionText.length() - prevLength) > MIN_LENGTH_CONNECTING_WORD) {
						retSet.add(new TextOffset().text(mentionText).offset(startIndex));
					}
					prevLength = mentionText.length();
				}
			}
			break;
		case UNBOUND_DYNAMIC_WINDOW_STRICT_MULTI_WORD:
//			// Multi-words (excluding single words)
//			combinedWords.setLength(0);
//			stringPos = 0;
//			for (int i = 0; i < words.length; ++i) {
//				combinedWords.append(words[i]);
//				int subPos = stringPos;
//				for (int j = i + 1; j < words.length; ++j) {
//					combinedWords.append(tokenSeparator + words[j]);
//					execFind(executor, mentions, doneCounter, combinedWords.toString(), source, threshold,
//							stringPos);
//					subPos = tokenSeparator.length() + words[j].length();
//				}
//				stringPos += words[i].length();
//				combinedWords.setLength(0);
//			}
			break;

		default:
			break;
		}

		// return retList.toArray(new String[retList.size()]);
		// System.out.println("RetSet: " + Arrays.toString(new
		// ArrayList<>(retSet).toArray()));
		return new ArrayList<>(retSet);
		// return retStr.split("\\p{Space}+");// POSIX class
	}

	/**
	 * Centralised splitting method (with stopword removal, based on the
	 * instance-defined blacklist)<br>
	 * Note: Relies on {@link #process(String)}
	 * 
	 * @param input
	 * @return split tokens excluding stopwords
	 */
	public String[] processAndRemoveStopwords(final String input) {
		return processAndRemoveStopwords(input, blacklist);
	}

	/**
	 * Centralised splitting and stopword removal method (with the passed blacklist)
	 * 
	 * @param input     input to be split and stopwords removal applied on
	 * @param blacklist list of stopwords to (potentially) remove from input
	 * @return split tokens excluding any defined stopwords
	 */
	public static String[] processAndRemoveStopwords(final String input, final Collection<String> blacklist) {
		final String[] inputArr = processToSingleWords(input);// space splitting etc.
		final List<String> ret = Lists.newArrayList();
		if (blacklist == null) {
			// No blacklist, so jump out with the normal-processed words
			return inputArr;
		}
		for (String str : inputArr) {
			if (!blacklist.contains(str) && !blacklist.contains(str.toLowerCase())) {
				ret.add(str);
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	/**
	 * Centralised way to combine split tokens into a single string (using a space
	 * as a delimiter and StringBuilder for concatenation)
	 * 
	 * @param inputTokens tokens to be concatenated
	 * @return concatenated tokens
	 */
	public static String combineProcessedInput(final String[] inputTokens) {
		if (inputTokens == null || inputTokens.length == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder(inputTokens[0]);
		for (int i = 1; i < inputTokens.length; ++i) {
			sb.append(" ");
			sb.append(inputTokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Treats the key as it would any other input and saves it to a new HashMap with
	 * the associated previous key value (meant for use for [Keys=surfaceForm;
	 * Values=entities]<br>
	 * Note: Does not modify passed Map instance
	 * 
	 * @param map
	 * @return
	 */
	public static <V> Map<String, Collection<V>> processCollection(final Map<String, Collection<V>> map,
			final InputProcessor inputProcessor) {
		final HashMap<String, Collection<V>> ret = new HashMap<>(map.keySet().size());
		for (Map.Entry<String, Collection<V>> e : map.entrySet()) {
			String key = null;
			String[] toProcessArr = null;
			try {
				toProcessArr = inputProcessor.processAndRemoveStopwords(e.getKey());
				key = InputProcessor.combineProcessedInput(toProcessArr);
				final Collection<V> val = ret.get(key);
				if (val == null) {
					ret.put(e.getKey(), e.getValue());
				} else {
					val.addAll(e.getValue());
				}
			} catch (NullPointerException npe) {
				throw npe;
			}
		}
		return ret;
	}

	/**
	 * Takes same comparator as the passed one and uses it for the creation of a new
	 * treeset which contains the sanitized versions of the contents of the
	 * previously-passed one.<br>
	 * Note: Does not change passed SortedSet instance
	 * 
	 * @param inputSet
	 * @return
	 */
	public static TreeSet<String> processCollection(final SortedSet<String> inputSet,
			final InputProcessor inputProcessor) {
		final TreeSet<String> ret = new TreeSet<String>(inputSet.comparator());
		inputSet.forEach(item -> {
			if (item != null) {
				ret.add(InputProcessor.combineProcessedInput(inputProcessor.processAndRemoveStopwords(item)));
			}
		});
		return ret;
	}

	/**
	 * Sanitizes passed list items<br>
	 * Note: does not modify passed List instance
	 * 
	 * @param inputList
	 * @return
	 */
	public static ArrayList<String> processCollection(final List<String> inputList,
			final InputProcessor inputProcessor) {
		final ArrayList<String> ret = Lists.newArrayList();
		inputList.forEach(item -> {
			if (item != null) {
				ret.add(InputProcessor.combineProcessedInput(inputProcessor.processAndRemoveStopwords(item)));
			}
		});
		return ret;
	}
}
