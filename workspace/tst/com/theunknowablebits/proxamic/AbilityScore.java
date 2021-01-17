package com.theunknowablebits.proxamic;

public interface AbilityScore extends DocumentView {
	
	@Getter("Name") public String name();
	@Setter("Name") public String name(String name);
	@Setter("Name") public AbilityScore withName(String name);

	
	@Getter("Value") public Integer value();
	@Setter("Value") public Integer value(Integer value);
	@Setter("Value") public AbilityScore withValue(Integer value);

}
