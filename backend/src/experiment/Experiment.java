package experiment;

import java.util.ArrayList;
import java.util.List;

public class Experiment {

	private List<ExperimentTask> experimentTasks;

	public Experiment() {
		this.experimentTasks = new ArrayList<>();
	}

	public List<ExperimentTask> getExperimentTasks() {
		return experimentTasks;
	}

	public void setExperimentTasks(List<ExperimentTask> experimentTasks) {
		this.experimentTasks = experimentTasks;
	}

	public void addExperimentTask(ExperimentTask experimentTask) {
		this.experimentTasks.add(experimentTask);
	}

}
