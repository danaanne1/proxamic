package com.theunknowablebits.proxamic;

import static org.junit.jupiter.api.Assertions.*;

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
			CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
			docStore.put(characterRecord.usingName("Dananator").characterClass("SoftwareEngineer").withLevel(25));
			CharacterRecord retrieved = docStore.lock(CharacterRecord.class,"danas character");
			assertThrows(ConcurrentModificationException.class,()->docStore.put(characterRecord));
			assertThrows(ConcurrentModificationException.class,()->docStore.release(characterRecord));
			docStore.release(retrieved);
			docStore.release(retrieved);
			docStore.release(characterRecord);
			docStore.delete(characterRecord);
			assertThrows(ConcurrentModificationException.class, ()->docStore.put(characterRecord));
			
			retrieved = docStore.lock(CharacterRecord.class,"danas character");
			
		}
		
	}
	
}
