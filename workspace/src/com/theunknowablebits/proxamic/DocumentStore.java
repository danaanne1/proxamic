package com.theunknowablebits.proxamic;

/**
 * A place to store and retrieve documents.
 */
public interface DocumentStore {

	public String getID(Document document);
	
	public Document newInstance();

	public Document newInstance(String key);

	public Document get(String key);

	public void put(Document document);

	public void delete(Document document);

}
