package structure.config.constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ExperimentTypeToLinkerMapper {
	
	//private final HashMap<Linker, Collection<ExperimentType>> mapping = new HashMap<>();
	private final HashMap<String, Collection<EnumTaskType>> mapping = new HashMap<>();
	
	public ExperimentTypeToLinkerMapper() {
		// Agnos
		List<EnumTaskType> experimentTypesAgnos = new ArrayList<EnumTaskType>();
		//experimentTypesAgnos.add(EnumTaskType.MD);
		//experimentTypesAgnos.add(EnumTaskType.CG_ED);
		experimentTypesAgnos.add(EnumTaskType.FULL);
		this.mapping.put("Agnos", experimentTypesAgnos);
		
		// AIDA
		List<EnumTaskType> experimentTypesAIDA = new ArrayList<EnumTaskType>();
		experimentTypesAIDA.add(EnumTaskType.MD);
		experimentTypesAIDA.add(EnumTaskType.CG_ED);
		experimentTypesAIDA.add(EnumTaskType.FULL);
		this.mapping.put("AIDA", experimentTypesAIDA);
		
		// Babelfy
		List<EnumTaskType> experimentTypesBabelfy = new ArrayList<EnumTaskType>();
		experimentTypesBabelfy.add(EnumTaskType.MD);
		experimentTypesBabelfy.add(EnumTaskType.CG_ED);
		experimentTypesBabelfy.add(EnumTaskType.FULL);
		this.mapping.put("Babelfy", experimentTypesBabelfy);
		
		// DBpediaSpotlight
		List<EnumTaskType> experimentTypesDBpediaSpotlight = new ArrayList<EnumTaskType>();
		experimentTypesDBpediaSpotlight.add(EnumTaskType.MD);
		experimentTypesDBpediaSpotlight.add(EnumTaskType.CG_ED);
		experimentTypesDBpediaSpotlight.add(EnumTaskType.FULL);
		this.mapping.put("DBpediaSpotlight", experimentTypesDBpediaSpotlight);
		
		// OpenTapioca
		List<EnumTaskType> experimentTypesOpenTapioca = new ArrayList<EnumTaskType>();
		//experimentTypesOpenTapioca.add(EnumTaskType.MD);
		//experimentTypesOpenTapioca.add(EnumTaskType.CG_ED);
		experimentTypesOpenTapioca.add(EnumTaskType.FULL);
		this.mapping.put("OpenTapioca", experimentTypesOpenTapioca);
		
		// MAG
		List<EnumTaskType> experimentTypesMAG = new ArrayList<EnumTaskType>();
		experimentTypesMAG.add(EnumTaskType.CG_ED);
		this.mapping.put("MAG", experimentTypesMAG);
	}
	
	/**
	 * Return the linkers that can perform a specific experiment type
	 * @param experimentType
	 * @return
	 */
	public List<String> getLinkerForExperimentType(EnumTaskType experimentType) {
		List<String> linkers = new ArrayList<>();
		for (String key : this.mapping.keySet()) {
			if (this.mapping.get(key).contains(experimentType)) {
				linkers.add(key);
			}
		}
		return linkers;
	}

}
