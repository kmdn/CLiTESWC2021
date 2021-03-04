package linking.disambiguation.scorers.embedhelp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import structure.config.constants.Comparators;
import structure.config.constants.EnumEmbeddingMode;
import structure.config.constants.Numbers;
import structure.utils.DecoderUtils;
import structure.utils.EmbeddingsUtils;
import structure.utils.TextUtils;

/**
 * An LRU-based caching service for entity similarity with a size of
 * {@link Numbers}.SIMILARITY_CACHE_SIZE.<br>
 * Pretty much just a convenience class for speed-up.
 * 
 * @author Kristian Noullet
 *
 */
public class EntitySimilarityService {
	private final Map<String, List<Number>> embeddings;
	private final LRUMap<String, Number> distCache = new LRUMap<>(Numbers.SIMILARITY_CACHE_SIZE.val.intValue());
	public final Set<String> notFoundIRIs = new HashSet<>();
	public final AtomicInteger recovered = new AtomicInteger();
	public final boolean DEFAULT_LOCAL_OR_API = false;
	private final ProcessBuilder processBuilder;
	private final String baseURL = "http://localhost:3030/model?";
	private final String arg1 = "url1=";
	private final String arg2 = "url2=";
	private final CloseableHttpClient client = HttpClients.createDefault();
	private final boolean APACHE_VS_URL = false;

	// private static int counter = 0;

	public EntitySimilarityService(final Map<String, List<Number>> embeddings) {
		this.embeddings = embeddings;
		this.processBuilder = null;
	}

	public EntitySimilarityService() {
		this.processBuilder = new ProcessBuilder();
		this.embeddings = null;
	}

	/**
	 * Computes similarity between two passed entities - repeated calls to the same
	 * (or switched around) pair will grab the value directly from an in-memory
	 * lookup table <br>
	 * <b>Note</b>: Thread-safety ensured
	 * 
	 * @param entity1 first entity
	 * @param entity2 second entity
	 * @return similarity between the two
	 */
	public Number similarity(final String entity1, final String entity2) {
		return similarity(entity1, entity2, EnumEmbeddingMode.DEFAULT.val == EnumEmbeddingMode.LOCAL);
	}

	public Number similarity(final String entity1_raw, final String entity2_raw, final boolean LOCAL_OR_API) {
		final String entity1 = TextUtils.stripArrowSigns(entity1_raw.trim());
		final String entity2 = TextUtils.stripArrowSigns(entity2_raw.trim());
		final String keyStr = key(entity1, entity2);
		Number retVal;
		synchronized (this.distCache) {
			retVal = this.distCache.get(keyStr);
		}

		if (retVal == null) {
			if (LOCAL_OR_API) {
				List<Number> left = this.embeddings.get(entity1);
				List<Number> right = this.embeddings.get(entity2);
				if (left == null || right == null) {
					// Try with percentage decoding!
					// Attempt left recovery - if required
					if (left == null) {
						final String decodedEntity1 = DecoderUtils.escapePercentage(entity1);
						if (decodedEntity1 != null && decodedEntity1.length() > 0) {
							left = this.embeddings.get(decodedEntity1);
						}
					}

					// Attempt right recovery - if required
					if (right == null) {
						final String decodedEntity2 = DecoderUtils.escapePercentage(entity2);
						if (decodedEntity2 != null && decodedEntity2.length() > 0) {
							right = this.embeddings.get(decodedEntity2);
						}
					}

					if (left == null || right == null) {
						// -> couldn't be (completely) recovered
						if (left == null) {
							notFoundIRIs.add(entity1);
						}
						if (right == null) {
							notFoundIRIs.add(entity2);
						}
						return 0F;
					} else {
						recovered.incrementAndGet();
					}
				}
				retVal = EmbeddingsUtils.cosineSimilarity(left, right, true);
			} else {
				// API Calls!
				final StringBuilder sbURL = new StringBuilder(baseURL);
				sbURL.append(arg1);
				sbURL.append(entity1.replace("#", "%23"));
				sbURL.append("&");
				sbURL.append(arg2);
				sbURL.append(entity2.replace("#", "%23"));
				try {
					// if (counter > 5) {
					// System.out.println(sbURL.toString());
					// throw new RuntimeException("forced death");
					// }
					retVal = curlHTTP(sbURL);
					// counter++;
				} catch (IOException e1) {
					System.err.println("Attempting to recover by cmd line (curl): " + e1.getMessage());
					e1.printStackTrace();
					try {
						retVal = curl(sbURL);
					} catch (IOException e) {
						System.err.println("Not recoverable... sim(" + entity1 + "/" + entity2 + ")");
					}
				}
			}
		}

		synchronized (this.distCache) {
			if (this.distCache.get(keyStr) != null) {
				this.distCache.put(keyStr, retVal);
			}
		}
		return retVal;

	}

	/**
	 * Makes use of URL and URL.openstream to get the response
	 * 
	 * @param sbURL
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private Number curlHTTP(final StringBuilder sbURL) throws UnsupportedEncodingException, IOException {
		final StringBuilder sbRet = new StringBuilder();
		Number retVal = 0d;
		if (APACHE_VS_URL) {
			// Has issues with special characters
			final HttpGet httpGet = new HttpGet(sbURL.toString());
			try (final CloseableHttpResponse response = client.execute(httpGet)) {
				final HttpEntity httpEntity = response.getEntity();
				try (final InputStream is = httpEntity.getContent();
						final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
					for (String line; (line = reader.readLine()) != null;) {
						sbRet.append(line);
						// System.out.println(line);
					}
				}
			}
		} else {
			final URL url = new URL(sbURL.toString());
			try (final InputStream is = url.openStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
				for (String line; (line = reader.readLine()) != null;) {
					sbRet.append(line);
					// System.out.println(line);
				}
			}
		}
		try {
			retVal = Double.valueOf(sbRet.toString().trim());
		} catch (NumberFormatException nfe) {
			retVal = 0d;
		}
		return retVal;
	}

	/**
	 * Works with processbuilder to send commands over the command line...
	 * 
	 * @param sbURL
	 * @return
	 * @throws IOException
	 */
	private Number curl(final StringBuilder sbURL) throws IOException {
		final String command = "curl -X GET " + sbURL.toString();
		Number retVal = 0d;
		processBuilder.command(command.split(" "));
		processBuilder.directory(new File("./"));
		Process process = null;
		try {
			process = processBuilder.start();
			try {
				final boolean finished = process.waitFor(5, TimeUnit.MINUTES);
				if (!finished) {
					// If it doesn't manage to finish, return 0
					return 0d;
				}
				Thread.sleep(10l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (process.exitValue() != 0) {
				throw new IOException("Process Error code: " + process.exitValue());
			}
			try (final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line = null;
				final StringBuilder content = new StringBuilder();
				while ((line = br.readLine()) != null) {
					content.append(line);
					content.append(" ");
				}
				System.out.println("Received content:" + content.toString());
				final String contentString = content.toString();
				retVal = Double.valueOf(contentString.trim());
			}
		} finally {
			process.destroy();
		}
		return retVal;
	}

	/**
	 * Create an internal key for storing/retrieving similarity values from a map
	 * 
	 * @param entity1 first entity
	 * @param entity2 second entity
	 * @return
	 */
	private String key(String entity1, String entity2) {
		final StringBuilder sbDistKey;
		final int compareRes = entity1.compareTo(entity2);
		if (compareRes > 0) {
			sbDistKey = new StringBuilder(entity1);
			sbDistKey.append(entity2);
		} else {
			// If they're both equal... it means it's the same entity, so doesn't matter
			// which one is first and which second
			sbDistKey = new StringBuilder(entity2);
			sbDistKey.append(entity1);
		}
		return sbDistKey.toString();
	}

	/**
	 * Returns the highest-rated pair (entity URL, score) based on cosine similarity
	 * rating from a specified entity to wanted targets
	 * 
	 * @param source
	 * @param targets
	 * @return
	 */
	public Pair<String, Double> topSimilarity(final String source, Collection<String> targets,
			final boolean allowSelfConnection) {
		if (!allowSelfConnection && targets.contains(source)) {
			// Copies to a new list and removes the source
			final Set<String> copyTargets = new HashSet<String>(targets);
			copyTargets.remove(source);
			List<Pair<String, Double>> pairs = computeSortedSimilarities(source, copyTargets,
					Comparators.pairRightComparator.reversed());
			if (pairs != null && pairs.size() > 0) {
				return pairs.get(0);
			} else {
				return null;
			}
		} else {
			List<Pair<String, Double>> pairs = computeSortedSimilarities(source, targets,
					Comparators.pairRightComparator.reversed());
			if (pairs != null && pairs.size() > 0) {
				return pairs.get(0);
			} else {
				return null;
			}
		}
	}

	/**
	 * Computes similarities between a given source and targets, sorting it
	 * afterwards with the passed comparator
	 * 
	 * @param source     source entity
	 * @param targets    target entities
	 * @param comparator to sort results
	 * @return sorted list of pairs
	 */
	public List<Pair<String, Double>> computeSortedSimilarities(final String source, final Collection<String> targets,
			final Comparator<Pair<? extends Comparable, ? extends Comparable>> comparator) {
		final List<Pair<String, Double>> retList = computeSimilarities(source, targets);
		Collections.sort(retList, comparator);
		return retList;
	}

	/**
	 * Computes similarities from a source to the given targets and returns a list
	 * with all of them
	 * 
	 * @param source  from where
	 * @param targets to where
	 * @return
	 */
	public List<Pair<String, Double>> computeSimilarities(final String source, final Collection<String> targets) {
		List<Pair<String, Double>> retList = Lists.newArrayList();
		for (String target : targets) {
			retList.add(new ImmutablePair<String, Double>(target, similarity(source, target).doubleValue()));
		}
		return retList;
	}

	/**
	 * Check that the passed iterable's items really have associated embeddings.
	 * Removes item from collection otherwise.
	 * 
	 * @param collection
	 */
	public void ascertainSimilarityExistence(final Iterable<String> collection) {
		if ((this.embeddings == null && this.processBuilder != null) || this.embeddings.size() < 1) {
			return;
		}

		final Iterator<String> iter = collection.iterator();
		while (iter.hasNext()) {
			final String key = iter.next();
			if (!this.embeddings.containsKey(key)) {
				iter.remove();
			}
		}
	}

}
