package linking.mentiondetection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import structure.config.constants.FilePaths;
import structure.config.kg.EnumModelType;

public class StopwordsLoader {

	private final String stopwordsPath;
	private Set<String> stopwords = null;

	public StopwordsLoader(final String stopwordsPath) {
		this.stopwordsPath = stopwordsPath;
	}

	public StopwordsLoader(final EnumModelType KG) {
		this(FilePaths.FILE_STOPWORDS.getPath(KG));
	}

	/**
	 * Gets the stopwords. If they have already been loaded, it does NOT load them
	 * again, but returns the old ones.<br>
	 * <b>Note</b>: If you want to be sure your stopwords are up-to-date with the
	 * file, always use {@link #load()}
	 * 
	 * @return stopwords
	 * @throws IOException
	 */
	public Set<String> getStopwords() throws IOException {
		if (stopwords == null) {
			this.stopwords = load();
		}
		return this.stopwords;
	}

	/**
	 * Loads stopwords from a defined file
	 * 
	 * @return stopwords
	 * @throws IOException
	 */
	public Set<String> load() throws IOException {
		final Set<String> stopwords = new HashSet<>();
		try (final BufferedReader brIn = new BufferedReader(new FileReader(stopwordsPath))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				stopwords.add(line);
			}
		}
		return stopwords;
	}
}
