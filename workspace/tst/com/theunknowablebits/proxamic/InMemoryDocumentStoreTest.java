package com.theunknowablebits.proxamic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
		
		void testNewInstance() {
			
		}
		
		
	}
	
}
