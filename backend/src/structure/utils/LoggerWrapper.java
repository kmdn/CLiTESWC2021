package structure.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerWrapper {

	public final String logName;

	public LoggerWrapper(final String logName) {
		this.logName = logName;
	}

	public static LoggerWrapper getLogger(final String logName) {
		return createLogger(logName);
	}

	private static LoggerWrapper createLogger(String logName) {
		return new LoggerWrapper(logName);
	}

	public void warning(final String warnMsg) {
		warn(warnMsg);
	}

	// METHODS TO EDIT IN CASE LOGGER CHANGES BELOW \|/

	private Logger getLogger() {
		// initLogging();
		// return LogFactory.getLog(getClass());
		// return Logger.getLogger(getClass());
		// return LogManager.getLogger(getClass());
		// return Logger.getLogger(canonicalName);
		return Logger.getLogger(this.logName);
	}

	public void debug(final String msg) {
		// getLogger().warn(msg);
		getLogger().log(Level.FINE, msg);
	}

	public void info(final String msg) {
		// getLogger().warn(msg);
		getLogger().log(Level.INFO, msg);
	}

	public void warn(final String msg) {
		// getLogger().warn(msg);
		getLogger().log(Level.WARNING, msg);
	}

	public void error(final String msg) {
		// getLogger().error(msg);
		getLogger().log(Level.SEVERE, msg);
	}

	public void error(final String msg, Exception exc) {
		getLogger().log(Level.SEVERE, msg, exc);
	}

//	static void initLogging() {
//		try {
//			InputStream input = Loggable.class.getClassLoader().getResourceAsStream("log4j.properties");
//			Properties prop = new Properties();
//			prop.load(input);
//			PropertyConfigurator.configure(prop);
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.out.println("ERROR: Unable to load subscriptionlog4j.properties");
//		}
//	}

}
