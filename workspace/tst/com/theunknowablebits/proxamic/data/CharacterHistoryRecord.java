package com.theunknowablebits.proxamic.data;

import java.util.Date;

import com.theunknowablebits.proxamic.DocumentView;

public interface CharacterHistoryRecord extends DocumentView {


	public String getLogEntry();
	public void setLogEntry(String entry);
	public CharacterHistoryRecord withLogEntry(String logEntry);
	
	public Date getLogDate();
	public void setLogDate(Date d);
	public CharacterHistoryRecord withLogDate(Date d);
	
	
}
