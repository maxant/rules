package ch.maxant.rules.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.junit.Test;

import ch.maxant.rules.AbstractAction;
import ch.maxant.rules.CompileException;
import ch.maxant.rules.DuplicateNameException;
import ch.maxant.rules.Engine;
import ch.maxant.rules.NoActionFoundException;
import ch.maxant.rules.NoMatchingRuleFoundException;
import ch.maxant.rules.ParseException;
import ch.maxant.rules.Rule;
import ch.maxant.rules.SubRule;

public abstract class AbstractEngineTest {

	public abstract Engine getEngine(List<Rule> rules, boolean throwExceptionIfCompilationFails) throws DuplicateNameException, CompileException, ParseException, ScriptException, IOException;

	protected abstract boolean isJavascriptTest();

	/**
	 * similar to {@link #test1()}, but reversed rule priority.
	 * @throws ParseException 
	 * @throws CompileException 
	 * @throws IOException 
	 * @throws ScriptException 
	 */
	@Test
	public void test2() throws DuplicateNameException, CompileException, ParseException, ScriptException, IOException {
		Rule rule1 = new Rule("R1", "input.p1.name == \"ant\" && input.p2.name == \"clare\"", "outcome1", 0, "ch.maxant.produkte", "Spezi Regel für Familie Kutschera");
		Rule rule2 = new Rule("R2", "true", "outcome2", 1, "ch.maxant.produkte", "Default Regel");
		List<Rule> rules = Arrays.asList(rule1, rule2);

		AbstractAction<MyInput, BigDecimal> action1 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome1") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("100.0");
			}
		};
		
		AbstractAction<MyInput, BigDecimal> action2 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome2") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("101.0");
			}
		};
		List<AbstractAction<MyInput, BigDecimal>> actions = Arrays.asList(action1, action2);
		
		Engine e = getEngine(rules, true);
		
		MyInput input = new MyInput();
		Person p1 = new Person("ant");
		Person p2 = new Person("clare");
		input.setP1(p1);
		input.setP2(p2);
		
		try {
			BigDecimal price = e.executeBestAction(input, actions);
			assertEquals(new BigDecimal("101.0"), price);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}
	
	@Test
	public void testNamespaceMatching() throws DuplicateNameException, CompileException, ParseException{
		Rule rule1 = new Rule("R1", "true", "outcome1", 0, "ch.maxant.produkte", "one");
		Rule rule2 = new Rule("R1", "true", "outcome2", 1, "ch.maxant.fahrplan", "two");
		List<Rule> rules = Arrays.asList(rule1, rule2);
		
		AbstractAction<MyInput, BigDecimal> action1 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome1") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("100.0");
			}
		};
		AbstractAction<MyInput, BigDecimal> action2 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome2") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("101.0");
			}
		};
		List<AbstractAction<MyInput, BigDecimal>> actions = Arrays.asList(action1, action2);
		
		try {
			Engine e = getEngine(rules, true);
			MyInput input = new MyInput();
			
			BigDecimal price = e.executeBestAction("ch\\.maxant\\.prod.*", input, actions);
			assertEquals(new BigDecimal("100.0"), price);
		} catch (Exception ex) {
			fail("not expected");
		}
	}
	
	@Test
	public void testNamespaceMatchingFailed() throws DuplicateNameException, CompileException, ParseException, ScriptException, IOException{
		Rule rule1 = new Rule("R1", "true", "outcome1", 0, "ch.maxant.produkte", "one");
		Rule rule2 = new Rule("R1", "true", "outcome2", 1, "ch.maxant.fahrplan", "two");
		List<Rule> rules = Arrays.asList(rule1, rule2);
		
		AbstractAction<MyInput, BigDecimal> action1 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome1") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("100.0");
			}
		};
		AbstractAction<MyInput, BigDecimal> action2 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome2") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("101.0");
			}
		};
		List<AbstractAction<MyInput, BigDecimal>> actions = Arrays.asList(action1, action2);

		Engine e = getEngine(rules, true);
		
		MyInput input = new MyInput();
		
		BigDecimal price = null;
		try {
			price = e.executeBestAction("co\\.uk\\.maxant\\..*", input, actions);
			fail("no matching rule was found");
		} catch (NoMatchingRuleFoundException ex) {
			assertNull(price);
		} catch (Exception ex) {
			fail("not expected");
		}
	}
	
	/**
	 * tests getting a list of rules associated with {@link StringAction}s.
	 */
	@Test
	public void testGetList() {
		Rule rule1 = new Rule("A", "input.distance < 100", "productA", 1, "ch.maxant.produkte", "Rule for product A");
		Rule rule2 = new Rule("B", "input.distance > 100", "productB", 2, "ch.maxant.produkte", "Rule for product B");
		Rule rule3 = new Rule("C", "input.distance > 150", "productC", 3, "ch.maxant.produkte", "Rule for product C");
		Rule rule4 = new Rule("D", "input.distance > 100 && (input.map[\"travelClass\"] == 1)", "productC", 4, "ch.maxant.produkte", "Rule for product C");
		List<Rule> rules = Arrays.asList(rule1, rule2, rule3, rule4);

		try{
			Engine e = getEngine(rules, true);
			
			TravelRequest request = new TravelRequest(50);
			request.put("travelClass", 2);
			List<Rule> rs = e.getMatchingRules(null, request);
			assertEquals(1, rs.size());
			assertEquals(rule1, rs.get(0));
			assertEquals("productA", e.getBestOutcome(request));

			request = new TravelRequest(102);
			request.put("travelClass", 2);
			rs = e.getMatchingRules(null,  request);
			assertEquals(1, rs.size());
			assertEquals(rule2, rs.get(0));
			assertEquals("productB", e.getBestOutcome(request));
			
			request = new TravelRequest(152);
			request.put("travelClass", 2);
			rs = e.getMatchingRules(null,  request);
			assertEquals(2, rs.size());
			assertEquals(rule3, rs.get(0));
			assertEquals(rule2, rs.get(1));
			assertEquals("productC", e.getBestOutcome(request));

			request = new TravelRequest(50);
			request.put("travelClass", 1);
			rs = e.getMatchingRules(null,  request);
			assertEquals(1, rs.size());
			assertEquals(rule1, rs.get(0));
			assertEquals("productA", e.getBestOutcome(request));
			
			request = new TravelRequest(102);
			request.put("travelClass", 1);
			rs = e.getMatchingRules(null,  request);
			assertEquals(2, rs.size());
			assertEquals(rule4, rs.get(0));
			assertEquals(rule2, rs.get(1));
			assertEquals("productC", e.getBestOutcome(request));
			
			request = new TravelRequest(152);
			request.put("travelClass", 1);
			rs = e.getMatchingRules(null,  request);
			assertEquals(3, rs.size());
			assertEquals(rule4, rs.get(0));
			assertEquals(rule3, rs.get(1));
			assertEquals(rule2, rs.get(2));
			assertEquals("productC", e.getBestOutcome(request));
		}catch(Exception ex){
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
	
	@Test
	public void testSwallowELException() throws ScriptException, IOException{
		Rule rule = new Rule("1", "input eq 345", "SomeCommand", 0, "ch.maxant.produkte");
		List<Rule> rules = Arrays.asList(rule);

		try{
			Engine e = getEngine(rules, false); //false!! thats what we are testing!!
			
			List<Rule> rs = e.getMatchingRules(null, 123);
			
			assertNotNull(rs);
			assertEquals(0, rs.size());
		}catch(DuplicateNameException ex){
			fail("didnt expect this exception during this test...");
		} catch (CompileException ex) {
			fail("exception was indeed thrown...");
		} catch (ParseException ex) {
			fail("didnt expect this exception during this test...");
		}
	}
	
	@Test
	public void testBadExpression(){
		Rule rule = new Rule("1", "input someIllegalOperator 345", "SomeCommand", 0, "ch.maxant.produkte");
		List<Rule> rules = Arrays.asList(rule);

		try{
			getEngine(rules, true);
			
			fail("no exception found");
		}catch(Exception ex){
			if(isJavascriptTest()){
				assertTrue(ex.getMessage().contains("<eval>:1:6 Expected ; but found someIllegalOperator"));
				assertTrue(ex.getMessage().contains("input someIllegalOperator 345"));
				assertTrue(ex.getMessage().contains("^ in <eval> at line number 1 at column number 6"));
			}else{
				assertTrue(ex.getMessage().startsWith("[Error: unknown class or illegal statement: "));
			}
		}
	}

	@Test
	public void testOverrideInputName() throws Exception {
		Rule rule= new Rule("1", "jane.age > 34", "veryOld", 1, "ch.maxant.produkte");
		List<Rule> rules = Arrays.asList(rule);

		assertEquals("veryOld", new Engine(rules, "jane", true).getBestOutcome(new Person(35)));
		assertEquals(0, new Engine(rules, "jane", true).getMatchingRules(new Person(33)).size());
	}
	
	@Test
	public void testReferencedRuleIsARuleRatherThanASubrule(){
		Rule rule1 = new Rule("1", "true", "SomeCommand", 1, "ch.maxant.produkte");
		Rule rule2 = new Rule("2", "#1", "SomeCommand", 2, "ch.maxant.produkte");
		List<Rule> rules = Arrays.asList(rule1, rule2);

		try{
			Engine e = getEngine(rules, true);
			
			List<Rule> rs = e.getMatchingRules(null,  234);
			
			//both are returned, because a referencedrule is also a rule.  only subrules are not returned!
			
			assertEquals(2, rs.size());
			assertEquals(rule2.getName(), rs.get(0).getName());
			assertEquals(rule1, rs.get(1));
			
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals("Error while attempting to add subrule to rule ch.maxant.produkte.2.  Unable to replace #1 with subrule ch.maxant.produkte.1 because it is a rule, rather than a subrule", ex.getMessage());
		}
	}
	
	@Test
	public void testSubrules(){
	    SubRule sr1 = new SubRule("1", "true", "ch.maxant.test");
	    SubRule sr2 = new SubRule("2", "false", "ch.maxant.test");
	    Rule rule = new Rule("3", "#1 && !#2", "Bingo", 1, "ch.maxant.test");
	    List<Rule> rules = Arrays.asList(sr1, sr2, rule);
	    
	    try{
	        Engine e = getEngine(rules, true);
	        
	        List<Rule> matches = e.getMatchingRules(null);
	        assertEquals(1, matches.size());
	        assertEquals(rule.getName(), matches.get(0).getName());
	        
	        String result = e.getBestOutcome(null);
	        assertEquals("Bingo", result);
	        
	    }catch(Exception ex){
	        ex.printStackTrace();
	        fail();
	    }
	}
	
	@Test
	public void testSubrulesWithActions(){
	    SubRule sr1 = new SubRule("1", "true", "ch.maxant.test");
	    SubRule sr2 = new SubRule("2", "false", "ch.maxant.test");
	    Rule rule = new Rule("3", "#1 && !#2", "Bingo", 1, "ch.maxant.test");
	    List<Rule> rules = Arrays.asList(sr1, sr2, rule);

        AbstractAction<MyInput, BigDecimal> action1 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("Bingo") {
            @Override
            public BigDecimal execute(MyInput input) {
                return new BigDecimal("100.0");
            }
        };
	    
        List<AbstractAction<MyInput, BigDecimal>> actions = Arrays.asList(action1);

        try {
            Engine e = getEngine(rules, true);
            
            MyInput input = new MyInput();
            
            BigDecimal price = null;

            price = e.executeBestAction(input, actions);
            assertEquals(new BigDecimal("100.0"), price);
	        
	    }catch(Exception ex){
	        ex.printStackTrace();
	        fail();
	    }
	}
	
	@Test
	public void testJavadocExample() throws ScriptException, IOException{
		
		Rule r1 = new Rule("YouthTarif", "input.person.age < 26", "YT2011", 3, "ch.maxant.someapp.tarifs");
		Rule r2 = new Rule("SeniorTarif", "input.person.age > 59", "ST2011", 3, "ch.maxant.someapp.tarifs");
		Rule r3 = new Rule("DefaultTarif", "!#YouthTarif && !#SeniorTarif", "DT2011", 3, "ch.maxant.someapp.tarifs");
		Rule r4 = new Rule("LoyaltyTarif", "#DefaultTarif && input.account.ageInMonths > 24", "LT2011", 4, "ch.maxant.someapp.tarifs");
		List<Rule> rules = Arrays.asList(r1, r2, r3, r4);

		try {
			Engine engine = getEngine(rules, true);

			TarifRequest request = new TarifRequest();
			request.setPerson(new Person("p"));
			request.setAccount(new Account());

			request.getPerson().setAge(24);
			request.getAccount().setAgeInMonths(5);
			String tarif = engine.getBestOutcome(request);
			assertEquals("YT2011", tarif);
			assertEquals(1, engine.getMatchingRules(request).size());
			
			request.getPerson().setAge(24);
			request.getAccount().setAgeInMonths(35);
			tarif = engine.getBestOutcome(request);
			assertEquals("YT2011", tarif);
			assertEquals(1, engine.getMatchingRules(request).size());
			
			request.getPerson().setAge(35);
			request.getAccount().setAgeInMonths(5);
			tarif = engine.getBestOutcome(request);
			assertEquals("DT2011", tarif);
			assertEquals(1, engine.getMatchingRules(request).size());
			
			request.getPerson().setAge(35);
			request.getAccount().setAgeInMonths(35);
			tarif = engine.getBestOutcome(request);
			assertEquals("LT2011", tarif);
			assertEquals(2, engine.getMatchingRules(request).size()); //since DT2011 and LT2011 both match
			
			request.getPerson().setAge(65);
			request.getAccount().setAgeInMonths(5);
			tarif = engine.getBestOutcome(request);
			assertEquals("ST2011", tarif);
			assertEquals(1, engine.getMatchingRules(request).size());
			
			request.getPerson().setAge(65);
			request.getAccount().setAgeInMonths(35);
			tarif = engine.getBestOutcome(request);
			assertEquals("ST2011", tarif);
			assertEquals(1, engine.getMatchingRules(request).size());
			
		} catch (DuplicateNameException e) {
			fail(e.getMessage());
		} catch (CompileException e) {
			fail(e.getMessage());
		} catch (ParseException e) {
			fail(e.getMessage());
		} catch (NoMatchingRuleFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testExecuteBestActionDuplicateName(){
		Rule rule1 = new Rule("1", "true", "SomeCommand", 3, "ch.maxant.produkte");
		List<Rule> rules = Arrays.asList(rule1);

		AbstractAction<MyInput, BigDecimal> action1 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome1") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("100.0");
			}
		};
		AbstractAction<MyInput, BigDecimal> action2 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome1") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("101.0");
			}
		};
		List<AbstractAction<MyInput, BigDecimal>> actions = Arrays.asList(action1, action2);
		
		try{
			Engine engine = getEngine(rules, true);
			engine.executeBestAction(new MyInput(), actions);
			fail("why no expection for duplicate action name?");
		}catch(DuplicateNameException ex){
			assertEquals("The name outcome1 was found in a different action.  Action names must be unique.", ex.getMessage());
		}catch(Exception ex){
			fail(ex.getMessage());
		}
	}
	
	@Test
	public void testExecuteBestActionNoActionFound(){
		Rule rule1 = new Rule("1", "true", "SomeCommand", 3, "ch.maxant.produkte");
		List<Rule> rules = Arrays.asList(rule1);
		
		AbstractAction<MyInput, BigDecimal> action1 = new AbstractAction<AbstractEngineTest.MyInput, BigDecimal>("outcome1") {
			@Override
			public BigDecimal execute(MyInput input) {
				return new BigDecimal("100.0");
			}
		};
		List<AbstractAction<MyInput, BigDecimal>> actions = Arrays.asList(action1);
		
		try{
			Engine engine = getEngine(rules, true);
			engine.executeBestAction(new MyInput(), actions);
			fail("why no expection for duplicate action name?");
		}catch(NoActionFoundException ex){
			assertEquals("No action has been associated with the outcome \"SomeCommand\"", ex.getMessage());
		}catch(Exception ex){
			fail(ex.getMessage());
		}
	}

	public static final class Person {
		private String name;
		private Integer age;
		public Person(int age) {
			this.age = age;
		}
		public Person(String name) {
			this.name = name;
		}
		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Integer getAge() {
			return age;
		}
		public void setAge(Integer age) {
			this.age = age;
		}
	}
	
	public static final class MyInput {
		private Person p1;
		private Person p2;
		public Person getP1() {
			return p1;
		}
		public void setP1(Person p1) {
			this.p1 = p1;
		}
		public Person getP2() {
			return p2;
		}
		public void setP2(Person p2) {
			this.p2 = p2;
		}
	}
	
	public static final class Account {
		private int ageInMonths;
		public void setAgeInMonths(int ageInMonths) {
			this.ageInMonths = ageInMonths;
		}
		public int getAgeInMonths() {
			return ageInMonths;
		}
	}

	public static final class TarifRequest {
		private Person p;
		private Account a;
		public Person getPerson() {
			return p;
		}
		public void setPerson(Person p) {
			this.p = p;
		}
		public Account getAccount() {
			return a;
		}
		public void setAccount(Account a) {
			this.a = a;
		}
	}
	
	public static final class TravelRequest {
		private int distance;
		private Map<Object, Object> map = new HashMap<Object, Object>();
		public TravelRequest(int distance) {
			this.distance = distance;
		}
		public void put(Object key, Object value){
			map.put(key, value);
		}
		public int getDistance() {
			return distance;
		}
		@SuppressWarnings("rawtypes")
		public Map getMap() {
			return map;
		}
	}
	
	public static final class ForumSetup {
		private Config config = new Config();
		private User user = new User();
		public Config getConfig() {
			return config;
		}
		public User getUser() {
			return user;
		}
	}
	
	public static final class Config {
		private boolean sendUserEmail;
		private boolean sendAdministratorEmail;
		public void setSendUserEmail(boolean sendUserEmail) {
			this.sendUserEmail = sendUserEmail;
		}
		public void setSendAdministratorEmail(boolean sendAdministratorEmail) {
			this.sendAdministratorEmail = sendAdministratorEmail;
		}
		public boolean isSendAdministratorEmail() {
			return sendAdministratorEmail;
		}
		public boolean isSendUserEmail() {
			return sendUserEmail;
		}
	}

	public static final class User {
		private int numberOfPostings = 0;
		public void setNumberOfPostings(int numberOfPostings) {
			this.numberOfPostings = numberOfPostings;
		}
		public int getNumberOfPostings() {
			return numberOfPostings;
		}
	}
	
	public static final class Classroom {
		List<Person> students = new ArrayList<Person>();
		public List<Person> getStudents() {
			return students;
		}
	}
}
