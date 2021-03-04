package launcher.depgraph;

import java.util.Collection;

import com.google.common.collect.Lists;

public class DepNode {
	public final String name;
	public final Collection<DepNode> dependencies = Lists.newArrayList();

	public DepNode(final String name) {
		this.name = name;
	}
}
