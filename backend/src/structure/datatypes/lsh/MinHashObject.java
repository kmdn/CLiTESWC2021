package structure.datatypes.lsh;

public class MinHashObject {

	public final String word;
	public final double confidence;

	public MinHashObject(String word, double confidence) {
		this.word = word;
		this.confidence = confidence;
	}

}
