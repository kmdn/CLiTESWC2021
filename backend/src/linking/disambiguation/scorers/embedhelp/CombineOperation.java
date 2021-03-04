package linking.disambiguation.scorers.embedhelp;

import java.util.function.BiFunction;

/**
 * Enumeration handling different types of combination procedures of scores (for
 * embedding similarities) through use of static BiFunctions
 * 
 * @author Kristian Noullet
 *
 */
public enum CombineOperation {
	OCCURRENCE(ClusterItemPicker::occurrenceOperation), //
	MAX_SIM(ClusterItemPicker::maxedOperation), //
	SIM_ADD(ClusterItemPicker::similarityOperation), //
	SIM_SQUARE_ADD(ClusterItemPicker::similaritySquaredOperation),//

	;
	public final BiFunction<Double, Double, Double> combineOperation;

	CombineOperation(BiFunction<Double, Double, Double> combineOperation) {
		this.combineOperation = combineOperation;
	}
}
