package linking.disambiguation.consolidation;

import java.util.Collection;
import java.util.Map;

import structure.datatypes.Mention;
import structure.linker.Linker;

public interface Consolidator {


	
	public Map<String, Collection<Mention>> mergeByKG(
			Map<Linker, Collection<Mention>> mapLinkerMention);
}
