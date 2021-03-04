package structure.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Lists;

import structure.datatypes.lsh.LSHSparseVector;

public class FuzzyUtils {
	private static final char padding = '$';

	/**
	 * Creates ngrams for given input and passed 'n' (aka. windowSize)
	 * 
	 * @param input      text to be turned into ngrams/shingles
	 * @param windowSize n parameter - size of resulting shingles
	 * @return
	 */
	public static List<String> generateNgrams(final String input, final int windowSize) {
		if (windowSize < 1)
			return null;
		List<String> ret = Lists.newArrayList();
		final String paddedInput = generatePadding(windowSize - 1) + input.toLowerCase()
				+ generatePadding(windowSize - 1);
		for (int i = 0; i < paddedInput.length() - windowSize + 1; ++i) {
			final StringBuilder shingle = new StringBuilder();
			for (int j = i; j < i + windowSize; ++j) {
				shingle.append(paddedInput.charAt(j));
			}
			ret.add(shingle.toString());
			shingle.setLength(0);
		}
		return ret;
	}

	/**
	 * Generates padding for n-grams
	 * 
	 * @param windowSize
	 * @return
	 */
	private static String generatePadding(int windowSize) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < windowSize; ++i) {
			sb.append(padding);
		}
		return sb.toString();
	}

	public static Double jaccardSimilarity(LSHSparseVector<Boolean> vec1, LSHSparseVector<Boolean> vec2) {
		double sim = 0d;
		Set<Integer> vec1_entries = vec1.getEntries();// 0, 1, 2, 5, 7
		Set<Integer> vec2_entries = vec2.getEntries();// 0, 1, 2, 3, 4, 5, 6, 8, 9
		// Overlap: |{0, 1, 2, 5}| = 4
		// Denominator: |{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}| = 10
		for (Integer i : vec1_entries) {
			if (!vec2_entries.contains(i))
				continue;
			sim++;
		}
		// 5 + 9 - 4 = 10
		double den = ((double) (vec1_entries.size() + vec2_entries.size())) - sim;
		return sim / den;
	}

	public static List<Integer> intArrToList(int[] sig) {
		List<Integer> ret = new ArrayList<Integer>(sig.length);
		for (int n : sig) {
			ret.add(n);
		}
		return ret;
	}

	public static Double jaccardSimilarity(int[] sig1, int[] sig2) {
		if (sig1.length != sig2.length) {
			System.err.println("Warning: Rerun setup");
		}
		Set<Integer> set1 = new HashSet<Integer>(intArrToList(sig1));
		Set<Integer> set2 = new HashSet<Integer>(intArrToList(sig2));
		// Overlap: |{0, 1, 2, 5}| = 4
		// Denominator: |{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}| = 10
		int sim = 0;
		for (Integer i : set1) {
			if (!set2.contains(i))
				continue;
			sim++;
		}
		// 5 + 9 - 4 = 10
		double den = sig1.length + sig2.length - sim;
		return sim / den;
	}

	/**
	 * <b>WARNING</b>: Use this method only if a "anything that's above this
	 * threshold is fine" applies in the encompassing logic. Remember that this just
	 * makes sure that the threshold is reached, not that it is the maximal value
	 * nor best candidate that reached it. <br>
	 * Same as its non-threshold variant except that this one applies the prefix
	 * filtering principle, allowing for faster returns but for
	 * continuously-computed values
	 * 
	 * @param queryDocument
	 * @param doc2
	 * @param threshold
	 * @return
	 */
	public static boolean reachesThreshold(Set<Integer> set1, Set<Integer> set2, double threshold) {
		double sim = 0d;
		final double longer_size = Math.max(set1.size(), set2.size());
		final int shorter_size = Math.min(set1.size(), set2.size());
		int indexCounter = 0;
		// How far must we go?
		// Worst case: sim = 0
		// Best case: sim = curr length
		for (final Integer number : set1) {
			if (set2.contains(number)) {
				sim++;
			}
			if ((sim / longer_size) >= threshold) {
				// Final result is definitely already above, so jump out
				return true;
			}
			// Jump out if it's not possible to continue
			final double ratio = ((double) (sim + ((double) (shorter_size - indexCounter)))) / longer_size;
			// Ratio represents the ideal case from now on
			if (ratio < threshold) {
				// System.out.println("threshold(" + threshold + ") / Length(" + shorter_size +
				// ") - Cannot recover - Sim("
				// + sim + ") @ pos(" + indexCounter + "); Top poss ratio: " + ratio);
				return false;
			}
			indexCounter++;
		}
		// 5 + 9 - 4 = 10
		double den = ((double) (set1.size() + set2.size())) - sim;
		return (sim / den) >= threshold;

	}

	/**
	 * Calls {@link #reachesThreshold(Set, Set, double)} with vec1.getEntries() and
	 * vec2.getEntries()
	 * 
	 * @param queryVector
	 * @param otherVector
	 * @param threshold
	 * @return
	 */
	public static boolean reachesThreshold(LSHSparseVector<Boolean> queryVector, LSHSparseVector<Boolean> otherVector,
			double threshold) {
		Set<Integer> vec1_entries = queryVector.getEntries();// 0, 1, 2, 5, 7
		Set<Integer> vec2_entries = otherVector.getEntries();// 0, 1, 2, 3, 4, 5, 6, 8, 9
		// Overlap: |{0, 1, 2, 5}| = 4
		// Denominator: |{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}| = 10
		/*
		 * Threshold is a ratio threshold = 0.5 {0, 1, 2, 4, 5} {0, 5} 0 1 2 4 5 Sim: 1
		 * 0.5 0.3 0.25 0.4
		 */
		return reachesThreshold(vec1_entries, vec2_entries, threshold);
	}

	/**
	 * Returns TRUE if at least one item (on the same index) is the same
	 * 
	 * @param query_hash
	 * @param hash2
	 * @return
	 */
	public static boolean sameHashOnSameIndex(int[] query_hash, int[] hash2) {
		for (int i = 0; i < query_hash.length; ++i) {
			if (query_hash[i] == hash2[i]) {
				return true;
			}
		}
		return false;
	}
}
