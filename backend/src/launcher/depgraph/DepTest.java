package launcher.depgraph;

import java.util.Collection;

import com.google.common.collect.Lists;

public class DepTest {

	private static int counter = 0;

	public static void main(String[] args) {
		new DepTest().run();
	}

	private void run() {
		final Collection<DepNode> nodes = Lists.newArrayList();

		final DepNode md1 = createNode("MD1", nodes);
		final DepNode md2 = createNode("MD2", nodes);
		final DepNode co1 = createNode("CO1", nodes);
		final DepNode cg1 = createNode("CG1", nodes);
		final DepNode md3 = createNode("MD3", nodes);
		cg1.dependencies.add(co1);
		co1.dependencies.add(md1);
		co1.dependencies.add(md2);
		
		resolve(cg1);
	}

	private static DepNode createNode(final String name, final Collection<DepNode> nodes) {
		final DepNode node = new DepNode(name);
		nodes.add(node);
		return node;
	}

	private void resolve(final DepNode node) {
		for (DepNode n : node.dependencies)
		{
			resolve(n);
		}
		System.out.println("Executing: "+node.name);
	}

}
