package be.naafets.canvasfun.helpers;

import com.google.appinventor.components.common.Default;
import com.google.appinventor.components.common.OptionList;
import java.util.Map;
import java.util.HashMap;


/*
 * ARC, TEXTS and DRAW are deprecated and thus cannot be chosen in the list of options
 * But they are used hardcoded in their corresponding functions and the draw methods
 */
public enum Shapes implements OptionList<String> {
	@Default
	Circle("circle"),
	Square("square"),
	Text("text"),
	Pacman("pacman"),
	@Deprecated
	Image("image"),
	@Deprecated
	Arc("arc"),	
	@Deprecated
	SubArc("subarc"),
	@Deprecated
	RoundRect2("RoundRect2"),
	@Deprecated
	Texts("texts"),
	@Deprecated
	Draw("draw");

	private String choice;

	Shapes(String aChoice) {
		this.choice = aChoice;
	}

	public String toUnderlyingValue() {
		return choice;
	}

	private static final Map<String, Shapes> lookup = new HashMap<>();

	static {
		for(Shapes aChoise : Shapes.values()) {
			lookup.put(aChoise.toUnderlyingValue(), aChoise);
		}
	}

	public static Shapes fromUnderlyingValue(String aChoise) {
		return lookup.get(aChoise);
	}
}
