package com.theunknowablebits.proxamic;

public interface InventoryItem extends DocumentView {
	
	@Getter("Name") public String name();
	@Setter("Name") public void name(String value);
	public InventoryItem withName(String value);

}
