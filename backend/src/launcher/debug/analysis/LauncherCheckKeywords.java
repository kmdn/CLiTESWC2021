package launcher.debug.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import linking.mentiondetection.exact.HashMapCaseInsensitive;
import structure.utils.Stopwatch;

public class LauncherCheckKeywords {

	public static void main(String[] args) {
		// Keywords stuff
		final String inPathKeywords = "./keywords/keywords_reuters30.txt";
		final File inFileKeywords = new File(inPathKeywords);
		// KG Stuff
		final String inPathKG = "/vol2/cb/crunchbase-201806/dumps/crunchbase-dump-2018-06_sanitized.nt";

		final File inFileKG = new File(inPathKG);
		final Map<String, Integer> counterMap = new HashMapCaseInsensitive<Integer>();
		final int initVal = 0;
		int lineCounter = 0;
		try (final BufferedReader brIn = new BufferedReader(new FileReader(inFileKeywords))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				// Add to map
				final String key = line;
				counterMap.put(key, initVal);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Stopwatch.start(LauncherCheckKeywords.class.getName());
		final Set<String> keywords = counterMap.keySet();
		// Now go through the KG and check if the words appear
		try (final BufferedReader brIn = new BufferedReader(new FileReader(inFileKG))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				lineCounter++;
				final String line_low = line.toLowerCase();
				for (String keyword : keywords) {
					// Increment in map by amount
					final Integer val = line_low.contains(keyword) ? 1 : 0;// StringUtils.countMatches(line_low,
																			// keyword);
					if (val > 0) {
						final Integer oldVal = counterMap.getOrDefault(keyword, 0);
						counterMap.put(keyword, oldVal + val);
					}
				}
				if (lineCounter % 100_000 == 0) {
					System.out.println("Line #" + lineCounter);
					Stopwatch.endOutput(LauncherCheckKeywords.class.getName());
				}
			}
			final File outFile = new File(inFileKeywords.getAbsolutePath() + "_out");
			try (final BufferedWriter bwOut = new BufferedWriter(new FileWriter(outFile))) {
				for (Map.Entry<String, Integer> e : counterMap.entrySet()) {
					bwOut.write(e.getKey() + "\t" + e.getValue());
					bwOut.newLine();
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Finished!");
	}
}
