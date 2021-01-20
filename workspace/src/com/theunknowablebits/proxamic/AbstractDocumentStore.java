package com.theunknowablebits.proxamic;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
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

	@Override
	public void transact(final Consumer<DocumentStore> transaction) {
		DocumentStoreTransactor transactor = new DocumentStoreTransactor(this);
		boolean success = false;
		try {
			transaction.accept(transactor);
			success = true;
		} finally {
			if (success)
				transactor.commit();
			else
				transactor.rollback();
		}
	}

	@Override
	public void execute(Consumer<DocumentStore> execution) {
		//new DocumentStoreExecutor(this).execute(transaction);
	}


	private static final class DocumentStoreTransactor implements DocumentStore {
		DocumentStore delegate;

		HashMap<String, Document> documentsById = new HashMap<>(); 
		LinkedHashMap<String,Document> toPut = new LinkedHashMap<>();
		LinkedHashMap<String,Document> toDelete = new LinkedHashMap<>();
		
		public DocumentStoreTransactor(DocumentStore delegate) {
			this.delegate = delegate;
		}
		
		private Document mappingDocStore(Document document) {
			if (document instanceof DocumentStoreAware) {
				((DocumentStoreAware)document).setDocumentStore(this);
			}
			return document;
		}
		
		@Override
		public String getID(Document document) { return delegate.getID(document); }

		@Override
		public Document newInstance() { return mappingDocStore(delegate.newInstance()); }

		@Override
		public Document newInstance(String key) { return mappingDocStore(delegate.newInstance(key)); }

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
			//  simple passthrough at this point
			transaction.accept(this);
		}

		@Override
		public void execute(Consumer<DocumentStore> execution) {
			// executions inside a transaction are still transactions
			execution.accept(this);
		}
		@Override
		public synchronized Document lock(String key) {
			if (toDelete.containsKey(key))
				throw new IllegalStateException("document was removed");
			if (toPut.containsKey(key)) 
				return toPut.get(key);
			if (!documentsById.containsKey(key))
				documentsById.put(key,mappingDocStore(delegate.lock(key)));
			return documentsById.get(key);
		}
		@Override
		public void release(Document document) {
			// noop
		}
		private final void commit() {
			documentsById.keySet().removeAll(toDelete.keySet());
			documentsById.keySet().removeAll(toPut.keySet());
			toPut.keySet().removeAll(toDelete.keySet());

			toPut.forEach((key,document)->delegate.put(document));
			toDelete.forEach((key,document)->delegate.delete(document));
			documentsById.forEach((key,document)->delegate.release(document));
		}
		private final void rollback() {
			documentsById.forEach((key,document)->delegate.release(document));
		}
	}

}
