package com.theunknowablebits.proxamic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
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

	public static interface Bob extends DocumentView { }

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
	@DisplayName("assumptions")
	class Assumptions {

		public List<String> getGenericResultMethod() {
			return null;
		}

		public List getGenericResultMethod2() {
			return null;
		}
		
		@Test
		@DisplayName("accessing generic type parameters")
		void generics() throws NoSuchMethodException, SecurityException {
			Type rt = getClass().getMethod("getGenericResultMethod").getGenericReturnType();
			assertTrue(rt instanceof ParameterizedType);
			assertFalse(rt instanceof Class);
			assertTrue(((ParameterizedType)rt).getRawType() instanceof Class);
			assertEquals(String.class, ((ParameterizedType)rt).getActualTypeArguments()[0]);
			Type t2 = getClass().getMethod("getGenericResultMethod2").getGenericReturnType();
			assertTrue(t2 instanceof Class);
		}

		
		@Test
		@DisplayName("dynamic proxies include inherited instances") 
		void proxies1() {
			Bob bob = (Bob) Proxy.newProxyInstance(getClass().getClassLoader(), new Class [] { Bob.class }, new InvocationHandler() {
				
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					// TODO Auto-generated method stub
					return null;
				}
			});
			assertTrue(bob instanceof DocumentView);
		}
		
	}
}
