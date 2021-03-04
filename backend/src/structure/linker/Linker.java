package structure.linker;

import java.util.Collection;

import structure.datatypes.Mention;
import structure.interfaces.Weighable;
import structure.utils.Loggable;

public interface Linker extends Weighable<Mention>, Loggable {

	public boolean init();

	public String annotate(final String input) throws Exception;

	public Collection<Mention> annotateMentions(final String input) throws Exception;

	public String getKG();

	@Override
	public int hashCode();

	@Override
	boolean equals(Object obj);

	public long getID();

	default int nullHash(Object o, int otherwise) {
		return o == null ? otherwise : o.hashCode();
	}
}
