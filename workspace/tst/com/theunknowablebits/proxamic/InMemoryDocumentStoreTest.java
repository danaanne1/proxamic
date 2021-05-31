package com.theunknowablebits.proxamic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ConcurrentModificationException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.theunknowablebits.proxamic.data.CharacterRecord;

@DisplayName("InMemoryDocumentStore")
class InMemoryDocumentStoreTest {

	
	InMemoryDocumentStore docStore; 
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	@Test
	@DisplayName("instantiates")
	void setUp() throws Exception {
		docStore = new InMemoryDocumentStore();
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Nested
	@DisplayName("basic operations") 
	class BasicOperations {
		
		@Test
		@DisplayName("creation")
		void testNewInstance() {
			CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
			assertEquals("danas character", docStore.getID(characterRecord));
			assertThrows(IllegalArgumentException.class, () -> docStore.getID(new BuffDocument()));
		}
		
		@Test
		@DisplayName("crud") 
		void testCrud() {
			// create an instance via get (over missing)
			CharacterRecord otherRecord = docStore.get(CharacterRecord.class, "danas character");

			// create an instance via new instance
			CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);

			// put the new instance, creating an actual record.
			docStore.put(characterRecord.usingName("Dananator").characterClass("SoftwareEngineer").withLevel(25));

			// obtain the inserted object
			CharacterRecord retrieved = docStore.get(CharacterRecord.class,"danas character");

			// the retrieved object should actually have gone over the byte bridge
			assertEquals("Dananator", retrieved.name() );

			// get should always return a copy
			assertNotSame(characterRecord, retrieved);
			
			// should not be able to insert a new record over an existing one
			assertThrows( ConcurrentModificationException.class, () -> docStore.put(docStore.newInstance(CharacterRecord.class,"danas character")) );
			
			// out of order delete
			assertThrows( ConcurrentModificationException.class, () -> docStore.delete(otherRecord) ); 

			// in order delete
			docStore.delete(characterRecord);

			// character record is still assigned a version (even after delete) and should not be insertable as new:
			assertThrows( ConcurrentModificationException.class, () -> docStore.put(characterRecord) );
			
			// this record is still new, and should be insertable now
			docStore.put(otherRecord);  
		}
		
		@Test
		@DisplayName("lock and release")
		void testLockRelease() {
			// insert a document
			final CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
			docStore.put(characterRecord.usingName("Dananator").characterClass("SoftwareEngineer").withLevel(25));

			// obtain a locked variant
			CharacterRecord retrieved = docStore.lock(CharacterRecord.class,"danas character");

			// mutation other than through the lock item should fail
			assertThrows(ConcurrentModificationException.class,()->docStore.put(characterRecord));

			// dont allow release except through the lock
			assertThrows(ConcurrentModificationException.class,()->docStore.release(characterRecord));

			// cant lock over a lock
			assertThrows(ConcurrentModificationException.class, ()->docStore.lock(CharacterRecord.class, "danas character"));

			// proper release should work
			docStore.release(retrieved);
			retrieved = docStore.lock(CharacterRecord.class,"danas character");
			
			// put should release
			docStore.put(retrieved);

			CharacterRecord characterRecord2 = docStore.get(CharacterRecord.class , "danas character");
			
			// releasing or operating on an unlocked should work after release
			docStore.release(retrieved);
			docStore.release(characterRecord2);
			docStore.delete(characterRecord2);
			assertThrows(ConcurrentModificationException.class, ()->docStore.put(characterRecord2));
			
			// can lock after release, and delete
			retrieved = docStore.lock(CharacterRecord.class,"danas character");
			
			
		}
		
	}

	public static class DelegateDocumentStore implements DocumentStore, Serializable {
		private static final long serialVersionUID = 1L;
		private static final DocumentStore delegate = new InMemoryDocumentStore();
		
		public DelegateDocumentStore() {
		}

		public String getID(Document document) {
			return delegate.getID(document);
		}

		public Document withRemappedDocStore(Document d) {
			if (d instanceof DocumentStoreAware)
				((DocumentStoreAware)d).setDocumentStore(this);
			return d;
		}
		
		public Document newInstance() {
			return withRemappedDocStore(delegate.newInstance());
		}

		public Document newInstance(String key) {
			return withRemappedDocStore(delegate.newInstance(key));
		}

		public Document get(String key) {
			return withRemappedDocStore(delegate.get(key));
		}

		public Document lock(String key) {
			return withRemappedDocStore(delegate.lock(key));
		}

		public void release(Document document) {
			delegate.release(document);
		}

		public void put(Document document) {
			delegate.put(document);
		}

		public void delete(Document document) {
			delegate.delete(document);
		}

	}
	
	
	@Nested
	@DisplayName("serialization")
	class Serialization {
		private DelegateDocumentStore delegateDocStore;
		
		@BeforeEach
		public void setup() {
			delegateDocStore = new DelegateDocumentStore();
		}
		
		@Test
		@DisplayName("document view")
		public void document() throws ClassNotFoundException, IOException {
			CharacterRecord record = delegateDocStore.newInstance(CharacterRecord.class);
			record.setName("bob");
			delegateDocStore.put(record);

			CharacterRecord newRecord = serialized(record);
			assertArrayEquals(record.document().toBytes(), newRecord.document().toBytes());
			assertEquals(delegateDocStore.getID(record),delegateDocStore.getID(newRecord));
		}
		
		@SuppressWarnings("unchecked")
		private <T> T serialized(T record) throws IOException, ClassNotFoundException 
		{
			try (
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					ObjectOutputStream oout = new ObjectOutputStream(bout) )
			{
				oout.writeObject(record);
				oout.close();
				bout.close();
				try (
						ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
						ObjectInputStream oin = new ObjectInputStream(bin))
				{
					return (T)oin.readObject();
				}
			}
		}

		
	}
	
	
	@Nested
	@DisplayName("transactions") 
	class Transactions {
		
		@Test
		@DisplayName("cannonicalize")
		public void cannonicalization() {
			
			// insert a document
			final CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
			docStore.put(characterRecord.usingName("Dananator").characterClass("SoftwareEngineer").withLevel(25));

			// verify that the item returned is the item returned
			docStore.transact((docStore)->{
				assertSame(docStore.get("danas character"), docStore.get("danas character"));
			});

		}

		@Test
		@DisplayName("operation ordering")
		public void operationsOrdering() {
			final CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
			docStore.put(docStore.newInstance("danas character").as(CharacterRecord.class).withLevel(0));

			docStore.transact((docStore)->{
				CharacterRecord beforeUpdate = docStore.get(CharacterRecord.class, "danas character");

				// not a member:
				assertThrows(IllegalArgumentException.class, () -> docStore.put(characterRecord));
				
				docStore.put(beforeUpdate.usingName("Dananator").characterClass("SDE 3").withLevel(25));
				docStore.put(docStore.get(CharacterRecord.class, "danas character").withLevel(27));
				
				// get returns the put item
				assertEquals(27, docStore.get(CharacterRecord.class, "danas character").getLevel());

				// views may be different but the underlying document is always the same inside a transaction:
				assertNotSame(beforeUpdate, docStore.get(CharacterRecord.class, "danas character"));
				assertSame(beforeUpdate.document(), docStore.get(CharacterRecord.class, "danas character").document());
				
				// get after delete succeeds with a new instance
				docStore.delete(beforeUpdate);
				assertNull(docStore.get(CharacterRecord.class, "danas character").name());
				
			});
			
			
		}
		
		@Test
		@DisplayName("insert after delete")
		public void insertAfterDelete() {
			// insert a document
			final CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
			docStore.put(characterRecord.usingName("Dananator").characterClass("SoftwareEngineer 3").withLevel(25));

			docStore.transact((docStore)->{
				assertThrows(IllegalArgumentException.class, ()->docStore.delete(characterRecord));
				
				CharacterRecord toDelete = docStore.get(CharacterRecord.class, "danas character");
				docStore.delete(toDelete);

				docStore.delete(toDelete); // outside of a transaction this should fail

				
				assertThrows(IllegalArgumentException.class, ()->docStore.put(characterRecord));
				
				// insert a document
				CharacterRecord newRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
				
				docStore.put(newRecord.usingName("Dananator").characterClass("Software Engineer 4").withLevel(25));
				
			});
			
			assertEquals("Software Engineer 4", docStore.get(CharacterRecord.class, "danas character").characterClass());
		}
		
		@Test
		@DisplayName("rollback")
		public void rollbacks() {
			final CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
			docStore.put(characterRecord.usingName("Dananator").characterClass("SoftwareEngineer 3").withLevel(25));

			CharacterRecord conflictRecord = docStore.newInstance("other character").as(CharacterRecord.class);
			docStore.put(conflictRecord.usingName("Dananator2").characterClass("SoftwareEngineer 1").withLevel(12));
			conflictRecord = docStore.lock(CharacterRecord.class, "other character");
			
			assertThrows(ConcurrentModificationException.class, () -> {
				docStore.transact((tDocStore)->{
					tDocStore.put(tDocStore.get(CharacterRecord.class, "danas character").usingName("The Original Dananator"));
					tDocStore.get("other character"); // cause a rollback
				});
			});
			
			docStore.put(conflictRecord.usingName("The Original"));
			assertEquals("The Original", docStore.get(CharacterRecord.class, "other character").name());
			assertEquals("Dananator", docStore.get(CharacterRecord.class, "danas character").name());
			
		}
	
	}
	
}
