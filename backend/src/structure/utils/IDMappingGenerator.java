package structure.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import structure.config.constants.Strings;

public class IDMappingGenerator<V> implements AutoCloseable {
	// Key = prefix+counter; Value = URL or whatever value is given
	private DualHashBidiMap<String, V> mapping = new DualHashBidiMap<String, V>();
	private AtomicInteger counter = new AtomicInteger();
	private final boolean alwaysFlush;
	private final BufferedWriter humanOut;
	private final ObjectOutputStream machineOut;
	private final String tokenSeparator = Strings.ID_MAPPING_SEPARATOR.val;
	private final String prefix;

	public IDMappingGenerator(final File humanFile, final File machineFile, final boolean alwaysFlush)
			throws FileNotFoundException, IOException {
		this(humanFile, machineFile, alwaysFlush, "");
	}

	public IDMappingGenerator(final File humanFile, final File machineFile, final boolean alwaysFlush,
			final String prefix) throws FileNotFoundException, IOException {
		this.alwaysFlush = alwaysFlush;
		// Using a bw due to newline etc
		// Securing constructor NPE
		if (humanFile != null) {
			this.humanOut = new BufferedWriter(new FileWriter(humanFile));
		} else {
			this.humanOut = null;
		}

		// Securing constructor NPE
		if (machineFile != null) {
			this.machineOut = new ObjectOutputStream(new FileOutputStream(machineFile));
		} else {
			this.machineOut = null;
		}
		// Securing prefix value
		if (prefix == null) {
			this.prefix = "";
		} else {
			this.prefix = prefix;
		}
	}

	/**
	 * Gives counter value for specified input value that is either mapped or to be
	 * mapped (this method generates the counter values automatically)
	 * 
	 * @param val Mapped value / Value to be mapped
	 * @return Counter value corresponding to the value passed
	 * @throws IOException
	 */
	public synchronized String generateMapping(final V val) throws IOException {
		synchronized (mapping) {
			String key = mapping.getKey(val);
			if (key == null) {
				// Doesn't exist yet -> create + output + return
				key = prefix + counter.getAndIncrement();
				addMapping(key, val);
			}
			// Else: simply return it
			return key;
		}
	}

	public synchronized boolean exists(final V val) {
		synchronized (mapping) {
			return (mapping.getKey(val) != null);
		}
	}

	/**
	 * Returns associated value to the given mapping
	 * 
	 * @param i counter value mapped to wanted V-type object
	 * @return object that i is mapped to
	 */
	public synchronized V getMappedValue(final String i) {
		synchronized (mapping) {
			final V val = mapping.get(i);
			if (val == null) {
				throw new RuntimeException("Requested value(" + i + ") without valid mapping. Current counter is @ "
						+ counter.get() + " and prefix(" + this.prefix + ")");
			}
			return val;
		}
	}

	/**
	 * Writes out the key and value pairing (done when a new value is mapped)
	 * 
	 * @param key
	 * @param val
	 * @throws IOException
	 */
	private synchronized void write(final String key, final V val) throws IOException {
		if (this.humanOut != null) {
			synchronized (this.humanOut) {
				// Bufferedwriters are thread-safe, but just in case it were to be changed to
				// another type of writer that isn't
				this.humanOut.write(key + tokenSeparator + val.toString());
				this.humanOut.newLine();
			}
			if (alwaysFlush) {
				humanOut.flush();
			}
		}

		if (alwaysFlush && this.machineOut != null) {
			synchronized (this.machineOut) {
				this.machineOut.reset();
				synchronized (mapping) {
					this.machineOut.writeObject(mapping);
				}
			}
		}
	}

	/*
	 * Closes human readable and machine readable output objects (outputting the
	 * newest state into them)
	 * 
	 * @throws IOException
	 */
	public synchronized void close() throws IOException {
		if (this.humanOut == null && this.machineOut == null) {
			System.err.println(
					"Mapping not saved anywhere. If this is not intended as a kind of one-way hashing, please fix.");
		}
		if (this.humanOut != null) {
			synchronized (this.humanOut) {
				this.humanOut.close();
			}
		}
		if (this.machineOut != null) {
			synchronized (this.machineOut) {
				this.machineOut.reset();
				this.machineOut.writeObject(mapping);
				this.machineOut.close();
			}
		}
	}

	/**
	 * Inserts passed map's values (keys and each associated value) into this
	 * instance's map, therewith practically adding already existing mappings<br>
	 * Note: If the keys already exist in the map, they will be overwritten
	 * 
	 * @param map
	 * @throws IOException
	 */
	public void insertValues(final Map<String, V> map) throws IOException {
		for (Map.Entry<String, V> e : map.entrySet()) {
			addMapping(e.getKey(), e.getValue());
		}
	}

	/**
	 * Adds the passed mapping to the map and outputs it to the defined file<br>
	 * Note: Flushing is dependent on setup arguments
	 * 
	 * @param key
	 * @param val
	 * @throws IOException
	 */
	private void addMapping(String key, V val) throws IOException {
		this.mapping.put(key, val);
		write(key, val);
	}
}
