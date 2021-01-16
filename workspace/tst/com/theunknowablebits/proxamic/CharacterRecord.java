package com.theunknowablebits.proxamic;

import java.math.BigDecimal;

/** A test class representing a character sheet from classic D&D */
public interface CharacterRecord extends DocumentView {

	@Getter("Name") public String name();
	@Setter("Name") public String name(String value); // annotated with fluent non builder return
	@Setter("Name") public CharacterRecord usingName(String name); // annotated with fluent builder return
	
	
	public String getName();
	public String setName(String name); // returning old mode
	
	public Integer getLevel();
	public void setLevel(Integer level); // not caring about return mode
	public CharacterRecord withLevel(Integer level); // for fluent test

	// Serialization of unknown but serializable types
	public BigDecimal getAge();
	public CharacterRecord setAge(BigDecimal age); // fluent mode
	
	
}
