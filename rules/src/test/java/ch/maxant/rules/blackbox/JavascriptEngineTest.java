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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptException;

import org.junit.Test;

import ch.maxant.rules.AbstractAction;
import ch.maxant.rules.CompileException;
import ch.maxant.rules.DuplicateNameException;
import ch.maxant.rules.Engine;
import ch.maxant.rules.JavascriptEngine;
import ch.maxant.rules.NoActionFoundException;
import ch.maxant.rules.NoMatchingRuleFoundException;
import ch.maxant.rules.ParseException;
import ch.maxant.rules.Rule;

public class JavascriptEngineTest extends AbstractEngineTest {

	@Override
	public Engine getEngine(List<Rule> rules, boolean throwExceptionIfCompilationFails)
			throws DuplicateNameException, CompileException, ParseException, ScriptException, IOException {
		return new JavascriptEngine(rules, throwExceptionIfCompilationFails);
	}
	
	@Override
	protected boolean isJavascriptTest() {
		return true;
	}
	
	@Test
	public void testLibraryNotFound() throws DuplicateNameException, ParseException, ScriptException, IOException{
		try{
			new JavascriptEngine(new ArrayList<Rule>(), true, "unknown");
		}catch(CompileException e){
			assertEquals("No file named 'unknown' found on classpath. Assumed a script was passed instead.  But failed to evaluate script: ReferenceError: \"unknown\" is not defined in <eval> at line number 1", e.getMessage());
		}
	}
	
	@Test
	public void testExecuteBestActionManyActionsFiring() throws ScriptException, IOException{
		Rule r1 = new Rule("SendEmailToUser", "input.config.sendUserEmail == true", "SendEmailToUser", 1, "ch.maxant.someapp.config");
		Rule r2 = new Rule("SendEmailToModerator", "input.config.sendAdministratorEmail == true && input.user.numberOfPostings < 5", "SendEmailToModerator", 2, "ch.maxant.someapp.config");
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
	public void testRuleWithIterationUsingLibrary() throws Exception {

		String expression = 
				"_(input.students).some(function(student){" +
				"	return student.age < 10;" +
				"});";

		Rule r1 = new Rule("containsStudentUnder10", expression , "leaveEarly", 1, "ch.maxant.rules", "If a class contains a student under 10 years of age, then the class may go home early");
		
		Rule r2 = new Rule("default", "true" , "leaveOnTime", 0, "ch.maxant.rules", "this is the default");
		
		Classroom classroom = new Classroom();
		classroom.getStudents().add(new Person(12));
		classroom.getStudents().add(new Person(10));
		classroom.getStudents().add(new Person(8));

		Engine e = new JavascriptEngine(Arrays.asList(r1, r2), true, "lodash-3.10.0.js");
		
		assertEquals("leaveEarly", e.getBestOutcome(classroom));
		
		classroom.getStudents().remove(classroom.getStudents().size()-1);

		assertEquals("leaveOnTime", e.getBestOutcome(classroom));
	}

	/** test shows how you can add your own very complex business rules to the engine */
	@Test
	public void testComplexRuleInLibrary() throws Exception {
		String expression = "maxant.rule419(input) === 'Scam'";

		Rule r1 = new Rule("mightBeScam", expression , "higherPremium", 1, "ch.maxant.rules", "If call to our library returns 'Scam', then this may be a scam, so charge a higher premium");
		
		Rule r2 = new Rule("default", "true" , "standardPremium", 0, "ch.maxant.rules", "this is the default");

		List<Person> people = new ArrayList<Person>();
		people.add(new Person("John"));
		people.add(new Person("Ant"));

		Engine e = new JavascriptEngine(Arrays.asList(r1, r2), true, "maxant.js", "lodash-3.10.0.js");
		
		assertEquals("higherPremium", e.getBestOutcome(people));
		
		people.remove(0);

		assertEquals("standardPremium", e.getBestOutcome(people));
	}
	
	@Test
	public void testMultithreadingAndPerformance_NoProblemsExpectedBecauseScriptsAreStateless() throws Exception {
		
		String expression = 
				"_(input.students).some(function(student){" +
				"	return student.age < 10;" +
				"});";

		Rule r1 = new Rule("containsStudentUnder10", expression , "leaveEarly", 1, "ch.maxant.rules", "If a class contains a student under 10 years of age, then the class may go home early");
		
		Rule r2 = new Rule("default", "true" , "leaveOnTime", 0, "ch.maxant.rules", "this is the default");
		
		long start = System.currentTimeMillis();
		final Engine engine = new JavascriptEngine(Arrays.asList(r1, r2), true, "lodash-3.10.0.js");
		System.out.println("Created engine including compiling scripts in " + (System.currentTimeMillis()-start) + "ms");

		ExecutorService pool = Executors.newFixedThreadPool(20);

		final int numTasks = 1000;
		final CountDownLatch latch = new CountDownLatch(numTasks);
		final Random random = new Random();
		start = System.currentTimeMillis();
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
					classroom.getStudents().add(new Person(age)); //9 or 10
					
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
					} catch (Throwable t) {
						t.printStackTrace();
						System.exit(-1); //since fail does not work inside execution pool
					} finally {
						latch.countDown();
					}
				}
			});
		}
		latch.await();
		pool.shutdown();
		System.out.println("Took on average " + ((System.currentTimeMillis()-start)/numTasks) + "ms per task");
		
		assertEquals(0, ((JavascriptEngine)engine).getPoolSize()[0]);
		System.out.println("JavaScript engines Instances (active, idle): " + Arrays.toString(((JavascriptEngine)engine).getPoolSize()));
		System.out.println("Before GC:");
		System.out.println("Free memory: " + (Runtime.getRuntime().freeMemory()/1024/1024) + "MB");
		System.out.println("Total memory: " + (Runtime.getRuntime().totalMemory()/1024/1024) + "MB");
		System.out.println("Used memory: " + ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024) + "MB");
		System.gc();
		System.out.println("After GC:");
		System.out.println("JavaScript engines Instances (active, idle): " + Arrays.toString(((JavascriptEngine)engine).getPoolSize()));
		System.out.println("Free memory: " + (Runtime.getRuntime().freeMemory()/1024/1024) + "MB");
		System.out.println("Total memory: " + (Runtime.getRuntime().totalMemory()/1024/1024) + "MB");
		System.out.println("Used memory: " + ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024) + "MB");
	}
	
	@Test
	public void testMultithreadingStatefulRules_NoProblemsExpectedBecauseOfEnginePool() throws Exception {

		String expression = "rule420() === 40000.0"; //will only evaluate to true if there is no concurrency, because the function is stateful!

		Rule r1 = new Rule("valueIsCorrect", expression, "threadSafe", 1, "ch.maxant.rules", "If this rule fires, then the engine is thread safe");
		
		Rule r2 = new Rule("default", "true" , "notThreadSafe", 0, "ch.maxant.rules");
		
		final Engine engine = new JavascriptEngine(Arrays.asList(r1, r2), true, "bad-stateful-rule.js");
		final AtomicInteger successCount = new AtomicInteger();
		final AtomicInteger unsuccessCount = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(50);

		final int numTasks = 10000;
		final CountDownLatch latch = new CountDownLatch(numTasks);
		long start = System.currentTimeMillis();
		for(int i = 0; i < numTasks; i++){
			pool.submit(new Runnable() {
				@Override
				public void run() {
					try {
						try {
							String outcome = engine.getBestOutcome(0);
							if("threadSafe".equals(outcome)){
								successCount.incrementAndGet();
							}else{
								System.err.println(outcome);
								unsuccessCount.incrementAndGet();
							}
						} catch (Throwable t) {
							t.printStackTrace();
							System.exit(-1); //since fail does not work inside execution pool
						}
					}finally{
						latch.countDown();
					}
				}
			});
		}
		latch.await();
		pool.shutdown();
		System.out.println("Took on average " + ((System.currentTimeMillis()-start)/numTasks) + "ms per task. successful calls: " + successCount + ", total calls: " + numTasks);
		assertEquals(numTasks, successCount.get());
		assertEquals(0, unsuccessCount.get());

		assertEquals(0, ((JavascriptEngine)engine).getPoolSize()[0]);
		System.out.println("JavaScript engines Instances (active, idle): " + Arrays.toString(((JavascriptEngine)engine).getPoolSize()));
		System.out.println("Before GC:");
		System.out.println("Free memory: " + (Runtime.getRuntime().freeMemory()/1024/1024) + "MB");
		System.out.println("Total memory: " + (Runtime.getRuntime().totalMemory()/1024/1024) + "MB");
		System.out.println("Used memory: " + ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024) + "MB");
		System.gc();
		System.out.println("After GC:");
		System.out.println("JavaScript engines Instances (active, idle): " + Arrays.toString(((JavascriptEngine)engine).getPoolSize()));
		System.out.println("Free memory: " + (Runtime.getRuntime().freeMemory()/1024/1024) + "MB");
		System.out.println("Total memory: " + (Runtime.getRuntime().totalMemory()/1024/1024) + "MB");
		System.out.println("Used memory: " + ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024) + "MB");
	}

	@Test
	public void testScriptCanUseBeanOrJavaNotation() throws Exception {
		Collection<Rule> rules = Arrays.asList(new Rule("name", "input.getName() === 'John'", "ok", 1, "ch.maxant.test"));
		JavascriptEngine engine = new JavascriptEngine(rules, "input", true, 10, false);
		String bestOutcome = engine.getBestOutcome(new Person("John"));
		assertEquals("ok", bestOutcome);

		rules = Arrays.asList(new Rule("name", "input.name === 'John'", "ok", 1, "ch.maxant.test"));
		engine = new JavascriptEngine(rules, "input", true, 10, false);
		bestOutcome = engine.getBestOutcome(new Person("John"));
	}
	
	@Test
	public void testLoadScriptRatherThanFile() throws Exception {
		Collection<Rule> rules = Arrays.asList(new Rule("name", "f() === 1", "ok", 1, "ch.maxant.test"));
		JavascriptEngine engine = new JavascriptEngine(rules, "input", true, 10, false, "function f(){return 1;}");
		String bestOutcome = engine.getBestOutcome(1);
		assertEquals("ok", bestOutcome);
	}
	
	@Test
	public void testLoadBadScriptRatherThanFile() throws Exception {
		Collection<Rule> rules = Arrays.asList(new Rule("name", "f() === 1", "ok", 1, "ch.maxant.test"));
		try{
			new JavascriptEngine(rules, "input", true, 10, false, "function f(){%someInvalidToken}");
			fail("no exception");
		}catch(CompileException e){
			assertTrue(e.getMessage(), e.getMessage().contains("Assumed a script was passed instead.  But failed to evaluate script: <eval>:1:13 Expected an operand but found %"));
		}
	}
	
	@Test
	public void testPreloadPoolAndPoolSize() throws Exception {
		
		Collection<Rule> rules = Arrays.asList(new Rule("name", "true", "ok", 1, "ch.maxant.test"));
		JavascriptEngine engine = new JavascriptEngine(rules, "input", true, 10, true);
		
		assertEquals(0, engine.getPoolSize()[0]);
		assertTrue(engine.getPoolSize()[1] >= 8); //altho ten are created, the pool seems to like having its default number in it, and two are removed...
	}

	@Test
	public void testBuilder() throws Exception {
		Collection<Rule> rules = Arrays.asList(new Rule("name", "input.getName() === 'John'", "ok", 1, "ch.maxant.test"));
		JavascriptEngine engine = new JavascriptEngine.Builder(rules).withPoolSize(2).withPreloadPool(true).build();
		String bestOutcome = engine.getBestOutcome(new Person("John"));
		assertEquals("ok", bestOutcome);
		assertEquals(0, engine.getPoolSize()[0]);
		assertTrue(engine.getPoolSize()[1] == 2);
	}

}
