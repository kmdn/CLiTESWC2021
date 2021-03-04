package structure.datatypes.lsh;

import java.util.HashSet;
import java.util.Set;

/**
 * Hit/Miss-based Sparse vector object used for LSH
 * 
 * @author Kristian Noullet
 *
 * @param <V> what type of value is stored in the vector
 */
public class LSHSparseVector<V> {
	private final Set<Integer> entries = new HashSet<>();
	private final int length;
	private final V missValue;
	private final V hitValue;

	/**
	 * Creates a Boolean-typed sparse vector of specified length (as it's the most
	 * common one used, at least for our use cases)
	 * 
	 * @param length size of vector
	 * @return Boolean-Typed Sparse Vector
	 */
	public static LSHSparseVector<Boolean> create(int length) {
		return new LSHSparseVector<Boolean>(length, false, true);
	}

	/**
	 * Creates a sparse vector of specified length and default hit and miss values
	 * @param length length of vector
	 * @param missValue value if no such entry exists
	 * @param hitValue value if entry exists
	 */
	LSHSparseVector(int length, V missValue, V hitValue) {
		this.length = length;
		this.missValue = missValue;
		this.hitValue = hitValue;
	}

	/**
	 * Returns hit/miss value, depending on whether value is within our collection of entries
	 * @param index whether an entry at the specified index exists
	 * @return defined hit or miss value
	 */
	public V get(int index) {
		if (index < 0 || index > length || !entries.contains(index)) {
			return missValue;
		} else {
			return hitValue;
		}
	}

	/**
	 * Returns all entries of this sparse vector
	 * @return entries within this sparse vector
	 */
	public Set<Integer> getEntries() {
		return this.entries;
	}

	/**
	 * Adds an entry to the sparse vector at the specified index
	 * @param index index at which an entry exists
	 */
	public void set(int index) {
		this.entries.add(index);
	}

	/**
	 * Transforms our hit-or-miss based sparse vector object into a boolean array
	 * <br>
	 * Note: Large vectors should not all be 'expanded' at the same time unless appropriate memory considerations have been made
	 * @return boolean array, representing this sparse vector
	 */
	public boolean[] toBooleanArray() {
		boolean[] arr = new boolean[length];
		final double loadRatioThreshold = 0.6;
		if (loadRatioThreshold * ((double) length) <= entries.size()) {
			// There are a lot of entries, so input entries alongside the miss values
			// Threshold at 0.3 -> if the entries fill up 30% of the vector or more
			for (int i = 0; i < length; ++i) {
				arr[i] = entries.contains(i) ? true : false;
			}
		} else {
			// There are few entries -> fill it out with default value, then with entries
			for (int i = 0; i < arr.length; ++i) {
				arr[i] = false;
			}
			for (Integer hitIndex : entries) {
				arr[hitIndex] = true;
			}
		}
		return arr;
	}
}
