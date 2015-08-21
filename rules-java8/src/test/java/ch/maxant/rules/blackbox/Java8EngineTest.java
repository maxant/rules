/*
 * Copyright (c) 2011-2014 Ant Kutschera
 * 
 * This file is part of Ant Kutschera's blog.
 * 
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 * You should have received a copy of the Lesser GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.maxant.rules.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import ch.maxant.rules.CompileException;
import ch.maxant.rules.DuplicateNameException;
import ch.maxant.rules.Engine;
import ch.maxant.rules.IAction;
import ch.maxant.rules.Java8Engine;
import ch.maxant.rules.ParseException;
import ch.maxant.rules.Rule;
import ch.maxant.rules.SamAction;
import ch.maxant.rules.blackbox.AbstractEngineTest.MyInput;
import ch.maxant.rules.blackbox.AbstractEngineTest.Person;

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
		IAction<MyInput, BigDecimal> action1 = new SamAction<MyInput, BigDecimal>("outcome1", i -> new BigDecimal("100.0"));
		IAction<MyInput, BigDecimal> action2 = new SamAction<MyInput, BigDecimal>("outcome2", i -> new BigDecimal("101.0"));

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

		Stream<Rule> streamOfRules = getStreamOfRules();

		//to pass in a stream, we need to use a different Engine
		Java8Engine e = new Java8Engine(streamOfRules, true);

		//to use a lambda, construct a SamAction and pass it a lambda.
		IAction<MyInput, BigDecimal> action1 = new SamAction<MyInput, BigDecimal>("outcome1", i -> new BigDecimal("100.0"));
		IAction<MyInput, BigDecimal> action2 = new SamAction<MyInput, BigDecimal>("outcome2", i -> new BigDecimal("101.0"));
		List<IAction<MyInput, BigDecimal>> actions = Arrays.asList(action1, action2);
		
		MyInput input = new MyInput();
		Person p1 = new Person("ant");
		Person p2 = new Person("clare");
		input.setP1(p1);
		input.setP2(p2);
		
		try {
			BigDecimal price = e.executeBestAction(input, actions.stream());
			assertEquals(new BigDecimal("101.0"), price);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}

	private Stream<Rule> getStreamOfRules() {
		Rule rule1 = new Rule("R1", "input.p1.name == \"ant\" && input.p2.name == \"clare\"", "outcome1", 0, "ch.maxant.produkte", "Spezi Regel für Familie Kutschera");
		Rule rule2 = new Rule("R2", "true", "outcome2", 1, "ch.maxant.produkte", "Default Regel");
		List<Rule> rules = Arrays.asList(rule1, rule2);
		return rules.stream();
	}
	
}
