package com.theunknowablebits.proxamic;

import java.util.List;

public interface CharacterHistory extends DocumentView {

	@Getter("Records") public List<CharacterHistoryRecord> records();
	
}
