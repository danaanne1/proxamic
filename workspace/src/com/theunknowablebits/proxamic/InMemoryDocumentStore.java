package com.theunknowablebits.proxamic;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class InMemoryDocumentStore extends AbstractDocumentStore implements DocumentStore {

	private static class Record {
		String documentId;
		ByteBuffer document;
		long versionNumber;
		public Record(String documentId, ByteBuffer document, long versionNumber) {
			super();
			this.documentId = documentId;
			this.document = document;
			this.versionNumber = versionNumber;
		}
	}

	Map<Document,Record> recordsByDocument = Collections.synchronizedMap(new WeakHashMap<>());
	ConcurrentHashMap<String,Record> tipRecordsById = new ConcurrentHashMap<String,Record>();

	
	public InMemoryDocumentStore(Supplier<Document> docFromNothing, Function<ByteBuffer, Document> docFromBytes, Supplier<String> idSupplier) {
		super(docFromNothing, docFromBytes, idSupplier);
	}

	public InMemoryDocumentStore(Supplier<Document> docFromNothing, Function<ByteBuffer, Document> docFromBytes) {
		super(docFromNothing, docFromBytes);
	}

	public InMemoryDocumentStore() {
		super();
	}
	
	@Override
	public String getID(Document document) {
		return recordsByDocument.get(document).documentId;
	}


	@Override
	public Document newInstance(String key) {
		Document doc = docFromNothing.get();
		recordsByDocument.put(doc, new Record(key,doc.toByteBuffer(),0));
		if (doc instanceof DocumentStoreAware)
			((DocumentStoreAware)doc).setDocumentStore(this);
		return doc;
	}

	@Override
	public Document get(String key) {
		Record dataRec = tipRecordsById.get(key);
		Document doc = docFromBytes.apply(dataRec.document);
		recordsByDocument.put(doc,dataRec);
		if (doc instanceof DocumentStoreAware)
			((DocumentStoreAware)doc).setDocumentStore(this);
		return doc;
	}

	@Override
	public void put(Document document) {
		Record toInsert = recordsByDocument.get(document);
		synchronized(toInsert.documentId) {
			Record toReplace = tipRecordsById.get(toInsert.documentId);
			if ((toReplace!=null)&&(toInsert.versionNumber!=toReplace.versionNumber)) 
				throw new ConcurrentModificationException("Version mismatch");
			if ( ( toReplace == null ) && ( toInsert.versionNumber != 0) )
				throw new ConcurrentModificationException("Cannot insert a previously deleted document. Obtain a newInstance first.");
			recordsByDocument.put(document, toInsert = new Record(toInsert.documentId,document.toByteBuffer(),toInsert.versionNumber++));
			tipRecordsById.put(toInsert.documentId, toInsert);
		}
	}

	@Override
	public void delete(Document document) {
		Record toDelete = recordsByDocument.get(document);
		synchronized(toDelete.documentId) {
			Record toReplace = tipRecordsById.get(toDelete.documentId);
			if ( ( toReplace != null ) && ( toDelete.versionNumber != toReplace.versionNumber ) )
				throw new ConcurrentModificationException("Version mismatch");
			tipRecordsById.remove(toDelete.documentId);
		}
	}
	
	@Override
	public Document lock(String key) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void release(Document document) {
		// TODO Auto-generated method stub
		
	}

}
