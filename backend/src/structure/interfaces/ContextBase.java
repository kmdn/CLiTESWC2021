package structure.interfaces;

import java.util.Collection;

/**
 * Interface for context-relevant classes which should link their context and
 * occasionally update it
 * 
 * @author Kristian Noullet
 *
 * @param <M> type of context collection
 */
public interface ContextBase<M> {
	/**
	 * Links context to this context base
	 * @param context
	 */
	public void linkContext(Collection<M> context);

	/**
	 * Generally meant for some stuff that should be executed upon context changing,
	 * e.g. when something should be computed only once and not every time an item
	 * is to be scored
	 */
	public void updateContext();

}
