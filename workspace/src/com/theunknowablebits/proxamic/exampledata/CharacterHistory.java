package com.theunknowablebits.proxamic.exampledata;

import java.util.List;

import com.theunknowablebits.proxamic.DocumentView;
import com.theunknowablebits.proxamic.Getter;

public interface CharacterHistory extends DocumentView {

	@Getter("Records") public List<CharacterHistoryRecord> records();
	
}
