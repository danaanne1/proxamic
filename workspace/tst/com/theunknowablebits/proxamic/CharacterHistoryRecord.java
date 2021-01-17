package com.theunknowablebits.proxamic;

import java.util.Date;

public interface CharacterHistoryRecord extends DocumentView {


	public String getLogEntry();
	public void setLogEntry(String entry);
	public CharacterHistoryRecord withLogEntry(String logEntry);
	
	public Date getLogDate();
	public void setLogDate(Date d);
	public CharacterHistoryRecord withLogDate(Date d);
	
	
}
