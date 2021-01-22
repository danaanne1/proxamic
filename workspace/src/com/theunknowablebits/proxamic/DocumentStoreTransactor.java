package com.theunknowablebits.proxamic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

final class DocumentStoreTransactor implements DocumentStore, Consumer<Consumer<DocumentStore>> {
	DocumentStore delegate;

	HashMap<String, Document> documentsById = new HashMap<>(); 
	LinkedHashMap<String,Document> toPut = new LinkedHashMap<>();
	LinkedHashMap<String,Document> toDelete = new LinkedHashMap<>();
	
	public DocumentStoreTransactor(DocumentStore delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public String getID(Document document) { return delegate.getID(document); }

	@Override
	public Document newInstance() { return AbstractDocumentStore.withDocStore(delegate.newInstance(),this); }

	@Override
	public Document newInstance(String key) { return AbstractDocumentStore.withDocStore(delegate.newInstance(key),this); }

	@Override
	public Document get(String key) {
		return lock(key);
	}

	@Override
	public synchronized void put(Document document) {
		if (toDelete.containsKey(getID(document)))
			throw new IllegalStateException("document was removed");
		toPut.put(getID(document), document);
	}

	@Override
	public synchronized void delete(Document document) {
		toDelete.put(getID(document), document);
	}

	@Override
	public void transact(Consumer<DocumentStore> transaction) {
		transaction.accept(this);
	}

	@Override
	public void execute(Consumer<DocumentStore> execution) {
		execution.accept(this);
	}
	@Override
	public synchronized Document lock(String key) {
		if (toDelete.containsKey(key))
			throw new IllegalStateException("document was removed");
		if (toPut.containsKey(key)) 
			return toPut.get(key);
		if (!documentsById.containsKey(key))
			documentsById.put(key, AbstractDocumentStore.withDocStore(delegate.lock(key),this));
		return documentsById.get(key);
	}
	@Override
	public void release(Document document) {
		// noop
	}
	final void commit() {
		documentsById.keySet().removeAll(toDelete.keySet());
		documentsById.keySet().removeAll(toPut.keySet());
		toPut.keySet().removeAll(toDelete.keySet());

		toPut.forEach((key,document)->delegate.put(document));
		toDelete.forEach((key,document)->delegate.delete(document));
		documentsById.forEach((key,document)->delegate.release(document));
	}
	final void rollback() {
		documentsById.forEach((key,document)->delegate.release(document));
	}

	@Override
	public void accept(Consumer<DocumentStore> transactor) {
		boolean success = false;
		try {
			transactor.accept(this);
			success = true;
		} finally {
			if (success)
				commit();
			else
				rollback();
		}
	}
}