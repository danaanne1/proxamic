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
			CharacterRecord otherRecord = docStore.get(CharacterRecord.class, "danas character");
			CharacterRecord characterRecord = docStore.newInstance("danas character").as(CharacterRecord.class);
			docStore.put(characterRecord.usingName("Dananator").characterClass("SoftwareEngineer").withLevel(25));
			CharacterRecord retrieved = docStore.get(CharacterRecord.class,"danas character");
			assertEquals("Dananator",retrieved.name());
			assertNotSame(characterRecord, retrieved);
			assertThrows(ConcurrentModificationException.class, () -> docStore.put(docStore.newInstance(CharacterRecord.class,"danas character")));
			assertThrows(ConcurrentModificationException.class, () -> docStore.put(otherRecord));
			docStore.delete(characterRecord);
			assertThrows(ConcurrentModificationException.class, () -> docStore.put(characterRecord));
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
