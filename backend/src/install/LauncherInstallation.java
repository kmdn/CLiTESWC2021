package install;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import com.beust.jcommander.internal.Lists;

import install.surfaceform.query.general.SFQuery;
import structure.config.constants.FilePaths;
import structure.config.kg.DefaultQuery;
import structure.config.kg.EnumModelType;

public class LauncherInstallation {

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.DEFAULT;
		final String KGpath = FilePaths.FILE_KNOWLEDGE_GRAPH.getPath(KG);
		final String query = DefaultQuery.ALL_LABELS.query;

		// Import KG into RDF Store (Jena TDB Store)
		new LauncherSetupTDB().exec(KG, KGpath);

		// Compute PageRank and output to FilePaths.FILE_PAGERANK.getPath(KG)
		new PageRankComputer(KG).exec(new String[] { KGpath });

		// Extract mentions
		extractMentions(KG, query);

	}

	private static void extractMentions(EnumModelType KG, String query) {
		// Execute default SF query on TDB to extract mentions
		final Dataset dataset = TDBFactory.createDataset(FilePaths.DATASET.getPath(KG));
		final Model model = dataset.getDefaultModel();
		final String SFout = FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG);
		try (final BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(SFout)))) {
			final List<BufferedWriter> writers = Lists.newArrayList();
			writers.add(bwOut);
			try {
				new SFQuery(KG).execSelectQuery(query, model, writers);
			} catch (IOException e) {
				System.err.println("[ERROR] Could not query surface forms.");
				e.printStackTrace();
			}
			writers.clear();
		} catch (IOException e1) {
			System.err.println("[ERROR] Issue with the BufferedWriter.");
			e1.printStackTrace();
		}
		model.close();
		dataset.close();
	}

}
