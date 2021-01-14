package com.theunknowablebits.proxamic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BuffDocument")
class BuffDocumentTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		fail("Not yet implemented");
	}

	@Nested
	@DisplayName("assumptions for")
	class Assumptions {

		public List<String> getGenericResultMethod() {
			return null;
		}
		
		@Test
		@DisplayName("accessing generic type parameters")
		void generics() throws NoSuchMethodException, SecurityException {
			ParameterizedType c = (ParameterizedType)getClass().getMethod("getGenericResultMethod").getGenericReturnType();
			assertEquals(String.class, c.getActualTypeArguments()[0]);
		}
	
	}
}
