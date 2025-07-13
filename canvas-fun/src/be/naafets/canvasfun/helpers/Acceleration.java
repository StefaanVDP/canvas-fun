package be.naafets.canvasfun.helpers;

import com.google.appinventor.components.common.OptionList;
//import com.google.appinventor.components.common.Default;
import java.util.Map;
import java.util.HashMap;

public enum Acceleration implements OptionList<Integer> {
	None(0),
	Slow(1),
	Medium(3),
	Fast(5),
	SuperFast(8);

	private int choice;

	Acceleration(int aChoice) {
		this.choice = aChoice;
	}

	public Integer toUnderlyingValue() {
		return choice;
	}

	private static final Map<Integer, Acceleration> lookup = new HashMap<>();

	static {
		for(Acceleration aChoise : Acceleration.values()) {
			lookup.put(aChoise.toUnderlyingValue(), aChoise);
		}
	}

	public static Acceleration fromUnderlyingValue(Integer aChoise) {
		return lookup.get(aChoise);
	}
}
