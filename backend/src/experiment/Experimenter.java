package experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import structure.datatypes.AnnotatedDocument;
import structure.datatypes.Mention;

public class Experimenter {

	private Experiment experiment;

	public Experimenter(Experiment experiment) {
		this.experiment = experiment;
	}

	public List<ExperimentTaskResult> run() {
		
		List<ExperimentTaskResult> results = new ArrayList<>();

		for (ExperimentTask experimentTask : experiment.getExperimentTasks()) {
			// TODO dataset or inputText, currently inputText only
			experimentTask.getAnnotationPipeline().execute(experimentTask.getInputText());
			Collection<Collection<Mention>> entitiesCol =
					experimentTask.getAnnotationPipeline().getResults(experimentTask.getInputText());
			Collection<Mention> entities = entitiesCol.iterator().next();
			AnnotatedDocument document = new AnnotatedDocument(experimentTask.getInputText(), entities);
			ExperimentTaskResult result = new ExperimentTaskResult(experimentTask.getExperimentId(),
					experimentTask.getTaskId(), experimentTask.getExperimentType(),
					experimentTask.getPipelineConfig(), experimentTask.getDataset(),
					experimentTask.getKnowledgeBase(), document);
			results.add(result);
		}

		return results;
	}
}
