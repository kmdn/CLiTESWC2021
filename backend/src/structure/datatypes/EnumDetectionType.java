package structure.datatypes;

public enum EnumDetectionType {
	SINGLE_WORD("Only process single words as possible mentions"), //
	UNBOUND_DYNAMIC_WINDOW(
			"Process single words and any number of consecutive tokens. -> Note the O(n^2) execution complexity"), //
	BOUND_DYNAMIC_WINDOW(
			"Process single and multi-word tokens with a maximum number of consecutive words. (See settings file/class to change value)"), //
	UNBOUND_DYNAMIC_WINDOW_STRICT_MULTI_WORD("Only process multiple words as possible mentions (aka. same as "
			+ UNBOUND_DYNAMIC_WINDOW.name() + " except that single words are not taken into account)");
	final String desc;

	EnumDetectionType(final String desc) {
		this.desc = desc;
	}
}
