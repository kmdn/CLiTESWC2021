package structure.interfaces;

import structure.utils.Loggable;

public interface Executable extends Loggable {

	public void init();

	default public boolean reset() {
		return false;
	}

	default public <T> T exec() throws Exception {
		return exec(null);
	};

	public <T> T exec(Object... o) throws Exception;

	public boolean destroy();

	default public String getExecMethod() {
		return null;
	};

}
