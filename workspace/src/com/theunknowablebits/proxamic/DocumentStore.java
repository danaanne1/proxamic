package com.theunknowablebits.proxamic;

import java.util.function.Consumer;

/**
 * A place to store and retrieve documents.
 */
public interface DocumentStore {

	public String getID(Document document);
	
	public Document newInstance();

	public Document newInstance(String key);

	public Document get(String key);
	
	public Document lock(String key);
	
	public void release(Document document);

	public void put(Document document);

	public void delete(Document document);

	/** 
	 * Runs an execution as a transaction. The transaction will either succeed or throw an exception.
	 * @param transaction
	 */
	public default void transact(Consumer<DocumentStore> transactor) { new DocumentStoreTransactor(this).accept(transactor); }

	/** 
	 * Runs a scoped execution similar to a transaction but without transactional semantics. This is useful for queries that require 
	 * help with cannonicalization.
	 * @param transaction
	 */
	public default void execute(Consumer<DocumentStore> executor) { new CachingDocumentStore(this).accept(executor); }
	
}
