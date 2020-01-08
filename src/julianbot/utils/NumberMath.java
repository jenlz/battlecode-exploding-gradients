package julianbot.utils;

public class NumberMath {

	public static int indexOfLeast(int... values) {
		int minIndex = 0, minValue = Integer.MAX_VALUE;
		for(int i = 0; i < values.length; i++) {
			if(minValue > values[i]) {
				minValue = values[i];
				minIndex = i;
			}
		}
		
		return minIndex;
	}
	
}
