package dataset;

public enum EnumTSVCoNLL2011 {
	// - Each document starts with a line: -DOCSTART- (<docid>)
	// - Each following line represents a single token, sentences are separated by
	// an empty line
	//
	// Lines with tabs are tokens the are part of a mention:
	// - column 1 is the token
	// - column 2 is either B (beginning of a mention) or I (continuation of a
	// mention)
	// - column 3 is the full mention used to find entity candidates
	// - column 4 is the corresponding YAGO2 entity (in YAGO encoding, i.e. unicode
	// characters are backslash encoded and spaces are replaced by underscores, see
	// also the tools on the YAGO2 website), OR --NME--, denoting that there is no
	// matching entity in YAGO2 for this particular mention, or that we are missing
	// the connection between the mention string and the YAGO2 entity.
	// - column 5 is the corresponding Wikipedia URL of the entity (added for
	// convenience when evaluating against a Wikipedia based method)
	// - column 6 is the corresponding Wikipedia ID of the entity (added for
	// convenience when evaluating against a Wikipedia based method - the ID refers
	// to the dump used for annotation, 2010-08-17)
	// - column 7 is the corresponding Freebase mid, if there is one (thanks to
	// Massimiliano Ciaramita from Google Zürich for creating the mapping and making
	// it available to us)
	TOKEN_OR_DOCSTART, //
	B_OR_I, //
	MENTION, //
	YAGO2_ENTITY, //
	WIKIPEDIA_URL, //
	WIKIPEDIA_ID, //
	FREEBASE_MID,//
	;
}
