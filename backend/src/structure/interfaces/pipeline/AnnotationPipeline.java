package structure.interfaces.pipeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;

import structure.datatypes.Mention;

public class AnnotationPipeline {
	private boolean finished = false;
	private final Map<String, AnnotationPipelineItem> pipelineItems = new HashMap<>();
	private final String keyResults = "results";
	private final AnnotationPipelineItem endItem;

	public AnnotationPipeline() {
		// end should be the results, likely have no appropriate component to execute
		// and be (in)directly connected to all the previous steps
//		final AnnotationPipelineItem endItem = createItem(keyResults, null);
//		this.endItem = endItem;
		this.endItem = addItem(this.keyResults, new PipelineComponent() {
			/**
			 * Handles what to do with the results in the end...<br>
			 * Right now it just adds results together to the result list if there are
			 * results from multiple sources...
			 */
			@Override
			public Collection<Collection<Mention>> execute(AnnotationPipelineItem callItem, String text) {
				// Combine the results of the multiple precedents
				final Collection<AnnotationPipelineItem> dependencies = callItem.getDependencies();
				final Collection<Collection<Mention>> retMentions = Lists.newArrayList();
				for (AnnotationPipelineItem dependency : dependencies) {
					final Collection<Collection<Mention>> results = dependency.getResults();
					if (results != null && results.size() > 0) {
						retMentions.addAll(results);
					}
				}
				return retMentions;
			}
		});
	}

	/**
	 * Adds an node to the graph
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addItem(final String ID, final PipelineComponent component,
			final EnumTypeComponent type) {
		final AnnotationPipelineItem item = createItem(ID, component, type);
		pipelineItems.put(ID, item);
		return item;
	}

	/**
	 * Adds an node to the graph<br>
	 * Adding an unspecified item through this makes its specific behaviour be
	 * executed through the components specific execute(...) function.<br>
	 * If you want to use the default one specified in AnnotationPipelineItem, use
	 * one of the addMD, addCG, ... or addItem(..., TYPE) where TYPE is the
	 * EnumTypeComponent defining the behaviour if the component is compatible
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addItem(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.UNSPECIFIED);
	}

	/**
	 * Adds an MD node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addMD(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.MD);
	}

	/**
	 * Adds a CG node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addCG(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.CG);
	}

	/**
	 * Adds an ED node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addED(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.ED);
	}

	/**
	 * Adds an CG_ED node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addCG_ED(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.CG_ED);
	}

	/**
	 * Adds an MD_CG_ED (=full Linker) node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addMD_CG_ED(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.MD_CG_ED);
	}

	/**
	 * Adds a Combiner node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addCombiner(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.COMBINER);
	}

	/**
	 * Adds a Splitter node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addSplitter(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.SPLITTER);
	}

	/**
	 * Adds a Transformer node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addTransformer(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.TRANSFORMER);
	}

	/**
	 * Adds a Translator node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addTranslator(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.TRANSLATOR);
	}

	/**
	 * Adds a Filter node to the graph<br>
	 * Difference compared to {@link #addItem(String, PipelineComponent)}:<br>
	 * This method makes the component behave in the default specified way, as
	 * specified in the switch-case in AnnotationPipelineItem
	 * 
	 * @param ID        string identifier
	 * @param component component which will be executed
	 * @return
	 */
	public AnnotationPipelineItem addFilter(final String ID, final PipelineComponent component) {
		return addItem(ID, component, EnumTypeComponent.FILTER);
	}

	/**
	 * 
	 * @param ID        identifier referring to the pipeline item
	 * @param component what should be executed
	 * @param type      type of component that will be executed. Important if
	 *                  default behaviour defined in AnnotationPipelineItem should
	 *                  be applied (if none is defined, it will take the one
	 *                  defined)
	 * @return
	 */
	private AnnotationPipelineItem createItem(final String ID, final PipelineComponent component,
			final EnumTypeComponent type) {
		return new AnnotationPipelineItem(ID, component, type);
	}

	public void addConnection(final String sourceID, final String targetID) {
		// If any of them does not yet exist --> needs to be added
		final AnnotationPipelineItem source = pipelineItems.get(sourceID);
		final AnnotationPipelineItem target = pipelineItems.get(targetID);
		if (source == null) {
			System.err.println("[" + sourceID + " --> " + targetID
					+ "] Source has not been added yet. Please add it prior to creating of connection. See #add");
			return;
		}
		if (target == null) {
			System.err.println("[" + sourceID + " --> " + targetID
					+ "] Target has not been added yet. Please add it prior to creation of connection. See #add");
			return;
		}
		if (source.getDependencies().contains(target)) {
			System.err.println("Warning: Skipping cyclic dependency " + sourceID + " -> " + targetID);
			return;
		}
		if (target.getTargets().contains(source)) {
			System.err.println("Warning: Skipping cyclic dependency " + sourceID + " -> " + targetID);
			return;
		}

		source.addTarget(target);
		target.addDependency(source);
	}

	public void execute(final String text) {
		// endItem is NOT in pipelineItems - otherwise it would add itself as a target
		// and dependency... which would be annoying
		// -----
		// Add connections to the final component (so we can grab dependencies through
		// it)
		// -----
		for (Map.Entry<String, AnnotationPipelineItem> e : this.pipelineItems.entrySet()) {
			final Collection<AnnotationPipelineItem> targets = e.getValue().getTargets();
			if (targets.size() <= 0 && !e.getKey().equals(this.keyResults)) {
				if (!e.getValue().getType().equals(EnumTypeComponent.MD_CG_ED) &&
						e.getValue().getDependencies().size() == 0) {
					// No full annotator and neither target nor dependency defined -> orphan, ignore
					System.err.println("Warning: '" + e.getKey() + "' had no targets and no dependencies defined, ignored");
				} else {
					// System.out.println("[" + e.getKey() + "] Sign me up for the end!");
					// no target to execute after, so it's the end...
					// aka. add connection to final step, so we can find it as a dependency
					addConnection(e.getKey(), this.endItem.getID());
					System.out.println("Info: '" + e.getKey() + "' had no targets defined, connected it with the end item");
				}
			} else {
				// System.out.println("[" + e.getKey() + "] Nope, not a final item!");
			}
		}

		resolve(this.endItem, text);
		finished = true;
	}

	/**
	 * Dependency resolution through proper execution order.
	 * 
	 * @param item
	 * @param text
	 */
	private void resolve(final AnnotationPipelineItem item, final String text) {
		AnnotationPipelineItem prevDependency = null;
		for (AnnotationPipelineItem dependency : item.getDependencies()) {
			resolve(dependency, text);
			prevDependency = dependency;
		}
		item.execute(prevDependency, text);
	}

	/**
	 * Executes pipeline if it hasn't been yet and returns final results. If it has
	 * been executed already, it will just return the results.
	 * 
	 * @param text
	 * @return
	 */
	public Collection<Collection<Mention>> getResults(final String text) {
		if (!finished) {
			execute(text);
		}
		return this.endItem.getResults();
	}

	/**
	 * Resets this entire pipeline, as well as all dependent AnnotationPipelineItem
	 * instances, so they may be reused for reuse with another input text
	 * (especially useful in the case of multiple sentences from data sets being
	 * sent out one by one)
	 * 
	 * @return
	 */
	public boolean reset() {
		reset(this.endItem);
		finished = false;
		return true;
	}

	/**
	 * Recursive reset to re-execute the pipeline if need be.
	 * 
	 * @param item item passed to backtrack through dependencies for reset
	 * @return
	 */
	public boolean reset(final AnnotationPipelineItem item) {
		boolean ret = true;
		for (AnnotationPipelineItem dependency : item.getDependencies()) {
			ret &= reset(dependency);
		}
		ret &= item.reset();
		// System.out.println("[" + item.getID() + "] Resetting");
		return ret;
	}

	@Override
	public String toString() {
		String pipelineItemsList = "";
		int i = 0;
		for (String pipelineItem : pipelineItems.keySet()) {
			if (i > 0) {
				pipelineItemsList += ", ";
			}
			pipelineItemsList += pipelineItem;
			i++;
		}
		return "Pipeline [" + pipelineItemsList + "]";
	}

}
