package linking.mentiondetection.exact;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;

import linking.mentiondetection.AbstractMentionDetector;
import linking.mentiondetection.InputProcessor;
import structure.config.constants.Numbers;
import structure.datatypes.EnumDetectionType;
import structure.datatypes.Mention;
import structure.utils.Loggable;

public class MentionDetectorMap extends AbstractMentionDetector implements Loggable {
	private final Set<String> keys = new HashSet<>();
	private final Set<String> processedKeys = new HashSet<>();
	private final String tokenSeparator = " ";// space
	private final EnumDetectionType detectionType = EnumDetectionType.BOUND_DYNAMIC_WINDOW;
	private final String mentionLock = "mentionsList";
	private final InputProcessor inputProcessor;
	private final DualHashBidiMap<String, String> originalMentions = new DualHashBidiMap<>();

	public MentionDetectorMap(final Map<String, Collection<String>> map, final InputProcessor inputProcessor) {
		Set<String> inKeys = map.keySet();
		// Adds everything in lower case

		for (String key : inKeys) {
			final String surfaceForm = InputProcessor.combineProcessedInput(
					// InputProcessor.processToSingleWords(key)
					inputProcessor.processAndRemoveStopwords(key));
			if (surfaceForm != null && surfaceForm.length() > 0) {
				final String processedSF = processKey(surfaceForm);
				this.keys.add(key);
				this.processedKeys.add(processedSF);
				// put(Angelina, angelina)
				originalMentions.put(key, processedSF);
			}
		}
		this.inputProcessor = inputProcessor;
	}

	/**
	 * Short-hand call to {@link #detect(String, String)} with {@param source=null}
	 * 
	 * @param input input text/corpus to detect mentions from
	 */
	@Override
	public List<Mention> detect(String input) {
		return detect(input, null);
	}

	/**
	 * @param input  input text/corpus to detect mentions from
	 * @param source where this text comes from or what it is linked to
	 */
	@Override
	public List<Mention> detect(final String input, final String source) {
		try {
			final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(Numbers.MENTION_DETECTION_THREAD_AMT.val.intValue());
			AtomicInteger doneCounter = new AtomicInteger(0);

			final List<Mention> mentions = inputProcessor.createMentions(input, source, detectionType);
			for (Mention mention : mentions) {
				execFind(executor, mention, doneCounter);
			}

			// No more tasks will be added
			executor.shutdown();
			do {
				// No need for await termination as this is pretty much it already...
				Thread.sleep(50);
				// getLogger().debug("Finished executing: " + doneCounter.get() + "
				// processes.");
			} while (!executor.isTerminated());
			// Shouldn't wait at all generally, but in order to avoid unexpected behaviour -
			// especially relating to logic changes on the above busy-waiting loop
			final boolean terminated = executor.awaitTermination(10L, TimeUnit.MINUTES);
			if (!terminated) {
				getLogger().error("Executor has not finished terminating");
			}

			// Removes all Mention objects that have no associated mention
			final Iterator<Mention> itMention = mentions.iterator();
			while (itMention.hasNext()) {
				final Mention mention = itMention.next();
				if (mention == null || mention.getMention() == null || mention.getMention().length() == 0) {
					itMention.remove();
				}
			}

			List<Mention> retMentions = mentions;
			// Removes longer mentions
			final boolean reduceMentions = false;
			if (reduceMentions) {
				final boolean removeStartOffset = true, removeEndOffset = true;
				retMentions = removeLongerCollisions(mentions, removeStartOffset, removeEndOffset);
//				System.out.println("Prior(" + mentions.size() + ") to removing:" + mentions);
//				System.out.println("After(" + retMentions.size() + ") removing:" + retMentions);
			}
			// return mentions;
			return retMentions;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Submits a find task to the executor, adding the result to the mentions list,
	 * incrementing the 'done' counter<br>
	 * <b>Note</b>: Mentions list MUST be synchronized (e.g. through
	 * Collections.synchronizedList(List))
	 * 
	 * @param executor
	 * @param mention
	 * @param doneCounter
	 */
	private void execFind(final ThreadPoolExecutor executor, final Mention mention, final AtomicInteger doneCounter) {
		executor.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				final String toFindText = mention.getOriginalWithoutStopwords();
				if (toFindText != null && toFindText.length() != 0) {
					final boolean found = find(toFindText, keys);
					if (found) {
						// Found the mention as-is within the object

						// mention.setMention(mention.getOriginalMention());
						// System.out.println("Found: ToFind(" + toFindText + "), Mapped("+
						// originalMentions.getKey(toFindText) + ")");
						// mention.setMention(originalMentions.getKey(toFindText));
						mention.setMention(toFindText);
						mention.setDetectionConfidence(1d);
					} else {
						// Cannot find the given text as-is, so let's try to potentially recover e.g. by
						// taking a processed version of it
						final String processedSF = processKey(toFindText);
						final boolean foundProcessed = find(processedSF, processedKeys);
						if (foundProcessed) {
							mention.setMention(originalMentions.getKey(processedSF));
							mention.setDetectionConfidence(1d);
						}
					}
				}
				return doneCounter.incrementAndGet();
			}
		});
	}

	private boolean find(final String input) {
		return find(input, this.keys);
	}

	/**
	 * Finds a mention for a given input token
	 * 
	 * @param input  token or word
	 * @param source what entity this word is linked to
	 * 
	 * @return mention with the closest possible mate
	 * 
	 */
	public boolean find(final String input, final Set<String> set) {
		/*
		 * System.out.println("Could not match w/:" + input); final int showAmt = 100;
		 * int showCounter = 0; System.out.println("Number of keys:" +
		 * this.keys.size()); for (String key : this.keys) { if (showCounter++ <
		 * showAmt) { System.out.println("Key:'" + key.toLowerCase() + "'"); } }
		 */
		return set.contains(input);
	}

	/**
	 * Mention detector-specific processing, e.g. everything to lowercase or
	 * uppercase etc.
	 * 
	 * @param input
	 * @return
	 */
	private String processKey(final String input) {
		return StringUtils.strip(input.toLowerCase());
	}

	@Override
	public boolean init() {
		return true;
	}
}
