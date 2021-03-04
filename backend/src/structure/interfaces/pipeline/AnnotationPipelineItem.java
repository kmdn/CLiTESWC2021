package structure.interfaces.pipeline;

import java.util.Collection;

import com.google.common.collect.Lists;

import structure.datatypes.Mention;
import structure.interfaces.CandidateGeneratorDisambiguator;
import structure.linker.Linker;
import structure.utils.MentionUtils;

/**
 * Class acting as a dependency node with details regarding execution and
 * results of said execution
 * 
 * @author wf7467
 *
 */
public class AnnotationPipelineItem {
	// What needs to be done prior to this
	private Collection<AnnotationPipelineItem> dependencies = Lists.newArrayList();
	// What to do next
	private Collection<AnnotationPipelineItem> targets = Lists.newArrayList();

	private final String id;

	// What may be executed
	private final PipelineComponent component;
	private boolean done = false;
	private Collection<Collection<Mention>> results = null;

	private final EnumTypeComponent type;

	public AnnotationPipelineItem(final String id, final PipelineComponent component, final EnumTypeComponent type) {
		this.id = id;
		this.component = component;
		this.type = type;
	}

	public String getID() {
		return this.id;
	}

	public PipelineComponent getComponent() {
		return this.component;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - -
	// - - - - - - - - - - Dependencies - - - - - - - - - -
	// - - - - - - - - - - - - - - - - - - - - - - - - - -
	public Collection<AnnotationPipelineItem> getDependencies() {
		return this.dependencies;
	}

	public void setDependencies(final Collection<AnnotationPipelineItem> dependencies) {
		this.dependencies.clear();
		this.dependencies.addAll(dependencies);
	}

	public void addDependency(final AnnotationPipelineItem dependency) {
		this.dependencies.add(dependency);
	}

	// - - - - - - - - - - - - - - - - - - - - - - - -
	// - - - - - - - - - - Targets - - - - - - - - - -
	// - - - - - - - - - - - - - - - - - - - - - - - -
	public Collection<AnnotationPipelineItem> getTargets() {
		return this.targets;
	}

	public void setTargets(final Collection<AnnotationPipelineItem> targets) {
		this.targets.clear();
		this.targets.addAll(targets);
	}

	public void addTarget(final AnnotationPipelineItem target) {
		this.targets.add(target);
	}

	// - - - - - - - - - - - - - - - - - - - - - - - -
	// Whether computation has finished
	// - - - - - - - - - - - - - - - - - - - - - - - -
	public void finished() {
		this.done = true;
	}

	public boolean isFinished() {
		return this.done;
	}

	public void setResults(final Collection<Collection<Mention>> mentions) {
		this.results = MentionUtils.copyMultiMentions(mentions);
	}

	public Collection<Collection<Mention>> getResults() {
		return this.results;
	}

	public void execute(final AnnotationPipelineItem prevDependency, final String text) {
		synchronized (this) {
			if (isFinished()) {
				System.out.println("[" + getID() + "] Already finished. You may grab results.");
				return;
			}
			System.out.println("[" + getID() + "] Executing");
			// Calls are always done as follows:
			// mentions; params=text, predecessors, successors
			if (getComponent() != null) {
				// Components get their data through this AnnotationPipelineItem, grabbing the
				// dependencies and their results

				Collection<Collection<Mention>> results = null;
				// System.out.println("Exec type: " + getType());
				EnumTypeComponent type = getType();
				switch (type) {
				case MD:
					results = md(text);
					break;
				case CG:
					results = cg();
					break;
				case CG_ED:
					results = cg_ed(text);
					break;
				case ED:
					results = ed(text);
					break;
				case MD_CG_ED:
					results = md_cg_ed(text);
					break;
				case COMBINER:
				case UNSPECIFIED:
				case FILTER:
				case SPLITTER:
				case TRANSFORMER:
				case TRANSLATOR:
				default:
					// System.out.println("DEFAULT CASE: Specific execution.");
					results = getComponent().execute(this, text);
					break;
				}
				setResults(results);
			}
			finished();
			// System.out.println("[" + getID() + "] Finished - Result: " + getResults());
		}
	}

	private Collection<Collection<Mention>> md(final String text) {
		if (EnumTypeComponent.MD.isInstance(getComponent())) {
			final Collection<Mention> mentions = ((MentionDetector) getComponent()).detect(text);
			final Collection<Collection<Mention>> results = Lists.newArrayList();
			results.add(mentions);
			return results;
		} else {
			throw new RuntimeException("Component class(" + (getComponent() == null ? null : getComponent().getClass())
					+ ") does not match expected type");
		}
	}

	private Collection<Collection<Mention>> cg() {
		if (EnumTypeComponent.CG.isInstance(getComponent())) {

			final Collection<AnnotationPipelineItem> dependencies = getDependencies();
			if (dependencies.size() == 1) {
				final Collection<Collection<Mention>> multiMentions = MentionUtils
						.copyMultiMentions(dependencies.iterator().next().getResults());
				if (multiMentions.size() != 1) {
					throw new RuntimeException(
							"Invalid number of arguments passed: multiMentions size[" + multiMentions.size() + "]");
				}

				final Collection<Mention> mentions = multiMentions.iterator().next();
				((CandidateGenerator) getComponent()).generate(mentions);
				final Collection<Collection<Mention>> retMentions = Lists.newArrayList();
				retMentions.add(mentions);
				return retMentions;
			} else {
				throw new IllegalArgumentException("Invalid number of dependencies for this component...");
			}

		} else {
			throw new RuntimeException("Component class(" + (getComponent() == null ? null : getComponent().getClass())
					+ ") does not match expected type");
		}

	}

	private Collection<Collection<Mention>> md_cg_ed(final String text) {
		if (EnumTypeComponent.MD_CG_ED.isInstance(getComponent())) {
			try {
				final Collection<Mention> mentions = MentionUtils
						.copyMentions(((Linker) getComponent()).annotateMentions(text));
				final Collection<Collection<Mention>> multiMentions = Lists.newArrayList();
				multiMentions.add(mentions);
				return multiMentions;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			throw new RuntimeException("Component class(" + (getComponent() == null ? null : getComponent().getClass())
					+ ") does not match expected type");
		}

	}

	private Collection<Collection<Mention>> cg_ed(final String text) {
		if (EnumTypeComponent.CG_ED.isInstance(getComponent())) {

			final Collection<AnnotationPipelineItem> dependencies = getDependencies();
			if (dependencies.size() == 1) {
				final Collection<Collection<Mention>> multiMentions = MentionUtils
						.copyMultiMentions(dependencies.iterator().next().getResults());
				if (multiMentions.size() != 1) {
					throw new RuntimeException(
							"Invalid number of arguments passed: multiMentions size[" + multiMentions.size() + "]");
				}

				if (multiMentions.size() != 1) {
					throw new IllegalArgumentException(
							"Wrong amount of mentions: multiMentions[" + multiMentions.size() + "]");
				}
				final Collection<Mention> mentions = ((CandidateGeneratorDisambiguator) getComponent())
						.generateDisambiguate(text, multiMentions.iterator().next());
				final Collection<Collection<Mention>> retMentions = Lists.newArrayList();
				retMentions.add(mentions);
				return retMentions;
			} else {
				throw new IllegalArgumentException("Invalid number of dependencies for component " + this.getID());
			}
		} else {
			throw new RuntimeException("Component class(" + (getComponent() == null ? null : getComponent().getClass())
					+ ") does not match expected type");
		}
	}

	private Collection<Collection<Mention>> ed(String text) {
		if (EnumTypeComponent.ED.isInstance(getComponent())) {
			final Collection<AnnotationPipelineItem> dependencies = getDependencies();
			if (dependencies.size() == 1) {
				try {
					final Collection<Collection<Mention>> multiMentions = MentionUtils
							.copyMultiMentions(dependencies.iterator().next().getResults());
					// Reject if it doesn't fit
					if (multiMentions.size() != 1) {
						throw new RuntimeException(
								"Invalid number of arguments passed: multiMentions size[" + multiMentions.size() + "]");
					}

					// Should only be one
					final Collection<Mention> mentions = multiMentions.iterator().next();
					// Disambiguator for the first incoming one
					final Collection<Mention> entities = ((Disambiguator) getComponent()).disambiguate(text, mentions);
					// make it a collection of collections
					final Collection<Collection<Mention>> retEntities = Lists.newArrayList();
					retEntities.add(entities);
					return retEntities;
				} catch (InterruptedException e) {
					System.err.println("[ERROR] Exception thrown while disambiguating...:");
					e.printStackTrace();
					return null;
				}
			} else {
				throw new IllegalArgumentException("Invalid number of dependencies["
						+ (getDependencies() == null ? "null" : getDependencies().size()) + "] for this component...");
			}
		} else {
			throw new RuntimeException("Component class(" + (getComponent() == null ? null : getComponent().getClass())
					+ ") does not match expected type");
		}
	}

	public EnumTypeComponent getType() {
		return this.type;
	}

	/**
	 * Resets this pipeline item by removing prior results and switching the "done"
	 * FLAG to FALSE<br>
	 * After calling this reset, this pipeline item may be used again.
	 * 
	 * @return true if properly reset
	 */
	public boolean reset() {
		this.done = false;
		if (this.results != null) {
			this.results.clear();
		}
		return true;
	}

	@Override
	public String toString() {
		return "AnnotationPipelineItem [" + id + ", done=" + done + "]";
	}
}
