package launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.beust.jcommander.internal.Lists;

import install.pr.PageRankRDF;
import structure.config.constants.FilePaths;
import structure.config.kg.EnumModelType;

public class LauncherPageRank {

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.DEFAULT;
		final String out = FilePaths.FILE_PAGERANK.getPath(KG);
		final boolean caseSensitive = false;
		
		final List<String> inFiles = Lists.newArrayList();
		final String inPath = "/vol2/wikidata/dumps/20190213/wikidata-20190213-truthy-BETA_all_URI-obj.nt";
		inFiles.add(inPath);

		final PageRankRDF pageRankRDF = new PageRankRDF(inFiles, 0.85, 1.0, 50, false, caseSensitive);
		pageRankRDF.compute();
		try (PrintWriter wrt = new PrintWriter(new BufferedWriter(new FileWriter(new File(out))))) {
			pageRankRDF.printPageRankScoresRDF(wrt);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
