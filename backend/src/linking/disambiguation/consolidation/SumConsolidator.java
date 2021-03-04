package linking.disambiguation.consolidation;

import structure.datatypes.PossibleAssignment;
import structure.linker.Linker;
import structure.utils.FunctionUtils;

/**
 * A simple type of consolidation, merging possible assignments by summing their
 * scores after modulating them based on their linkers' specifications (weight
 * and defined modulation function - e.g. when wanting to have a linker's scores
 * normalised/regularised or curved)
 * 
 * @author wf7467
 *
 */
public class SumConsolidator extends AbstractConsolidator {

	public SumConsolidator(final Linker... linkers) {
		super(linkers);
	}

	@Override
	public Number combineScore(PossibleAssignment leftAssignment, PossibleAssignment rightAssignment) {
//		leftVal == null ? 0 : leftVal.getScore().doubleValue())
//				+ (rightVal == null ? 0 : rightVal.getScore().doubleValue());
		return FunctionUtils.add(leftAssignment == null ? 0 : leftAssignment.getScore(),
				rightAssignment == null ? 0 : rightAssignment.getScore());
	}

}
