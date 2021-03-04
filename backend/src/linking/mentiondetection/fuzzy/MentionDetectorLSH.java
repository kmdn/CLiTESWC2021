package linking.mentiondetection.fuzzy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;

import info.debatty.java.lsh.LSHMinHash;
import linking.mentiondetection.AbstractMentionDetector;
import linking.mentiondetection.InputProcessor;
import structure.config.constants.FilePaths;
import structure.config.constants.Numbers;
import structure.config.constants.Strings;
import structure.config.kg.EnumModelType;
import structure.datatypes.EnumDetectionType;
import structure.datatypes.Mention;
import structure.datatypes.lsh.LSHSparseVector;
import structure.datatypes.lsh.MinHashObject;
import structure.utils.FuzzyUtils;
import structure.utils.Loggable;
import structure.utils.Stopwatch;

public class MentionDetectorLSH extends AbstractMentionDetector implements Loggable {
	private final InputProcessor inputProcessor;
	private boolean setup = false;
	private final AtomicInteger collisionCounter = new AtomicInteger(0);
	// Trigrams of all possible surface forms
	private final TreeMap<String, Integer> ngrams = new TreeMap<>();
	// Keyset of the linking map - all possible surface forms
	private final TreeSet<String> surface_forms = new TreeSet<String>();
	private final String tokenSeparator = " ";// space
	// Attention: to get relevant results, the number of elements per bucket
	// should be at least 100
	// Note that changing the seed, bands and/or buckets means all signatures must
	// be recomputed!

	// Constant / Default values
	private final int seed = 1337;
	private final static int bandsDefaultValue = Numbers.LSH_BANDS.val.intValue();
	private final static int bucketsDefaultValue = Numbers.LSH_BUCKETS.val.intValue();
	// LSH Variables
	private int bands = bandsDefaultValue;
	private int buckets = bucketsDefaultValue;
	private LSHMinHash lsh = null;
	private int[][] hashes = null;
	private LSHSparseVector<Boolean>[] document_vectors_sparse;
	// Text processing variables
	private final EnumDetectionType detectionType;
	private final int n_gram_length = 3;
	// Precomputed data variables
	private final String outDocVectorsEntries;
	private final String outHashes;
	private final String docArraySplitStr = Strings.LSH_HASH_DELIMITER.val;
	private final EnumModelType KG;
	// JS similarity min. threshold
	private final double threshold;
	// Whether or not data was loaded
	private boolean loaded = false;
	private final String mentionLock = "mentionsList";

	// #################################
	// ########## CONSTRUCTOR ##########
	// #################################
	public MentionDetectorLSH(final EnumModelType KG, final InputProcessor inputProcessor) {
		this(KG, 0.7, inputProcessor);
	}

	/**
	 * Calls {@link #MentionDetectorLSH(Map, double, int, int)} with the given map,
	 * threshold and the bands and buckets default values
	 * 
	 */
	public MentionDetectorLSH(final EnumModelType KG, final double threshold, final InputProcessor inputProcessor) {
		this(KG, threshold, bandsDefaultValue, bucketsDefaultValue, inputProcessor);
	}

	/**
	 * 
	 * @param map            Map where keys represent the text occurrences that are
	 *                       possible to be detected from input text
	 * @param threshold      similarity threshold for fuzzy matching
	 * @param value          either buckets or bands (other one will get its default
	 *                       value)
	 * @param bucketsVsBands TRUE means that passed VALUE parameter represents the
	 *                       number of buckets, FALSE means that it represents the
	 *                       number of bands
	 */
	public MentionDetectorLSH(final EnumModelType KG, final double threshold, final int value,
			final boolean bucketsVsBands, final InputProcessor inputProcessor) {
		this(KG, threshold, bucketsVsBands ? bandsDefaultValue : value, bucketsVsBands ? value : bandsDefaultValue,
				inputProcessor);
	}

	/**
	 * 
	 * @param map       Contains all possible String occurrences along with the
	 *                  associated resources. Only its keyset will be used in
	 *                  practice (as this is simple mention detection)
	 * @param threshold Similarity threshold that should be passed for fuzzy matches
	 * @param bands     how many bands LSH should be computed with
	 * @param buckets   how many buckets LSH should be computed with
	 */
	public MentionDetectorLSH(final EnumModelType KG, final double threshold, final int bands, final int buckets,
			final InputProcessor inputProcessor) {
		this(KG, threshold, bands, buckets, EnumDetectionType.BOUND_DYNAMIC_WINDOW, inputProcessor);
	}

	/**
	 * 
	 * @param map           Contains all possible String occurrences along with the
	 *                      associated resources. Only its keyset will be used in
	 *                      practice (as this is simple mention detection)
	 * @param threshold     Similarity threshold that should be passed for fuzzy
	 *                      matches
	 * @param bands         how many bands LSH should be computed with
	 * @param buckets       how many buckets LSH should be computed with
	 * @param detectionType which type of tokenization should be applied to the
	 *                      input text
	 */
	public MentionDetectorLSH(final EnumModelType KG, final double threshold, final int bands, final int buckets,
			EnumDetectionType detectionType, InputProcessor inputProcessor) {
		this.detectionType = detectionType;
		this.threshold = threshold;
		this.bands = bands;
		this.buckets = buckets;
		this.KG = KG;
		this.outHashes = FilePaths.FILE_LSH_HASHES.getPath(KG);
		this.outDocVectorsEntries = FilePaths.FILE_LSH_DOCUMENT_VECTORS_SPARSE.getPath(KG);
		this.inputProcessor = inputProcessor;
	}

	/**
	 * Persists all necessary data structures to the filesystem so that they can be
	 * loaded back with load()
	 * 
	 * @throws IOException
	 */
	public void backup() throws IOException {
		// Output all sparse vector entries
		try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outDocVectorsEntries)))) {
			for (LSHSparseVector<Boolean> vec : document_vectors_sparse) {
				bwOut.write(vec.getEntries().toString());
				bwOut.newLine();
			}
		}
		// Output all the hashes
		try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outHashes)))) {
			Iterator<String> docIterator = surface_forms.iterator();
			for (int[] hash : hashes) {
				bwOut.write(docIterator.next());
				bwOut.write(docArraySplitStr);
				bwOut.write(Arrays.toString(hash));
				bwOut.newLine();
			}
		}
	}

	/**
	 * Loads all required data structures from backed up files.<br>
	 * Note that if any of the following has been done, setup() must be called
	 * appropriately (followed by backup()) before this method can be called:<br>
	 * 1) changing buckets<br>
	 * 2) bands(bins) <br>
	 * 3) underlying knowledge base's surface forms<br>
	 * 
	 * @throws Exception
	 */
	public synchronized void load() throws Exception {
		if (loaded)
			return;
		Stopwatch.start(getClass().getName());
		getLogger().debug("Loading...");
		// Read all the document names & hashes appropriately
		surface_forms.clear();
		int lineCounter = 0;
		final List<int[]> hashList = Lists.newArrayList();
		try (BufferedReader bwIn = new BufferedReader(new FileReader(new File(outHashes)))) {
			String line = null;
			while ((line = bwIn.readLine()) != null) {
				String[] tokens = line.split(docArraySplitStr);
				// aka. surface form
				final String docName = tokens[0];
				surface_forms.add(docName);
				final String[] arrayTokens = tokens[1].replace("[", "").replace("]", "").split(",");

				int[] array = new int[arrayTokens.length];
				hashList.add(array);
				int arrCounter = 0;
				for (String arrayToken : arrayTokens) {
					array[arrCounter++] = Integer.valueOf(arrayToken.trim());
				}
				lineCounter++;
			}
		}
		hashes = hashList.toArray(new int[lineCounter][]);

		// Based on the documents, fill up the bag of words
		ngrams.clear();
		final TreeSet<String> sortedNGrams = new TreeSet<>();
		for (String s : surface_forms) {
			for (String ngram : FuzzyUtils.generateNgrams(s, n_gram_length)) {
				sortedNGrams.add(ngram);
			}
		}
		int nGramPosCounter = 0;
		for (String ngram : sortedNGrams) {
			ngrams.put(ngram, nGramPosCounter++);
		}
		updateBuckets();
		// Recreate the sparse vectors by loading from specified file
		try (BufferedReader bwIn = new BufferedReader(new FileReader(new File(outDocVectorsEntries)))) {
			String line = null;
			final List<LSHSparseVector<Boolean>> doc_vectors_list = Lists.newArrayList();
			while ((line = bwIn.readLine()) != null) {
				final String entries = line;
				final String[] entryTokens = entries.replace("[", "").replace("]", "").split(",");
				LSHSparseVector<Boolean> sparse_vec = LSHSparseVector.create(ngrams.size());
				for (String index : entryTokens) {
					sparse_vec.set(Integer.valueOf(index.trim()));
				}
				doc_vectors_list.add(sparse_vec);
			}
			this.document_vectors_sparse = doc_vectors_list.toArray(new LSHSparseVector[surface_forms.size()]);
		}

		// Set LSHMinHash
		this.lsh = new LSHMinHash(bands, buckets, this.ngrams.size(), this.seed);
		getLogger().info("Loading completed in " + Stopwatch.endDiff(getClass().getName()) + " ms.");
		loaded = true;
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
			getCollisionCounter().set(0);
			// this.hashes = setup();
			// backup all data
			// backup();
			load();
			final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(Numbers.MENTION_DETECTION_THREAD_AMT.val.intValue());
			AtomicInteger doneCounter = new AtomicInteger(0);
			final List<Mention> mentions = inputProcessor.createMentions(input, source, detectionType);
			for (Mention mention : mentions) {
				execFind(executor, mention, doneCounter, threshold);
			}

			// No more tasks will be added
			executor.shutdown();
			do {
				// No need for await termination as this is pretty much it already...
				Thread.sleep(50);
				// getLogger().debug("Finished executing: " + doneCounter.get() + "
				// processes.");
			} while (!executor.isTerminated());
			getLogger().info("Collisions: " + collisionCounter.get());
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
			return mentions;
//			return mentions.stream()
//					.filter(mention -> mention.getMention() != null && mention.getMention().length() > 0)
//					.collect(Collectors.toList());
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
	 * @param executor    multithreaded executor
	 * @param mention     mention to see if there are candidates for it
	 * @param doneCounter how many are done
	 * @param threshold   min. similarity threshold for matching
	 */
	private void execFind(final ThreadPoolExecutor executor, final Mention mention, final AtomicInteger doneCounter,
			final double threshold) {
		executor.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				final MinHashObject minhashResult = find(mention.getOriginalWithoutStopwords(), threshold);
				if (minhashResult != null) {
					mention.setMention(minhashResult.word);
					mention.setDetectionConfidence(minhashResult.confidence);
				}
				return doneCounter.incrementAndGet();
			}
		});
	}

	/**
	 * Finds a mention for a given input token
	 * 
	 * @param input     token or word
	 * @param source    what entity this word is linked to
	 * @param threshold minimum similarity threshold for it to be accepted
	 * @param offset    offset at which this string starts in the original text
	 * @return mention with the closest possible mate
	 * 
	 */
	public MinHashObject find(final String input, final double threshold) {
		if (input == null || input.length() == 0) {
			return null;
		}
		final MinHashObject resMinHash = minhash(input, threshold);
		if (resMinHash == null) {
			return null;
		}
		final String word = resMinHash.word;
		final double findConfidence = resMinHash.confidence;
		if (word == null) {
			// No mention found within our knowledge base... which would match threshold
			// criterion, at least
			return null;
		}
		return resMinHash;
	}

	/**
	 * Setup() done for pre-computing the LSH signatures etc.
	 * 
	 * @return
	 * @throws Exception
	 */
	public void setup(final Map<String, Collection<String>> map) throws Exception {
		if (setup)
			return;// this.hashes;
		ngrams.clear();
		this.surface_forms.clear();
		final Set<String> keys = map.keySet();
		this.surface_forms.addAll(keys);
		final TreeSet<String> allNGrams = new TreeSet<>();
		for (String word : keys) {
			// This is a word we want to ngram and add
			for (String ngram : FuzzyUtils.generateNgrams(word, n_gram_length)) {
				allNGrams.add(ngram);
			}
		}
		int ngramPosCounter = 0;
		// Populate ngrams
		for (String ngram : allNGrams) {
			ngrams.put(ngram, ngramPosCounter++);
		}
		updateBuckets();
		// Precompute the vectors + hashes

		// Required as this.ngrams is not final
		final SortedMap<String, Integer> dictionary = this.ngrams;
		System.out.println("Dict size: " + dictionary.size());
		final TreeSet<String> documents = this.surface_forms;
		// Number of sets ('documents', I assume?)
		int documentSize = documents.size();
		// Size of dictionary
		this.document_vectors_sparse = new LSHSparseVector[documentSize];
		this.lsh = new LSHMinHash(bands, buckets, dictionary.size(), this.seed);

		// Create the vectors to execute hashes on afterwards
		final Iterator<String> documentsIterator = documents.iterator();
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
		AtomicInteger doneCounter = new AtomicInteger(0);
		int[][] hashes = new int[documentSize][];
		for (int i = 0; i < documentSize; ++i) {
			final String document = documentsIterator.next();
			final int vectorIndex = i;
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					document_vectors_sparse[vectorIndex] = LSHSparseVector.create(dictionary.size());
					final LSHSparseVector<Boolean> vector = document_vectors_sparse[vectorIndex];
					// Then fill out the right values for this particular document
					final List<String> doc_ngrams = FuzzyUtils.generateNgrams(document, n_gram_length);
					for (final String ngram : doc_ngrams) {
						int index = dictionary.get(ngram);// getIndex(dictionary, ngram);
						vector.set(index);
					}
					// Now we can proceed to LSH binning
					// Compute the LSH hash of each vector
					hashes[vectorIndex] = lsh.hash(vector.toBooleanArray());
					// Returning the ID helps us potentially identify which ones have not completed
					// yet, if wanted in the future
					return doneCounter.incrementAndGet();
				}
			});
		}
		// No more tasks will be added
		executor.shutdown();
		do {
			// No need to await termination as this is pretty much it already...
			Thread.sleep(5_000);
			getLogger()
					.debug("Vector setup - In progress [" + doneCounter.get() + " / " + documentSize + "] documents.");
		} while (!executor.isTerminated());
		// Shouldn't wait at all generally, but in order to avoid unexpected behaviour -
		// especially relating to logic changes on the above busy-waiting loop
		executor.awaitTermination(10L, TimeUnit.MINUTES);
		//
		getLogger()
				.info("Finished computing signatures for " + doneCounter.get() + " / " + documentSize + " documents!");

		this.hashes = hashes;
		setup = true;
	}

	private void updateBuckets() {
		// Update buckets based on dataset size
		// n = bands * rows per band
		// this.buckets = (int) (Math.ceil(Math.sqrt(((double) this.ngrams.size()))));
		// this.buckets = 200;
		if (buckets == bucketsDefaultValue) {
			this.buckets = Numbers.LSH_BUCKETS.val.intValue();
		}
		if (bands == bandsDefaultValue) {
			this.bands = Numbers.LSH_BANDS.val.intValue();
		}
		System.out.println("Bands: " + this.bands + "; Buckets: " + buckets);
	}

	/**
	 * Returns at which position within a given sorted set the passed 'word' is
	 * located, returns -1 if none found
	 * 
	 * @param bagOfWords
	 * @param word
	 * @return
	 */
	private int getIndex(SortedSet<String> bagOfWords, String word) {
		return bagOfWords.contains(word) ? bagOfWords.headSet(word).size() : -1;
	}

	/**
	 * Generates signature for input query and retrieves with given minimum
	 * threshold the best word for it!
	 * 
	 * @param query     input query which we want to fuzzily match with
	 * @param threshold minimum threshold for Jaccard similarity to accept a word
	 * @return most similar word to passed one or null if threshold is not met
	 */
	private MinHashObject minhash(final String query, final double threshold) {
		try {
			final TreeMap<String, Integer> dictionary = this.ngrams;
			// Size of dictionary
			int n = dictionary.size();

			final LSHSparseVector<Boolean> query_vector_sparse = LSHSparseVector.create(n);

			final List<String> query_ngrams = FuzzyUtils.generateNgrams(query, n_gram_length);
			for (String ngram : query_ngrams) {
				int index = dictionary.getOrDefault(ngram, -1);// getIndex(dictionary, ngram);
				// index == -1 means that we are introducing a new ngram which didn't exist
				// before with our query aka. -> no hit possible with it, so we ignore it
				if (index != -1) {
					query_vector_sparse.set(index);
				}
			}
			final boolean[] query_vector_dense = query_vector_sparse.toBooleanArray();
			final int[] query_hash = lsh.hash(query_vector_dense);
			final Map<Integer, Double> similarDocs = findSimilarEntries(query_vector_sparse, query_hash, threshold,
					document_vectors_sparse, hashes);
			if (similarDocs == null || similarDocs.size() == 0) {
				return null;
			}
			final List<String> retrievalList = Lists.newArrayList(surface_forms);
			double maxVal = 0d;
			int maxValIndex = -1;
			for (Map.Entry<Integer, Double> e : similarDocs.entrySet()) {
				if (e.getValue() > maxVal) {
					maxValIndex = e.getKey();
					maxVal = e.getValue();
				}
			}
			final String retrievedWord = retrievalList.get(maxValIndex);
			return new MinHashObject(retrievedWord, maxVal);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Map<Integer, Double> findSimilarEntries(LSHSparseVector<Boolean> queryDocument, int[] query_hash,
			double threshold, LSHSparseVector<Boolean>[] document_vectors_sparse, int[][] hashes) {
		final Map<Integer, Double> ret = new TreeMap<Integer, Double>();
		// getLogger().debug("Hash - Query: " + Arrays.toString(query_hash));
		for (int i = 0; i < document_vectors_sparse.length; i++) {
			LSHSparseVector<Boolean> doc2 = document_vectors_sparse[i];
			// We compute the similarity between each pair of sets
			double similarity = 0d;
			final int[] hash2 = hashes[i];
			final boolean oneOrMoreSimilar = FuzzyUtils.sameHashOnSameIndex(query_hash, hash2);
			// Just for understanding purposes
			final boolean possiblyFits = oneOrMoreSimilar;
			if (possiblyFits) {
				try {
					similarity = FuzzyUtils.jaccardSimilarity(queryDocument, doc2);
					// MinHash.jaccardIndex(doc1, doc2);
					if (similarity >= threshold) {
						// if (FuzzyUtils.reachesThreshold(queryDocument.getEntries(),
						// doc2.getEntries(), threshold)) {
						// Add index of the document to ret list
						ret.put(i, similarity);
						// ret.put(i, threshold);
					} else {
						collisionCounter.incrementAndGet();
					}
				} catch (NullPointerException npe) {
					getLogger().error("NPE during JS computation for [" + queryDocument + "], doc2[" + doc2 + "]");
					throw npe;
					// continue;
				}
			} else {
			}
		}
		return ret;
	}

	public AtomicInteger getCollisionCounter() {
		return this.collisionCounter;
	}

	@Override
	public boolean init() {
		try {
			load();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
