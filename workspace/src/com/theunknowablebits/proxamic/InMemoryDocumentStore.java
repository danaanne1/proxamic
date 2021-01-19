package com.theunknowablebits.proxamic;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class InMemoryDocumentStore implements DocumentStore {

	Supplier<String> idGenerator = () -> TimeBasedUUIDGenerator.instance().nextUUID().toString();
	Supplier<Document> documentSupplier = () -> new BuffDocument();
	
	WeakHashMap<Document, String> documentIds = new WeakHashMap<>();
	HashMap<String,Document> documentsById = new HashMap<>();
	
	@Override
	public String getID(Document document) {
		return documentIds.get(document);
	}

	@Override
	public Document newInstance() {
		return newInstance(idGenerator.get());
	}

	@Override
	public Document newInstance(String key) {
		Document doc = documentSupplier.get();
		documentIds.put(doc, key);
		if (doc instanceof DocumentStoreAware)
			((DocumentStoreAware)doc).setDocumentStore(this);
		return doc;
	}

	@Override
	public Document get(String key) {
		Document doc = documentsById.get(key);
		if (doc instanceof DocumentStoreAware)
			((DocumentStoreAware)doc).setDocumentStore(this);
		return doc;
	}

	@Override
	public void put(Document document) {
		documentsById.put(documentIds.get(document), document);
	}

	@Override
	public void delete(Document document) {
		documentsById.remove(documentIds.get(document));
	}

	
	

}
