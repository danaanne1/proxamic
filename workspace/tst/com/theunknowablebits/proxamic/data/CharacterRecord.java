package com.theunknowablebits.proxamic.data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.theunknowablebits.proxamic.DocumentView;
import com.theunknowablebits.proxamic.Getter;
import com.theunknowablebits.proxamic.Setter;

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
	
	@Getter("Class") public String characterClass();
	@Setter("Class") public CharacterRecord characterClass(String className);
	
	public void unrecognizedMethod();
	
	default String sheetHeader() {
		return name() + ", Level " + getLevel() + " " + characterClass();
	}
	
	@Getter("Abilities") public AbilityScore [] abilityScores();
	@Setter("Abilities") public AbilityScore [] abilityScores(AbilityScore [] values);
	
	@Getter("Abilities") List<AbilityScore> abilityScoreList();
	@SuppressWarnings("rawtypes")
	@Getter("Abilities") List uncheckedAbilityScoreList();
	
	@Getter("AbilityMap") Map<String,AbilityScore> abilityScoreMap();
		
	public CharacterHistory getCharacterHistory();
	
	@Getter("InventoryItems") public Map<String,InventoryItem> inventoryItems();
}
