package ch.maxant.rules.blackbox;

import ch.maxant.rules.*;
import ch.maxant.rules.blackbox.AbstractEngineTest.MyInput;
import ch.maxant.rules.blackbox.AbstractEngineTest.Person;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Java8EngineTest {

	/**
	 * shows how to use the rule engine with lambdas.
	 */
	@Test
	public void testLambdas() throws DuplicateNameException, CompileException, ParseException {
		Rule rule1 = new Rule("R1", "input.p1.name == \"ant\" && input.p2.name == \"clare\"", "outcome1", 0, "ch.maxant.produkte", "Spezi Regel für Familie Kutschera");
		Rule rule2 = new Rule("R2", "true", "outcome2", 1, "ch.maxant.produkte", "Default Regel");
		List<Rule> rules = Arrays.asList(rule1, rule2);

		//to use a lambda, construct a SamAction and pass it a lambda.
		IAction<MyInput, BigDecimal> action1 = new SamAction<>("outcome1", i -> new BigDecimal("100.0"));
		IAction<MyInput, BigDecimal> action2 = new SamAction<>("outcome2", i -> new BigDecimal("101.0"));

		List<IAction<MyInput, BigDecimal>> actions = Arrays.asList(action1, action2);
		
		Engine e = new Engine(rules, true);
		
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

	/**
	 * shows how to use the rule engine with streams and lambdas.
	 */
	@Test
	public void testStreamsAndLambdas() throws DuplicateNameException, CompileException, ParseException {

		Collection<Rule> streamOfRules = getRules();

		//to pass in a stream, we need to use a different Engine
		Engine e = new Engine(streamOfRules, true);

		//to use a lambda, construct a SamAction and pass it a lambda.
		IAction<MyInput, BigDecimal> action1 = new SamAction<>("outcome1", i -> new BigDecimal("100.0"));
		IAction<MyInput, BigDecimal> action2 = new SamAction<>("outcome2", i -> new BigDecimal("101.0"));
		List<IAction<MyInput, BigDecimal>> actions = Arrays.asList(action1, action2);
		
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

	private Collection<Rule> getRules() {
		Rule rule1 = new Rule("R1", "input.p1.name == \"ant\" && input.p2.name == \"clare\"", "outcome1", 0, "ch.maxant.produkte", "Spezi Regel für Familie Kutschera");
		Rule rule2 = new Rule("R2", "true", "outcome2", 1, "ch.maxant.produkte", "Default Regel");
        return Arrays.asList(rule1, rule2);
	}
	
}
