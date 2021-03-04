package structure.linker;

import java.util.ArrayList;
import java.util.List;

import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;

public interface LinkerNIF extends Linker {
	public default String createNIF(final String input) {
		// final TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
		final Document document = new DocumentImpl(input, "http://kit.edu/agnos/documentInput");
		final List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		final NIFWriter writer = new TurtleNIFWriter();
		final String nifString = writer.writeNIF(documents);
		// final String retNifDocumentStr = creator.getDocumentAsNIFString(document);
		return nifString;
	}
}