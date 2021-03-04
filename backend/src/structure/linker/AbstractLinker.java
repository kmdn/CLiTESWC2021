package structure.linker;

import java.util.concurrent.atomic.AtomicLong;

import structure.config.kg.EnumModelType;

public abstract class AbstractLinker implements Linker {
	private static AtomicLong counter = new AtomicLong(0);
	private final long id;

	protected final EnumModelType KG;

	public AbstractLinker(EnumModelType KG) {
		this.KG = KG;
		synchronized (counter) {
			this.id = counter.incrementAndGet();
		}
	}

	@Override
	public long getID() {
		return this.id;
	}

	@Override
	public String getKG() {
		return this.KG.name();
	}

	@Override
	public int hashCode() {
		return super.hashCode() + getClass().hashCode() + getKG().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Linker) {
			final Linker linker = (Linker) obj;
			return super.equals(obj) && (getID() == linker.getID()) && getClass().equals(obj.getClass())
					&& getKG().equals(linker.getKG());
		}
		return false;
		// super.equals(obj);
	}
}
