package linking.disambiguation.scorers.embedhelp;

import java.util.function.BiFunction;

/**
 * Abstract class for picking items from specific clusters (disambiguation step
 * utilised with the embeddings)
 * 
 * @author Kristian Noullet
 *
 */
public abstract class AbstractClusterItemPicker implements ClusterItemPicker {
	public final BiFunction<Double, Double, Double> combinationOperation;

	protected AbstractClusterItemPicker(final BiFunction<Double, Double, Double> combinationOperation) {
		this.combinationOperation = combinationOperation;
	}

	/**
	 * Method executing the wanted operation for grouping of entities for the
	 * specified surface forms<br>
	 * Note: Pretty much fulfills the role of a reward function which in the end
	 * determines which entity is disambiguated to
	 * 
	 * @param previousValue     previous value within map
	 * @param pairSimilaritySum the cosine similarity that might want to be summed
	 * @return value resulting of the operation
	 */
	protected Double applyOperation(Double existingPairValue, Double right) {
		return getCombinationOperation().apply(existingPairValue, right);
	}

	@Override
	public BiFunction<Double, Double, Double> getCombinationOperation() {
		return this.combinationOperation;
	}
}
