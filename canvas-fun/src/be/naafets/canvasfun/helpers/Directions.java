package be.naafets.canvasfun.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.Map;
import java.util.HashMap;

public enum Directions implements OptionList<String> {
	Up("up"),
	Down("down"),
	Left("left"),
	Right("right"),
	Wobble("wobble"),
	Random("random");

	private String choice;

	Directions(String aChoice) {
		this.choice = aChoice;
	}

	public String toUnderlyingValue() {
		return choice;
	}

	private static final Map<String, Directions> lookup = new HashMap<>();

	static {
		for(Directions aChoise : Directions.values()) {
			lookup.put(aChoise.toUnderlyingValue(), aChoise);
		}
	}

	public static Directions fromUnderlyingValue(String aChoise) {
		return lookup.get(aChoise);
	}
}
