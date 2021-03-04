package linking.disambiguation;

import structure.datatypes.PossibleAssignment;
import structure.interfaces.pipeline.Disambiguator;

public interface MultiDisambiguator extends Disambiguator {
	public ScoreCombiner<PossibleAssignment> getScoreCombiner();

}
