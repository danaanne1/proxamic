package com.theunknowablebits.proxamic;

import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractDocumentStore implements DocumentStore {
	
	protected final Supplier<Document> docFromNothing;
	protected final Function<ByteBuffer,Document> docFromBytes;
	protected final Supplier<String> idSupplier;
	
	
	public AbstractDocumentStore(Supplier<Document> docFromNothing, Function<ByteBuffer, Document> docFromBytes,
			Supplier<String> idSupplier) {
		super();
		this.docFromNothing = docFromNothing;
		this.docFromBytes = docFromBytes;
		this.idSupplier = idSupplier;
	}

	public AbstractDocumentStore(Supplier<Document> docFromNothing, Function<ByteBuffer, Document> docFromBytes) {
		this(docFromNothing,docFromBytes,()->TimeBasedUUIDGenerator.instance().nextUUID().toString());
	}

	public AbstractDocumentStore() {
		this(BuffDocument::new,(bytes)->new BuffDocument(bytes),()->TimeBasedUUIDGenerator.instance().nextUUID().toString());
	}

	@Override
	public Document newInstance() {
		return newInstance(idSupplier.get());
	}

	public final Document withDocStore(Document document) {
		return withDocStore(document,this);
	}
	
	public static final Document withDocStore(Document document, DocumentStore store) {
		if (document instanceof DocumentStoreAware) {
			((DocumentStoreAware)document).setDocumentStore(store);
		}
		return document;
	}

}
