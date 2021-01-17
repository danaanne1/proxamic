package com.theunknowablebits.proxamic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.theunknowablebits.buff.serialization.Record;

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
	
	@Nested
	@DisplayName("document")
	class BasicDocument {
		BuffDocument document;
		
		@Test
		@DisplayName("instantiate")
		@BeforeEach
		void init() {
			document = new BuffDocument();
		}
		
		@Test
		@DisplayName("copy instantiate")
		void initFromBytes() {
			document = new BuffDocument(document.asByteBuffer());
		}

		@Nested
		@DisplayName("document view")
		class DocumentViews {
			CharacterRecord record;
			
			@Test
			@DisplayName("instantiation") 
			@BeforeEach
			void init () {
				record = document.newInstance(CharacterRecord.class);
			}
			
			@Test
			@DisplayName("primitives and fluent behavior")
			void getAndSetPrimitives() {
				record.setName("Character Name");
				assertEquals("Character Name",record.getName());
				assertEquals("Character Name",record.name());
				assertEquals("Character Name",record.name("Character Name2")); // annotated fluent non builder pattern
				assertEquals(record,record.usingName("Character Name3")); // annotated fluent builder pattern
				assertEquals("Character Name3",record.getName());
				assertEquals("Character Name3",record.name());

				record.setAge(BigDecimal.ONE);
				assertEquals(BigDecimal.ONE, record.getAge());

				record.setLevel(1);
				assertEquals(1, record.getLevel());
				
				assertEquals(record,record.withLevel(2)); // fluent builder pattern
				assertEquals(record,record.setAge(BigDecimal.TEN)); // fluent builder pattern
				
				assertEquals(BigDecimal.TEN, record.getAge());
				assertEquals(2, record.getLevel());
					
			}

			@Test
			@DisplayName("ambiguous fluent return")
			void ambiguousFluentReturn() {
			}
			
			@Test
			@DisplayName("unrecognized methods")
			void unrecognizedMethod() {
				assertThrows(RuntimeException.class,() -> {
					record.unrecognizedMethod();
				}, "No path to invoke for unrecognizedMethod");
			}

			@Test
			@DisplayName("default methods")
			void defaultMethod() {
				assertEquals
				(
						"Dana, Level 20 Software Engineer",
						record.usingName("Dana").withLevel(20).characterClass("Software Engineer").sheetHeader()
				);
			}

			@Test
			@DisplayName("equals and hashcode")
			void objectIdentity() {
				CharacterRecord record2 = record.document().as(CharacterRecord.class);
				assertTrue(record2.equals(record));
				assertFalse(record2.equals(null));
				assertFalse(record2.equals(new Object()));
				assertFalse(record2.equals(new Object[0]));
				assertEquals(record2.hashCode(), record.hashCode());
				CharacterRecord record3 = new BuffDocument(document.asByteBuffer()).as(CharacterRecord.class);
				assertNotEquals(record3, record);
				assertNotEquals(record3.hashCode(), record.hashCode());
			}

			private AbilityScore [] getAbilityScores() {
				String [] names = { "Strength", "Intellligence", "Wisdom", "Dexterity", "Constitution", "Charisma" };
				Integer [] values = { 12, 17, 14, 15, 12, 17 };
				AbilityScore [] scores = new AbilityScore[6];
				for (int i = 0; i < scores.length; i++) {
					scores[i] = BuffDocument.create(AbilityScore.class)
							.withName(names[i])
							.withValue(values[i]);
				}
				return scores;
			}
			
			@Test
			@DisplayName("arrays") 
			void arrays() {
				record.abilityScores(getAbilityScores());
				record = new BuffDocument(record.document().asByteBuffer()).as(CharacterRecord.class);
				AbilityScore [] scoresToCompare = getAbilityScores();
				AbilityScore [] recordsScores = record.abilityScores();
				assertEquals(scoresToCompare.length, recordsScores.length);
				for (int i = 0; i < recordsScores.length; i++) {
					assertEquals(scoresToCompare[i].name(),recordsScores[i].name());
					assertEquals(scoresToCompare[i].value(),recordsScores[i].value());
				}
			}

			@Test
			@DisplayName("generic lists") 
			void lists() {
				AbilityScore [] scores = getAbilityScores();
				assertEquals(0,record.abilityScoreList().size());
				assertTrue(record.abilityScoreList().isEmpty());
				record.abilityScoreList().add(scores[2]);
				assertEquals(1,record.abilityScoreList().size());
				assertFalse(record.abilityScoreList().isEmpty());
				assertEquals("Wisdom",record.abilityScoreList().get(0).name());

				record.abilityScoreList().clear();
				assertEquals(0,record.abilityScoreList().size());
				assertTrue(record.abilityScoreList().isEmpty());
				
			}
			
			@SuppressWarnings("unchecked")
			@Test
			@DisplayName("unchecked lists") 
			void uncheckedLists() {
				AbilityScore [] scores = getAbilityScores();
				assertEquals(0,record.uncheckedAbilityScoreList().size());
				assertTrue(record.uncheckedAbilityScoreList().isEmpty());
				record.uncheckedAbilityScoreList().add(scores[2]);
				assertEquals(1,record.uncheckedAbilityScoreList().size());
				assertFalse(record.uncheckedAbilityScoreList().isEmpty());

				record.uncheckedAbilityScoreList().clear();
				assertEquals(0,record.uncheckedAbilityScoreList().size());
				assertTrue(record.uncheckedAbilityScoreList().isEmpty());
				
			}

			@Test
			@DisplayName("maps") 
			void maps() {
				
			}
		}
	}

	

}
