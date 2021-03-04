package structure.utils;

import structure.datatypes.Mention;

public class FunctionUtils {
	public static Number returnScore(final Number score, final Mention mention) {
		return score;
	}
	
	/**
	 * Transforms both numbers to double and adds them together.<br>
	 * <b>If currScore is NULL, it is treated as 0.</b>
	 * 
	 * @param leftScore
	 * @param rightScore
	 * @return
	 */
	public static Number add(Number leftScore, Number rightScore) {
		final double left = leftScore == null ? 0 : leftScore.doubleValue();
		final double right = rightScore == null ? 0 : rightScore.doubleValue();
		return left + right;
	}


}
