package com.theunknowablebits.proxamic.exampledata;

import java.util.List;

import com.theunknowablebits.proxamic.DocumentStoreAware;
import com.theunknowablebits.proxamic.DocumentView;
import com.theunknowablebits.proxamic.Getter;
import com.theunknowablebits.proxamic.Indirect;
import com.theunknowablebits.proxamic.Setter;

public interface PlayerRecord extends DocumentView, DocumentStoreAware {
	
	
	@Getter("PlayerName") String name();
	@Setter("PlayerName") PlayerRecord name(String value);
	
	@Indirect @Getter("characters") List<CharacterRecord> characters();

}
