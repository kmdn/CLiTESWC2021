package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import experiment.Experiment;
import experiment.ExperimentBuilder;
import experiment.ExperimentTaskResult;
import experiment.Experimenter;
import structure.config.constants.EnumTaskType;
import structure.config.constants.ExperimentTypeToLinkerMapper;

@Controller
public class IndexController {

	// TODO Make it a HashMap with experiment IDs
	private int experimentIdCounter = 0;
	private List<ExperimentTaskResult> lastTaskResults = null;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		System.out.println("Home Page Requested, locale = " + locale);
		return "index";
	}

	@RequestMapping(value = "/result", method = RequestMethod.GET)
	public String result() {
		System.out.println("Show result");
		return "result";
	}
	
	@RequestMapping("/jsonresult")
	public @ResponseBody List<ExperimentTaskResult> getResult(
			@RequestParam(value = "id", required=false) String id) {
		System.out.println("JSON result requested for experiment " + id);
		return this.lastTaskResults;
	}
	
	// TODO Replace by /result?id=<ID>, and look it up in the HashMap
	@RequestMapping("/lastresult")
	public @ResponseBody List<ExperimentTaskResult> lastResult() {
		return this.lastTaskResults;
	}

	/**
	 * Returns a list of available linkers
	 * @param experimentTypeString
	 * @return
	 */
	@RequestMapping("/linkers")
	public @ResponseBody List<String> getLinkers(
			@RequestParam(value = "experimentType", required=false) String experimentTypeString) {
		EnumTaskType experimentType = (new ExperimentBuilder(0, null))
				.getTaskType(experimentTypeString);
		List<String> linkers = new ExperimentTypeToLinkerMapper()
				.getLinkerForExperimentType(experimentType);
        Collections.sort(linkers);
		return linkers;
	}

	/**
	 * Returns a list of available inter-component processors or possible values for a specific
	 * inter-component processor type
	 * @param icpTypeString
	 * @return
	 */
	@RequestMapping("/icps")
	public @ResponseBody List<String> getICPs(
			@RequestParam(value = "icpType", required=false) String icpTypeString) {
		List<String> icps = new ArrayList<>();
		if (icpTypeString.equals("combiner")) {
			Collections.addAll(icps, "Intersect", "Union");
		} else if (icpTypeString.equals("splitter")) {
			Collections.addAll(icps, "Copy");
		} else {
//			Collections.addAll(icps, "combiner", "splitter", "filter", "translator");
			Collections.addAll(icps, "combiner", "splitter");
		}
		return icps;
	}

	/**
	 * 
	 * @param experimentData
	 * @return
	 */
	@RequestMapping("/execute")
	public @ResponseBody List<ExperimentTaskResult> execute(
			@RequestParam(value = "experimentData") String experimentData) {
		System.out.println("Got request with experimentData=" + experimentData);
		ExperimentBuilder experimentBuilder = new ExperimentBuilder(getNextExperimentId(),
				experimentData);
		Experiment experiment = experimentBuilder.buildExperiment();
		Experimenter experimenter = new Experimenter(experiment);
		List<ExperimentTaskResult> results = experimenter.run();
		lastTaskResults = results;
		return results;
	}


	/**
	 * Returns the next free experiment ID and increases the counter for the experiment IDs by one
	 * @return the next free experiment ID
	 */
	private int getNextExperimentId() {
		int nextExperimentId = this.experimentIdCounter + 1;
		this.experimentIdCounter = nextExperimentId;
		return nextExperimentId;
	}

}
