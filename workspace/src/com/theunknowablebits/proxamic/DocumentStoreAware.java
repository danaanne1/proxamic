package com.theunknowablebits.proxamic;

public interface DocumentStoreAware {
	
	public void setDocumentStore(DocumentStore docStore);
	public DocumentStore getDocumentStore();

}
