package structure.interfaces;

import java.util.function.BiFunction;

public interface Weighable<T> {
	public Number getWeight();

	/**
	 * Meant for more complex score modulation, e.g. via squaring, applying the sqrt
	 * or such.</br>
	 * 
	 * <b>Number1</b>: Score for this passed parameter / possible assignment</br>
	 * <b>T</b>: possibleAssignment (or whatever is used for that particular scorer)</br>
	 * <b>Number2</b>: Return value is a number...
	 * 
	 * @return a function that modulates the score dynamically, e.g. via squaring a
	 *         score
	 */
	public BiFunction<Number, T, Number> getScoreModulationFunction();

}
