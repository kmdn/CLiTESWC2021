package structure.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

public class ListUtils {
	/**
	 * Saves collection's string-format objects in the specified file
	 * 
	 * @param c
	 *            Collection to be saved
	 * @param f
	 *            output file
	 */
	public static void saveAs(Collection c, File f) {
		if (c == null) {
			System.err.println("Null collection passed...");
			return;
		}
		try (BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(f.getPath()))) {
			for (Object o : c) {
				bwOut.write(o.toString());
				bwOut.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
