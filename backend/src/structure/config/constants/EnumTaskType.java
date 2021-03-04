package structure.config.constants;

public enum EnumTaskType {
	
	MD("Mention detection"),
	CG("Candidate generation"),
	ED("Entity disambiguation"),
	CG_ED("Candidate generation and entity disambiguation"),
	FULL("Mention detection, candidate generation and entity disambiguation");

	private String label;
	
	EnumTaskType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return this.label;
	}
	
}
