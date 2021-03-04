package structure.utils;

public interface Loggable {

	public default LoggerWrapper getLogger() {
		return getLogger(getClass().getName());
	}

	public static LoggerWrapper getLogger(Class<?> clazz) {
		return getLogger(clazz.getCanonicalName());
	}

	static LoggerWrapper getLogger(String canonicalName) {
		return LoggerWrapper.getLogger(canonicalName);
	}

	public default void warn(final String msg) {
		// getLogger().warn(msg);
		getLogger().warn(msg);
	}

	public default void error(final String msg) {
		// getLogger().error(msg);
		getLogger().error(msg);
	}

}
