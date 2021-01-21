package com.theunknowablebits.proxamic.data;

import com.theunknowablebits.proxamic.DocumentView;
import com.theunknowablebits.proxamic.Getter;
import com.theunknowablebits.proxamic.Setter;

public interface InventoryItem extends DocumentView {
	
	@Getter("Name") public String name();
	@Setter("Name") public void name(String value);
	public InventoryItem withName(String value);

}
