package com.theunknowablebits.proxamic;

/**
 * A place to store and retrieve documents.
 */
public interface DocumentStore {

	public Document create();

	public Document retrieve(String key);

	public void update(Document document);

	public void delete(Document document);

	public default <T extends DocumentView> T create(Class <T> documentClass) {
		return create().as(documentClass);
	}

	public default <T extends DocumentView> T retrieve(Class<T> documentClass, String key) {
		return retrieve(key).as(documentClass);
	}

	public default <T extends DocumentView> void update(T documentView) {
		update(documentView.document());
	}
	
	public default <T extends DocumentView> void delete(T documentView) {
		delete(documentView.document());
	}
	
}
