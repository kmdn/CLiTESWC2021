package structure.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class FileUtils {
	public static long getFileSizeKB(final File file) {
		return getFileSize(file, 1);
	}

	public static long getFileSizeMB(final File file) {
		return getFileSize(file, 2);
	}

	public static long getFileSizeGB(final File file) {
		return getFileSize(file, 3);
	}

	public static long getFileSize(final File file, final int unitCounter) {
		if (file.exists()) {
			return getSizeAsUnit(file.length(), unitCounter);
		} else {
			return -1l;
		}
	}

	public static long getSizeAsUnit(final long size, final int unitCounter) {
		if (unitCounter > 0) {
			return getSizeAsUnit(size, unitCounter - 1);
		}
		return size;
	}

	public static void transferFileContentsFromTo(final File inFile, final File outFile, final boolean append) throws IOException
			{

		if (append) {
			// Need to add a line if we append it all
			final BufferedWriter bw = new BufferedWriter(new FileWriter(outFile, append));
			bw.newLine();
			bw.close();
		}

		try (final FileInputStream is = new FileInputStream(inFile);
				final FileOutputStream fos = new FileOutputStream(outFile, append);
				final FileChannel channelIn = is.getChannel();
				final FileChannel channelOut = fos.getChannel()) {
			final ByteBuffer buf = ByteBuffer.allocateDirect(64 * 1024);
			long len = 0;
			while ((len = channelIn.read(buf)) != -1) {
				buf.flip();
				channelOut.write(buf);
				buf.clear();
			}
		}
	}

	/**
	 * IMPORTANT: Assumes UTF8 file encoding
	 * 
	 * @param file input file
	 * @return contents of passed file
	 * @throws IOException
	 */
	public static String getContents(final File file) throws IOException {
		return Files.asCharSource(file, Charsets.UTF_8).read();
	}

	/**
	 * 
	 * @param path input file path
	 * @return contents of passed file
	 * @throws IOException
	 */
	public static String getContents(final String path) throws IOException {
		return getContents(new File(path));
	}
}
