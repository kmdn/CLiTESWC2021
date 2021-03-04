package experiment;

import org.json.simple.JSONObject;

import structure.config.constants.EnumTaskType;
import structure.config.kg.EnumModelType;
import structure.datatypes.AnnotatedDocument;

/**
 * Result of an Agnos experiment, defined by the linker, TODO and the annotated document
 * @author samuel
 *
 */
public class ExperimentTaskResult {
	
	private int experimentId;
	private int taskId;
	private EnumTaskType taskType;
	//private String dataset;
	private JSONObject annotationPipelineConfig;
	private EnumModelType knowledgeBase;
	private AnnotatedDocument document;
	
	public ExperimentTaskResult(int experimentId, int taskId, EnumTaskType taskType,
			JSONObject annotationPipelineConfig, String dataset, EnumModelType knowledgeBase,
			AnnotatedDocument document) {
		this.experimentId = experimentId;
		this.taskId = taskId;
		this.taskType = taskType;
		this.annotationPipelineConfig = annotationPipelineConfig;
		//this.dataset = dataset;
		this.knowledgeBase = knowledgeBase;
		this.document = document;
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

	public EnumTaskType getTaskType() {
		return taskType;
	}

	public void settaskType(EnumTaskType taskType) {
		this.taskType = taskType;
	}

	public JSONObject getAnnotationPipelineConfig() {
		return annotationPipelineConfig;
	}

	public void setAnnotationPipelineConfig(JSONObject annotationPipelineConfig) {
		this.annotationPipelineConfig = annotationPipelineConfig;
	}

	public EnumModelType getKnowledgeBase() {
		return knowledgeBase;
	}

	public void setKnowledgeBase(EnumModelType knowledgeBase) {
		this.knowledgeBase = knowledgeBase;
	}

	public AnnotatedDocument getDocument() {
		return document;
	}

	public void setDocument(AnnotatedDocument document) {
		this.document = document;
	}

}
