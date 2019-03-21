package ch.maxant.rules.blackbox;

import ch.maxant.rules.*;
import org.junit.Test;
import org.mvel2.MVEL;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
		
		long start = currentTimeMillis();
		final Engine engine = new Engine(Arrays.asList(r1, r2), true);
		System.out.println("Created engine including compiling scripts in " + (currentTimeMillis()-start) + "ms");

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

	public static String getSomeString() {
	    return "THIS_NEVER_CHANGES";
    }

	@Test
    public void testStatics() throws Exception {
	    Map<String, Object> statics = new HashMap<String, Object>();
	    statics.put("someString", MVEL.getStaticMethod(this.getClass(), "getSomeString", new Class[0])); // import a static method

		Rule rule1 = new Rule("1", "input.name == someString()", "ok", 1, "ch.maxant.demo");
        Engine e = new Engine(singletonList(rule1), true, statics);

        List<Rule> matchingRules = e.getMatchingRules(new Person("THIS_NEVER_CHANGES"));

        assertEquals(1, matchingRules.size());
        assertEquals(rule1, matchingRules.get(0));
    }

    @Test
    public void testIllegalWords() throws Exception {
        // NOT allowed to use "new " keyword
        Rule rule1 = new Rule("1", "input.name == new Person('John', 18).name", "ok", 1, "ch.maxant.demo");
        try {
            new Engine(singletonList(rule1), true);
            fail("no exception");
        }catch (IllegalArgumentException e) {
            assertEquals("Rule has an illegal word! None of the following words may be contained in rules: [Runtime, java, new , getBean, System, InitialContext]. Alternatively override Engine#initIllegalWords or Engine#verifyLegal.", e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalWords_overrideAddMore() throws Exception {
        // NOT allowed to use "new " keyword
        Rule rule1 = new Rule("1", "input.name == 'John'", "ok", 1, "ch.maxant.demo");
        new Engine(singletonList(rule1), true){
            @Override
            protected Set<String> initIllegalWords() {
                Set<String> illegalWords = super.initIllegalWords();
                illegalWords.add("John");
                return illegalWords;
            }
        };
    }

    @Test
    public void testIllegalWords_overrideNothingIllegal() throws Exception {
        // NOT allowed to use "java" keyword => note that if you did need to use MAX_VALUE, you can because java lets you refer to the Integer class without the package
        Rule rule1 = new Rule("1", "input.age == java.lang.Integer.MAX_VALUE", "ok", 1, "ch.maxant.demo");
        new Engine(singletonList(rule1), true){
            @Override
            protected Set<String> initIllegalWords() {
                return new HashSet<String>(); //nothings illegal here since we trust our rule authors entirely
            }
        };
    }
}
