package structure.interfaces.pipeline;

import linking.pipeline.interfaces.Combiner;
import linking.pipeline.interfaces.Filter;
import linking.pipeline.interfaces.Splitter;
import linking.pipeline.interfaces.Transformer;
import linking.pipeline.interfaces.Translator;
import structure.interfaces.CandidateGeneratorDisambiguator;
import structure.linker.Linker;

public enum EnumTypeComponent {
	UNSPECIFIED(null)//
	, MD(MentionDetector.class)//
	, CG(CandidateGenerator.class)//
	, ED(Disambiguator.class)//
	, CG_ED(CandidateGeneratorDisambiguator.class)//
	, MD_CG_ED(Linker.class)//
	, COMBINER(Combiner.class)//
	, SPLITTER(Splitter.class)//
	, TRANSLATOR(Translator.class)//
	, TRANSFORMER(Transformer.class)//
	, FILTER(Filter.class)//
	;

	final Class type;

	EnumTypeComponent(final Class clazz) {
		this.type = clazz;
	}

	public boolean isInstance(Object o)
	{
		if (this.type == null || o == null)
		{
			return false;
		}
		return this.type.isInstance(o);
	}
}
