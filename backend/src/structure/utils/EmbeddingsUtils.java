package structure.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import structure.config.constants.Strings;

public class EmbeddingsUtils {
	private static Logger logger = Logger.getLogger(EmbeddingsUtils.class);

	/**
	 * See {@link #humanload(String, String, Set)}
	 */
	public static Map<String, List<Number>> humanload(final String mappingInPath, final String embeddingInPath)
			throws IOException {
		return humanload(mappingInPath, embeddingInPath, null);
	}

	/**
	 * Load entity embeddings from a human readable file and translate the entity
	 * mappings directly to the fully-qualified IRIs
	 * 
	 * @return the fully-qualified entity embeddings
	 * @throws IOException
	 */
	public static Map<String, List<Number>> humanload(final String mappingInPath, final String embeddingInPath,
			final Set<String> wantedEntities) throws IOException {
		IDMappingLoader<String> entityMapping = new IDMappingLoader<String>().loadHumanFile(new File(mappingInPath));
		final File embedFile = new File(embeddingInPath);
		logger.info("Loading embeddings from: " + embedFile.getAbsolutePath());
		Stopwatch.start(EmbeddingsUtils.class.getName() + "humanload");
		final Map<String, List<Number>> entityEmbeddingsMap = EmbeddingsUtils.readEmbeddings(embedFile, entityMapping,
				true, wantedEntities);
		logger.info("Finished(" + Stopwatch.endOutput(EmbeddingsUtils.class.getName() + "humanload")
				+ " ms.) loading embeddings from: " + embedFile.getAbsolutePath());
		entityMapping = null;
		return entityEmbeddingsMap;
	}

	/**
	 * See {@link #readEmbeddings(File, IDMappingLoader, boolean, Set)}
	 * 
	 * @param intputFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Map<String, List<Number>> readEmbeddings(final File intputFile)
			throws FileNotFoundException, IOException {
		return readEmbeddings(intputFile, null, true, null);
	}

	/**
	 * See {@link #readEmbeddings(File, IDMappingLoader, boolean, String, boolean, Set)}
	 * 
	 */
	public static Map<String, List<Number>> readEmbeddings(final File intputFile,
			final IDMappingLoader<String> mappingLoader, final boolean normalize, final Set<String> wantedEntities)
			throws FileNotFoundException, IOException {
		final String delim = Strings.EMBEDDINGS_TRAINED_DELIM.val;
//		final boolean stripArrows = false;
		// final String delim = " ";
		final boolean stripArrows = true;
		return readEmbeddings(intputFile, mappingLoader, normalize, delim, stripArrows, wantedEntities);
	}

	/**
	 * Reads a python embeddings file and loads it into a Map&lt;String,
	 * List&lt;Number&gt;&gt; structure<br>
	 * <b>Note</b>: If a vocabulary word appears multiple times, the latter will
	 * replace the existing one
	 * 
	 * @param intputFile     input file containing embeddings
	 * @param mappingLoader  NULLABLE; used IDMappingLoader (if applicable)
	 * @param normalize      whether to normalize the embeddings vectors
	 * @param delim          what delimiter was used to output the embeddings
	 * @param stripArrows    whether to strip arrows from the entity
	 * @param wantedEntities NULLABLE; allows for lazy-loading of embeddings (useful
	 *                       for particularly large embeddings files and when
	 *                       changes to MD are being done)
	 * @return populated embeddings map
	 * @throws FileNotFoundException if file was not found
	 * @throws IOException           if any IO exception happens, most likely due to
	 *                               file reading
	 */
	public static Map<String, List<Number>> readEmbeddings(final File intputFile,
			final IDMappingLoader<String> mappingLoader, final boolean normalize, final String delim,
			final boolean stripArrows, final Set<String> wantedEntities) throws FileNotFoundException, IOException {
		System.out.println("Wanted Entities: " + (wantedEntities == null ? "null" : wantedEntities.size()));
		// Embeddings format: vocabularyWord <delim> List<Double>
		final Map<String, List<Number>> embeddings = new HashMap<>();
		int lineCounter = 0, loadedCounter = 0;
		String line = null;
		try (final BufferedReader brIn = new BufferedReader(new FileReader(intputFile))) {
			while ((line = brIn.readLine()) != null) {
				if (lineCounter % 100_000 == 0) {
					System.out
							.println("# of embeddings: Loaded[" + loadedCounter + "] / Traversed[" + lineCounter + "]");
					System.out.println("Current: " + line.substring(0, 100));
				}
				lineCounter++;
				double sum = 0d;
				// Word \t 1.23123 \t 2.1421421 ...
				final String[] tokens = line.split(delim);
				String vocab = tokens[0];
				// If it's an ID and needs to translate to a resource
				if (mappingLoader != null && !mappingLoader.isEmpty()) {
					final String associatedWord = mappingLoader.getMapping(vocab);
					if (associatedWord != null) {
						vocab = associatedWord;
					}
				}

				// Lazy loading component - only load it if it can be detected through mentions
				if (wantedEntities != null && wantedEntities.size() > 0) {
					if (!wantedEntities.contains(vocab)) {
						// Not within our set, so skip it
						continue;
					}
				}

				// Strips < and > from token
				if (stripArrows) {
					final int endOffset = vocab.length() - 1;
					if ((vocab.charAt(0) == '<') && (vocab.charAt(endOffset) == '>') && endOffset > 1) {
						vocab = vocab.substring(1, endOffset);
					}
				}

				List<Number> embedding = Lists.newArrayList();
				for (int i = 1; i < tokens.length; ++i) {
					final float embedVal = // Double
							Float//
									.valueOf(tokens[i]);
					tokens[i] = null;
					embedding.add(embedVal);
					// Sum it up here for normalization
					// sum += embedVal;
				}
				// Normalize the list of values by dividing by its sum (aka. sum becomes 1.0)
				if (normalize) {
					embedding = normalize(embedding);
				}
				embeddings.put(vocab, embedding);
				loadedCounter++;
				embedding = null;
			}
		}

		return embeddings;
	}

	/**
	 * Builds the sentence embeddings through addition of each word embedding
	 * 
	 * @param embeddings computed embeddings to get required embeddings from
	 * @param sentence   tokenized sentence (should be tokenized the same way as the
	 *                   sentences are in the Python training script)
	 * @return embedding for this sentence
	 */
	public static List<Number> rebuildSentenceEmbeddingSum(Map<String, List<Number>> embeddings,
			List<String> sentence) {
		return rebuildSentenceEmbedding(embeddings, sentence, EmbeddingsUtils::add);
	}

	/**
	 * Builds the sentence embeddings through addition of each word embedding
	 * 
	 * @param embeddings computed embeddings to get required embeddings from
	 * @param sentence   tokenized sentence (should be tokenized the same way as the
	 *                   sentences are in the Python training script)
	 * @param method     which function to apply to combine the sentence's
	 *                   embeddings
	 * @return combined embedding
	 */
	public static List<Number> rebuildSentenceEmbedding(Map<String, List<Number>> embeddings, List<String> sentence,
			BiFunction<List<Number>, List<Number>, List<Number>> method) {
		List<Number> embedding = null;
		for (String s : sentence) {
			embedding = method.apply(embedding, embeddings.get(s));
		}
		return embedding;
	}

	/**
	 * Adds two list-vectors together and outputs the resulting vector
	 * 
	 * @param l1 first vector
	 * @param l2 second vector
	 * @return resulting vector
	 */
	public static List<Number> add(List<Number> l1, List<Number> l2) {
		if (l1 == null)
			return l2;
		if (l2 == null)
			return l1;
		if (l1.size() != l2.size()) {
			throw new RuntimeException("Wrong dimensions! " + l1.size() + " vs. " + l2.size());
		}
		final List<Number> result = Lists.newArrayList();
		for (int i = 0; i < l1.size(); ++i) {
			result.add(l1.get(i).doubleValue() + l2.get(i).doubleValue());
		}
		return result;
	}

	/**
	 * See {@link #outputEntityEmbeddings(File, Map, BiFunction)}
	 * 
	 * @param outFile            output file
	 * @param combinedEmbeddings combined embeddings
	 * @throws IOException
	 */
	public static void outputEntityEmbeddings(final File outFile, final Map<String, List<Number>> combinedEmbeddings)
			throws IOException {
		outputEntityEmbeddings(outFile, combinedEmbeddings, EmbeddingsUtils::formatEntityOutput);
	}

	/**
	 * Outputs embeddings according to func appropriately to the specified output
	 * file (line by line)
	 * 
	 * @param outFile            output file
	 * @param combinedEmbeddings structure containing the previously combined
	 *                           sentence embeddings (see
	 *                           {@link #rebuildSentenceEmbedding(Map, List, BiFunction)}
	 *                           )
	 * @param func               function to format the entity and its associated
	 *                           embedding
	 * @throws IOException
	 */
	public static void outputEntityEmbeddings(final File outFile, final Map<String, List<Number>> combinedEmbeddings,
			BiFunction<String, List<Number>, String> func) throws IOException {
		try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(outFile))) {
			for (Map.Entry<String, List<Number>> e : combinedEmbeddings.entrySet()) {
				bwOut.write(func.apply(e.getKey(), e.getValue()));
				bwOut.newLine();
			}
		}
	}

	/**
	 * Formats entity and embedding as: entity \<delim\> embedding1 \<delim\>
	 * embedding2 ... etc
	 * 
	 * @param entity    entity which the passed embedding belongs to
	 * @param embedding embedding to be formatted for output
	 * @return formatted output that can be output to the filesystem
	 */
	public static String formatEntityOutput(final String entity, List<Number> embedding) {
		final String delim = Strings.EMBEDDINGS_TRAINED_SENTENCES_DELIM.val;
		final StringBuilder retSB = new StringBuilder();
		retSB.append(entity);
		for (Number n : embedding) {
			retSB.append(delim);
			retSB.append(n);
		}
		return retSB.toString();
	}

//	public static void findPermutations(final int startIndex, final int entityIndex,
//			List<String> chosenEntities, final List<List<String>> result, List<String>... clusters) {
//		final List<List<String>> ret = Lists.newArrayList();
//		for (int i = startIndex; i < clusters.length; ++i) {
//			final String currEntity = clusters[i].get(entityIndex);
//			// List<String> cluster : clusters) {
//			iterateOverCluster(i, 0, clusters[i], result, chosenEntities, clusters);
//			return nextChosenEntities;
//		}
//		return null;
//	}
//
//	private static void iterateOverCluster(int clusterIndex, int from, List<String> cluster, List<List<String>> result,
//			List<String> chosenEntities, List<String>[] clusters) {
//		for (int j = from; j < cluster.size(); ++j) {
//			System.out.println("i=" + clusterIndex + ", j=" + j);
//			List<String> nextChosenEntities = Lists.newArrayList(chosenEntities);
//			nextChosenEntities.add(cluster.get(j));
//			// ret.add(findPermutations(i, j, nextChosenEntities, result, clusters));
//			// return findPermutations(i, j, nextChosenEntities, result, clusters);
//			System.out.println(nextChosenEntities);
//			result.add(nextChosenEntities);
//			final List<String> entities = findPermutations(clusterIndex + 1, j + 1, nextChosenEntities, result, clusters);
//		}
//	}

	/**
	 * Computes all wanted permutations for entities within clusters, they are
	 * generally clustered by surface form and the entities are the candidates
	 * entities for them. This way, we can then compute the global optimum
	 * (including all entities) for defined similarity/distance metrics.
	 * 
	 * @param clusters all input clusters of entities
	 * @return all possibilities
	 */
	public static List<List<String>> findPermutations(List<List<String>> clusters) {
		// Go through clusters
		List<List<String>> results = Lists.newArrayList();
		iterateClusters(0, null, results, clusters);
		return results;
	}

	/**
	 * Iterates recursively through clusters, picking one item per cluster
	 * 
	 * @param currClusterIndex index of cluster we are at
	 * @param currChoices      currently computed choice
	 * @param results          output for final computed choices (populated at the
	 *                         end when all clusters are chosen bc we want to have
	 *                         all choices)
	 * @param clusters         all the clusters from which to pick
	 * @return
	 */
	private static List<String> iterateClusters(final int currClusterIndex, List<String> currChoices,
			List<List<String>> results, List<List<String>> clusters) {
		if (currClusterIndex > clusters.size() - 1) {
			return currChoices;
		}
		if (currChoices == null) {
			currChoices = Lists.newArrayList();
		}
		final List<String> cluster = clusters.get(currClusterIndex);
		for (int j = 0; j < cluster.size(); ++j) {
			final List<String> combinedChoices = Lists.newArrayList(currChoices);
			combinedChoices.add(cluster.get(j));
			iterateClusters(currClusterIndex + 1, combinedChoices, results, clusters);
			if (currClusterIndex >= clusters.size() - 1) {
				results.add(combinedChoices);
			}
		}
		return currChoices;
	}

	public static Number cosineSimilarity(final List<Number> left, final List<Number> right) throws RuntimeException {
		return cosineSimilarity(left, right, true);
	}

	/**
	 * Computes cosine similarity of two vectors in terms of double values
	 * 
	 * @param l1 first vector
	 * @param l2 second vector
	 * @return cosine similarity of the two vectors
	 */
	public static Number cosineSimilarity(final List<Number> left, final List<Number> right, final boolean normalize)
			throws RuntimeException {
		if (left.size() != right.size()) {
			throw new RuntimeException(
					"Incompatible dimensions: Left(" + left.size() + ") vs. Right(" + right.size() + ")");
		}

		// Normalize input vectors
		final List<Number> l1;
		final List<Number> l2;
		if (normalize) {
			l1 = normalize(left);
			l2 = normalize(right);
		} else {
			l1 = left;
			l2 = right;
		}

		Number num = 0d;
		Number denLeft = 0d;
		Number denRight = 0d;
		for (int i = 0; i < l1.size(); ++i) {
			num = num.doubleValue() + (l1.get(i).doubleValue() * l2.get(i).doubleValue());
			denLeft = denLeft.doubleValue() + Math.pow(l1.get(i).doubleValue(), 2);
			denRight = denRight.doubleValue() + Math.pow(l2.get(i).doubleValue(), 2);
		}
		denLeft = Math.sqrt(denLeft.doubleValue());
		denRight = Math.sqrt(denRight.doubleValue());

		final Number den = (denLeft.doubleValue() * denRight.doubleValue());

		final Number ret;
		// return ((num.doubleValue() / den.doubleValue()) + 1d) / 2d;
		ret = Math.abs((num.doubleValue() / den.doubleValue()));
		// return (num.doubleValue() / den.doubleValue());
		return ret;
	}

	public static double dotProduct(List<Number> left, List<Number> right) {
		double sum = 0;
		for (int i = 0; i < left.size(); i++) {
			sum += // Math.abs(
					left.get(i).doubleValue() * right.get(i).doubleValue()
			// )
			;
		}
		return sum;
	}

	/**
	 * 
	 * <b>Note</b>: Does NOT change original list
	 * 
	 * @param inputList to normalize
	 * @return normalized list
	 */
	public static List<Number> normalize(final List<Number> inputList) {
		// Compute sum so we know what to divide each element by
		final List<Number> list = Lists.newArrayList();
		Number sum = 0d;
		for (Number n : inputList) {
			sum = sum.doubleValue() + //
//					Math.abs(//
					n.doubleValue()//
//			)//
			;//
		}

		// Add prior values divided by their sum to the returned list
		for (int i = 0; i < inputList.size(); ++i) {
			final Number normalizedVal = inputList.get(i).doubleValue() / sum.doubleValue();
			list.add(normalizedVal);
		}
		return list;
	}
}
