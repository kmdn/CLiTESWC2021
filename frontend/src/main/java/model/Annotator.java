package model;

public class Annotator {

	private String name;
	private int id;
	
	public Annotator() {}
	
	public Annotator(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

}
