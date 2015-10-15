/*
 * Copyright (c) 2011-2015 Ant Kutschera
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

public class DefaultEngineTest extends AbstractEngineTest {

	@Override
	public Engine getEngine(List<Rule> rules, boolean throwExceptionIfCompilationFails) throws DuplicateNameException, CompileException, ParseException {
		return new Engine(rules, throwExceptionIfCompilationFails);
	}

	@Override
	protected boolean isJavascriptTest() {
		return false;
	}
	
	@Test
	public void testExecuteBestActionManyActionsFiring() throws ScriptException, IOException{
		Rule r1 = new Rule("SendEmailToUser", "input.config.sendUserEmail == true", "SendEmailToUser", 1, "ch.maxant.someapp.config");
		Rule r2 = new Rule("SendEmailToModerator", "input.config.sendAdministratorEmail == true and input.user.numberOfPostings < 5", "SendEmailToModerator", 2, "ch.maxant.someapp.config");
		List<Rule> rules = Arrays.asList(r1, r2);
		
		final List<String> log = new ArrayList<String>();
		
		AbstractAction<ForumSetup, Void> a1 = new AbstractAction<ForumSetup, Void>("SendEmailToUser") {
			@Override
			public Void execute(ForumSetup input) {
				log.add("Sending email to user!");
				return null;
			}
		};
		AbstractAction<ForumSetup, Void> a2 = new AbstractAction<ForumSetup, Void>("SendEmailToModerator") {
			@Override
			public Void execute(ForumSetup input) {
				log.add("Sending email to moderator!");
				return null;
			}
		};

		try {
			Engine engine = getEngine(rules, true);

			ForumSetup setup = new ForumSetup();
			setup.getConfig().setSendUserEmail(true);
			setup.getConfig().setSendAdministratorEmail(true);
			setup.getUser().setNumberOfPostings(2);
			
			engine.executeAllActions(setup, Arrays.asList(a1, a2));
			assertEquals(2, log.size());
			assertEquals("Sending email to moderator!", log.get(0));
			assertEquals("Sending email to user!", log.get(1));
			
		} catch (DuplicateNameException e) {
			fail(e.getMessage());
		} catch (CompileException e) {
			fail(e.getMessage());
		} catch (ParseException e) {
			fail(e.getMessage());
		} catch (NoMatchingRuleFoundException e) {
			fail(e.getMessage());
		} catch (NoActionFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testRuleWithIteration() throws Exception {
 		
		String expression = 
				"for(student : input.students){" +
				"	if(student.age < 10) return true;" +
				"}" +
				"return false;";

		Rule r1 = new Rule("containsStudentUnder10", expression , "leaveEarly", 1, "ch.maxant.rules", "If a class contains a student under 10 years of age, then the class may go home early");
		
		Rule r2 = new Rule("default", "true" , "leaveOnTime", 0, "ch.maxant.rules", "this is the default");
		
		Classroom classroom = new Classroom();
		classroom.getStudents().add(new Person(12));
		classroom.getStudents().add(new Person(10));
		classroom.getStudents().add(new Person(8));

		Engine e = getEngine(Arrays.asList(r1, r2), true);
		
		assertEquals("leaveEarly", e.getBestOutcome(classroom));
		
		classroom.getStudents().remove(classroom.getStudents().size()-1);

		assertEquals("leaveOnTime", e.getBestOutcome(classroom));
	}

	@Test
	public void testMultithreadingAndPerformance() throws Exception {
		
		String expression = 
				"var i = 0;\n" + //could definitely cause problems, since this is state in the script! but it doesnt seem to!
				"for(; i < input.students.size(); i++){\n" +
				"    var student = input.students[i];\n" +
				"    if(student.age < 10) {\n" +
				"        return true;\n" +
				"    }\n" +
				"}\n" +
				"return false;";

		Rule r1 = new Rule("containsStudentUnder10", expression , "leaveEarly", 1, "ch.maxant.rules", "If a class contains a student under 10 years of age, then the class may go home early");
		
		Rule r2 = new Rule("default", "true" , "leaveOnTime", 0, "ch.maxant.rules", "this is the default");
		
		long start = System.currentTimeMillis();
		final Engine engine = new Engine(Arrays.asList(r1, r2), true);
		System.out.println("Created engine including compiling scripts in " + (System.currentTimeMillis()-start) + "ms");

		ExecutorService pool = Executors.newFixedThreadPool(20);

		final int numTasks = 10000;
		final CountDownLatch latch = new CountDownLatch(numTasks);
		final Random random = new Random();
		start = System.nanoTime();
		for(int i = 0; i < numTasks; i++){
			pool.submit(new Runnable() {
				@Override
				public void run() {
					Classroom classroom = new Classroom();
					int size = random.nextInt(10);
					for(int i = 0; i < size; i++){
						classroom.getStudents().add(new Person(10+random.nextInt(8))); //> 10
					}
					int age = random.nextInt(2) + 9;
					classroom.getStudents().add(new Person(age));
					
					try {
						String outcome = engine.getBestOutcome(classroom);
						if(age < 10){
							if(!outcome.equals("leaveEarly")){
								System.err.println("ERROR-2: " + outcome);
								System.exit(-2);
							}
						}else{
							if(!outcome.equals("leaveOnTime")){
								System.err.println("ERROR-3: " + outcome);
								System.exit(-3);
							}
						}
						latch.countDown();
					} catch (Throwable t) {
						t.printStackTrace();
						System.exit(-1); //since fail does not work inside execution pool
					}
				}
			});
		}
		latch.await();
		pool.shutdown();
		System.out.println("Took on average " + ((System.nanoTime()-start)/1000000.0/numTasks) + "ms per task");
	}
	
	/** tests that regular expressions work in MVEL based rules. rule only matches when input.name is purely characters. 
	 * since input is only characters, the rule is found. */
	@Test
	public void testRuleWithRegExpOK() throws Exception{
		Rule rule1 = new Rule("1", "input.name ~= '[a-zA-Z]*'", "RegExpWasMatched", 1, "ch.maxant.demo");
		List<Rule> rules = Arrays.asList(rule1);
		
		Engine engine = getEngine(rules, true);
		String outcome = engine.getBestOutcome(new Person("John"));
		assertEquals("RegExpWasMatched", outcome);
	}
	
	/** tests that regular expressions work in MVEL based rules. rule only matches when input.name is purely characters. 
	 * <b>since input contains numbers, we expect no rule to match!</b> */
	@Test(expected=NoMatchingRuleFoundException.class)
	public void testRuleWithRegExpNOK() throws Exception{
		Rule rule1 = new Rule("1", "input.name ~= '[a-zA-Z]*'", "RegExpWasMatched", 1, "ch.maxant.demo");
		List<Rule> rules = Arrays.asList(rule1);
		
		Engine engine = getEngine(rules, true);
		engine.getBestOutcome(new Person("F4G5"));
	}
	
}
