package structure.config.constants;

public enum EnumEmbeddingMode {
	LOCAL()//
	, REMOTE()//
	, DEFAULT(LOCAL)//
	;

	public final EnumEmbeddingMode val;

	EnumEmbeddingMode() {
		this.val = null;
	}

	EnumEmbeddingMode(EnumEmbeddingMode val) {
		this.val = val;
	}

}
