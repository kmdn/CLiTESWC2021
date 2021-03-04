package org.aksw.gerbil.io.nif.impl;

import org.apache.jena.rdf.model.Model;

public class AgnosTurtleNIFParser extends TurtleNIFParser {
	@Override
	protected void infereTypes(Model nifModel) {
		//super.infereTypes(nifModel);
		//don't do any type inference
		return;
	}
}
