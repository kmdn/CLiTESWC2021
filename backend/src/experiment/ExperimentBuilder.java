package experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import linking.disambiguation.linkers.AgnosLinker;
import linking.disambiguation.linkers.AidaLinker;
import linking.disambiguation.linkers.BabelfyLinker;
import linking.disambiguation.linkers.DBpediaSpotlightLinker;
import linking.disambiguation.linkers.MAGLinker;
import linking.disambiguation.linkers.OpenTapiocaLinker;
import structure.config.constants.EnumTaskType;
import structure.config.kg.EnumModelType;
import structure.interfaces.pipeline.AnnotationPipeline;

public class ExperimentBuilder {

	private final String experimentData;
	private final int experimentId;

	// TODO get from agnos mini project
	private static final String DEFAULT_LINKER = "DBpediaSpotlight";
	private static final EnumTaskType DEFAULT_TASKTYPE = EnumTaskType.FULL;
	private static final String DEFAULT_DATASET = ""; // TODO
	private static final EnumModelType DEFAULT_KNOWLEDGEBASE = EnumModelType.DEFAULT;

	/**
	 * Map assigning linker names (strings) to linker classes 
	 */
	//TODO get from agnos_mini project
	private static final Map<String, String> linkerClasses = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("AIDA", AidaLinker.class.getName());
			put("Agnos", AgnosLinker.class.getName());
			put("DBpediaSpotlight", DBpediaSpotlightLinker.class.getName());
			put("Babelfy", BabelfyLinker.class.getName());
			put("MAG", MAGLinker.class.getName());
			put("OpenTapioca", OpenTapiocaLinker.class.getName());
		}};

	public ExperimentBuilder(int experimentId, String experimentData) {
		this.experimentId = experimentId;
		this.experimentData = experimentData;
	}

	public Experiment buildExperiment() {
		Experiment experiment = new Experiment();
		
		Object obj = JSONValue.parse(experimentData);
		JSONObject configuration = (JSONObject) obj;

		String taskTypeString = (String) configuration.get("taskType");
		EnumTaskType taskType = readTaskType(taskTypeString);

		JSONArray annotationPipelinesJson = (JSONArray) configuration.get("linkerConfigs");
		
		JSONArray jsonDataset = (JSONArray) configuration.get("datasets");
		String[] datasets = readDatasets(jsonDataset);

		String inputText = (String) configuration.get("inputText");
		
		JSONArray knowledgeBasesJson = (JSONArray) configuration.get("knowledgeBases");
		List<EnumModelType> knowledgeBases = readKnowledgeBases(knowledgeBasesJson);
		
		for (String dataset : datasets) {
			for (EnumModelType knowledgeBase : knowledgeBases) {
				if (annotationPipelinesJson != null && annotationPipelinesJson.size() > 0) {
					for (Object pipelineConfigObj : annotationPipelinesJson) {
					    if (pipelineConfigObj instanceof JSONObject) {
					        JSONObject annotationPipelineConfig = (JSONObject) pipelineConfigObj;

					        int taskId = (int) (long) annotationPipelineConfig.get("id");
		
							AnnotationPipelineBuilder annotationPipelineBuilder = 
									new AnnotationPipelineBuilder(annotationPipelineConfig,
											knowledgeBase, linkerClasses);
							AnnotationPipeline annotationPipeline =
									annotationPipelineBuilder.buildAnnotationPipeline();
	
							ExperimentTask experimentTask = new ExperimentTask(experimentId, taskId,
									taskType, annotationPipelineConfig, annotationPipeline,
									dataset, inputText, knowledgeBase);
							experiment.addExperimentTask(experimentTask);
					    } else {
							System.err.println("Warning: Problem reading JSON");
					    	//TODO exception handling
					    }
					}
				} else {
					//TODO exception handling
					System.out.println("Info: No annotator specified, using default (" +
							DEFAULT_LINKER + ")");
					AnnotationPipeline annotationPipeline = new AnnotationPipeline();
					// TODO init default annotator and create annotationPipelineConfig JSON
					ExperimentTask experimentTask = new ExperimentTask(experimentId, 1,
							taskType, null, annotationPipeline, dataset, inputText, knowledgeBase);
					experiment.addExperimentTask(experimentTask);
				}
			}
		}

		return experiment;
	}

	/**
	 * Read experiment type from JSON
	 * @param taskTypeString
	 * @return experiment type
	 */
	private EnumTaskType readTaskType(String taskTypeString) {
		EnumTaskType taskType = null;
		if (taskTypeString == null) {
			taskType = DEFAULT_TASKTYPE;
			System.out.println("Warning: Experiment type not specified, using default (" +
					DEFAULT_TASKTYPE.getLabel() + ")");
		} else {
			try {
				taskType = EnumTaskType.valueOf(taskTypeString);
				System.out.println("Type: " + taskTypeString);
			} catch (IllegalArgumentException e) {
				taskType = DEFAULT_TASKTYPE;
				System.out.println("Warning: Wrong experiment type (\"" + taskTypeString +
						"\"), using default");
			}
		}
		return taskType;
	}

	/**
	 * Read dataset names from JSON
	 * @param jsonDataset
	 * @return Array of dataset names
	 */
	private String[] readDatasets(JSONArray jsonDataset) {
		String[] datasets = null;
		if (jsonDataset == null || jsonDataset.size() == 0) {
			datasets = new String[1];
			datasets[0] = DEFAULT_DATASET;
			System.out.println(
					"Warning: datasets not specified, using default (" + DEFAULT_DATASET + ")");
		} else {
			datasets = new String[jsonDataset.size()];
			for (int i = 0; i < jsonDataset.size(); i++) {
				datasets[i] = (String) jsonDataset.get(i);
			}
			System.out.println("Datasets: " + Arrays.toString(datasets));
		}
		return datasets;
	}

	/**
	 * Read knowledge bases from JSON and create a list of EnumModelType
	 * @param knowledgeBasesJson
	 * @return List of EnumModelType
	 */
	private List<EnumModelType> readKnowledgeBases(JSONArray knowledgeBasesJson) {
		List<EnumModelType> knowledgeBases = new ArrayList<>();
		if (knowledgeBasesJson == null || knowledgeBasesJson.size() == 0) {
			System.out.println("Warning: Knowledge bases not specified, using default (" +
					DEFAULT_KNOWLEDGEBASE.toString() + ")");
			knowledgeBases.add(DEFAULT_KNOWLEDGEBASE);
		} else {
			for (int i = 0; i < knowledgeBasesJson.size(); i++) {
				String knowledgeBaseString = (String) knowledgeBasesJson.get(i);
				try {
					EnumModelType knowledgeBase = EnumModelType.valueOf(knowledgeBaseString);
					knowledgeBases.add(knowledgeBase);
				} catch (IllegalArgumentException e) {
					System.out.println("Warning: Invalid knowledge base type (" +
							knowledgeBaseString + "), skipping");
					knowledgeBases.add(DEFAULT_KNOWLEDGEBASE);
				}
			}
			if (knowledgeBases.size() == 0) {
				knowledgeBases.add(DEFAULT_KNOWLEDGEBASE);
			}
			System.out.println("Knowledge bases: " + knowledgeBases.toString());
		}
		return knowledgeBases;
	}
	
	/**
	 * Helper method to access private readTaskType; TODO cleaner solution?
	 * @param taskTypeString
	 * @return
	 */
	public EnumTaskType getTaskType(String taskTypeString) {
		return readTaskType(taskTypeString);
	}

}
