package structure;

import java.util.LinkedList;

import org.apache.jena.ext.com.google.common.collect.Lists;

import structure.interfaces.Executable;
import structure.utils.Stopwatch;

/**
 * Overall execution pipeline, executing Executable instances through the passed
 * arguments <br>
 * <b>Note</b>: Pipeline itself is also an executable, meaning that one Pipeline
 * may execute another sub-pipeline
 * 
 * @author Kristian Noullet
 *
 */
public class Pipeline implements Executable {
	private final LinkedList<Executable> subprograms = Lists.newLinkedList();
	private final LinkedList<Object[]> params = Lists.newLinkedList();
	private boolean tracking = false;// TODO: Implement tracking system to see which program we are at...
	private String pipestep = "";
	private boolean output = false;
	private long firstStartTime, startTime, endTime;

	public final void queue(final Executable prog, final Object... o) {
		subprograms.add(prog);
		params.add(o);
	}

	@Override
	public void init() {
	}

	@Override
	public boolean reset() {
		subprograms.clear();
		params.clear();
		this.output = true;
		return false;
	}

	@Override
	public Boolean exec(Object... o) throws Exception {
		final String watchName = getClass().getName();
		Stopwatch.start(watchName);
		final String subWatchName = "pipeline";
		while (subprograms.size() > 0) {
			Stopwatch.start(subWatchName);
			final Executable prog = subprograms.removeFirst();
			this.pipestep = prog.getClass().getCanonicalName();
			if (output) {
				System.out.println("[" + pipestep + "] Starting");
			}
			prog.exec(params.removeFirst());
			if (output) {
				System.out.println("[" + pipestep + "] Finished in " + Stopwatch.endDiffStart(subWatchName) + " ms");
			}
		}
		System.out.println("Finished pipeline in " + Stopwatch.endDiff(watchName) + " ms");
		return true;
	}

	@Override
	public boolean destroy() {
		init();
		return false;
	}

	public void setOutput(boolean b) {
		this.output = true;

	}

	@Override
	public String getExecMethod() {
		// Shouldn't have anything else as it's the working pipeline
		return null;
	}

}
