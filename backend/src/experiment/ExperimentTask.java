package experiment;

import org.json.simple.JSONObject;

import structure.config.constants.EnumTaskType;
import structure.config.kg.EnumModelType;
import structure.interfaces.pipeline.AnnotationPipeline;

public class ExperimentTask {

	private int experimentId;
	private int taskId;
	private EnumTaskType experimentType;
	private JSONObject annotationPipelineConfig;
	private AnnotationPipeline annotationPipeline;
	private String dataset;
	private String inputText;
	private EnumModelType knowledgeBase;

	public ExperimentTask(int experimentId, int taskId, EnumTaskType experimentType, 
			JSONObject annotationPipelineConfig, AnnotationPipeline annotationPipeline,
			String dataset, String inputText, EnumModelType knowledgeBase) {
		this.experimentId = experimentId;
		this.taskId = taskId;
		this.experimentType = experimentType;
		this.annotationPipelineConfig = annotationPipelineConfig;
		this.annotationPipeline = annotationPipeline;
		this.dataset = dataset;
		this.inputText = inputText;
		this.knowledgeBase = knowledgeBase;
	}

	public int getExperimentId() {
		return experimentId;
	}

	public void setExperimentId(int experimentId) {
		this.experimentId = experimentId;
	}

	public int getTaskId() {
		return taskId;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	public EnumTaskType getExperimentType() {
		return experimentType;
	}

	public void setExperimentType(EnumTaskType experimentType) {
		this.experimentType = experimentType;
	}

	public AnnotationPipeline getAnnotationPipeline() {
		return annotationPipeline;
	}

	public void setAnnotationPipeline(AnnotationPipeline annotationPipeline) {
		this.annotationPipeline = annotationPipeline;
	}

	public String getDataset() {
		return dataset;
	}

	public void setDataset(String dataset) {
		this.dataset = dataset;
	}

	public String getInputText() {
		return inputText;
	}

	public void setInputText(String inputText) {
		this.inputText = inputText;
	}

	public EnumModelType getKnowledgeBase() {
		return knowledgeBase;
	}

	public void setKnowledgeBase(EnumModelType knowledgeBase) {
		this.knowledgeBase = knowledgeBase;
	}

	public JSONObject getPipelineConfig() {
		return annotationPipelineConfig;
	}

}
