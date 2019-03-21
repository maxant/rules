package ch.maxant.rules;

import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A Rule Engine.  Can evaluate rules and execute {@link IAction}s or simply provide an 
 * ordered list of the best matching {@link Rule}s.<br>
 * <br>
 * If you are using Java 8, investigate the rules-java8 module!
 * <br>
 * <br>
 * A fuller explanation of how to use rules can be found in the Javadoc for {@link Rule}s.<br>
 * <br>
 * Based on MVEL expression language from Codehaus.<br>
 * <br>
 * Imagine a tarif system which your business needed to be highly configurable.
 * The business wants to be able to specify new tarifs or update existing ones
 * at any time.  The criteria used to determine which tarifs are relevant
 * to a customer wanting to make a purchase are based on the attributes belonging 
 * firstly to the person, and secondly to their account.  For example, a customer
 * who is under 26 is eligible for a youth tarif.  Seniors are eligible for their own
 * tarif too.  Additionally, customers who do not get a youth or senior tarif
 * are entitled to a loyalty tarif if they have had an account for more than 24 months.<br>
 * <br>
 * These business rules can be modelled ina  database as a set of string.  The data 
 * can then be loaded and modelled in the code using the {@link Rule} API. 
 * Consider the following four rules:<br>
 * <br>
 * <code>
 * Rule r1 = new Rule("YouthTarif",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"input.age &lt; 26", new StringAction("YT2011"), 3,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ch.maxant.someapp.tarifs", null);<br>
 * Rule r2 = new Rule("SeniorTarif",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"input.age &gt; 59", new StringAction("ST2011"), 3,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ch.maxant.someapp.tarifs", null);<br>
 * Rule r3 = new Rule("DefaultTarif",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"!#YouthTarif &amp;&amp; !#SeniorTarif", new StringAction("DT2011"), 3,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ch.maxant.someapp.tarifs", null);<br>
 * Rule r4 = new Rule("LoyaltyTarif",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"#DefaultTarif &amp;&amp; input.account.ageInMonths &gt; 24",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new StringAction("LT2011"), 4,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ch.maxant.someapp.tarifs", null);<br>
 * </code>
 * <br>
 * The following object model can then be created and used to evaluate the rules.<br>
 * <br>
 * <code>
 * TarifRequest request = new TarifRequest();<br>
 * request.setPerson(new Person());<br>
 * request.setAccount(new Account());<br>
 * request.getPerson().setAge(35);<br>
 * request.getAccount().setAgeInMonths(25);<br>
 * </code>
 * <br>
 * The following code can then be used to determine the best tarif, or list of all matching tarifs:<br>
 * <br>
 * <code>
 * <br>
 * //will return both DT2011 and LT2011, since both are relevant.<br>
 * //Since LT2011 has a higher priority, it is first in the list.<br>
 * List&lt;Rule&gt; matches = engine.getMatchingRules(request);<br>
 * <br>
 * // returns "LT2011", since the loyalty tarif has a higher priority<br>
 * String tarif = engine.runBestAction(request, String.class); <br>
 * </code>
 * <br>
 * See {@link Rule} for more details.  See <code>EngineTest</code> for more examples.  
 * See <a href='https://github.com/mvel/mvel'>https://github.com/mvel/mvel</a> and
 * See <a href='http://mvel.documentnode.com/'>http://mvel.documentnode.com/</a> for full details of the expression language.
 */
public class Engine {

    //https://github.com/mvel/mvel/issues/123
    public static final String DEFAULT_ILLEGAL_WORDS = "java," + //e.g. access to java.io.xyz
            "System," + //e.g. access to System.exit(1);
            "Runtime," + //e.g. access to Runtime.getRuntime.exec("rm -rf")
            "InitialContext," + // e.g. for accessing the CDI Bean Manager or TX Manager or some EJB (Service)
            "new ," + //e.g. to instantiate classes - space afterwards so that its less critical
            "getBean" //e.g. using spring context in order to call a service
        ;

	/** the name which scripts should use for the input, unless overriden in the constructor/builder. */
	public static final String DEFAULT_INPUT_NAME = "input";

	private static final Logger log = Logger.getLogger(Engine.class.getName());

    /** static variable bindings to be used in addition to the input when executing rules */
    protected final Map<String, Object> statics;

    private List<CompiledRule> rules;
	protected final Set<String> uniqueOutcomes = new HashSet<String>();
	protected List<Rule> parsedRules;

	protected final boolean throwExceptionIfCompilationFails;
	protected final String inputName;
	
	//reserved for subclasses and not used in this class - yuck, but hey.
	protected final String[] javascriptFilesToLoad;
	protected final Integer poolSize;

	/**
	 * @param rules The rules which define the system.
	 * @param throwExceptionIfCompilationFails if true, and a rule cannot be compiled, then a {@link CompileException} will be thrown.
	 * @throws DuplicateNameException thrown if any rules have the same name within a namespace
	 * @throws CompileException thrown if throwExceptionIfCompilationFails is true, and a rule fails to compile, because its expression is invalid
	 * @throws ParseException Thrown if a subrule which is referenced in a rule cannot be resolved.
	 */
	public Engine(final Collection<Rule> rules, boolean throwExceptionIfCompilationFails) throws DuplicateNameException, CompileException, ParseException {
		this(rules, DEFAULT_INPUT_NAME, throwExceptionIfCompilationFails);
	}

	/**
	 * See {@link #Engine(Collection, boolean)}
	 * @param inputName the name of the input in scripts, normally "input", but you can specify your own name here.
	 */
	public Engine(final Collection<Rule> rules, String inputName, boolean throwExceptionIfCompilationFails) throws DuplicateNameException, CompileException, ParseException {
		this(rules, inputName, throwExceptionIfCompilationFails, null, null, new HashMap<String, Object>());
	}

    /**
	 * See {@link #Engine(Collection, String, boolean, Map)}
     */
	public Engine(final Collection<Rule> rules, boolean throwExceptionIfCompilationFails, Map<String, Object> statics) throws DuplicateNameException, CompileException, ParseException {
	    this(rules, DEFAULT_INPUT_NAME, throwExceptionIfCompilationFails, null, null, statics);
    }

    /**
	 * See {@link #Engine(Collection, boolean)}
     * <br><br>
     * Allows you to define constants or static methods which rules can refer to.
     * <code>
     * Map<String, Object> statics = new HashMap<String, Object>();
     * statics.put("someString", MVEL.getStaticMethod(this.getClass(), "getSomeString", new Class[0]));
     * Rule rule1 = new Rule("1", "input.name == someString()", "ok", 1, "ch.maxant.demo");
     * </code>
     *
     * @param statics a map containing variable bindings which do not change, e.g. constants or static methods (functions).
     */
	public Engine(final Collection<Rule> rules, String inputName, boolean throwExceptionIfCompilationFails, Map<String, Object > statics) throws DuplicateNameException, CompileException, ParseException {
	    this(rules, inputName, throwExceptionIfCompilationFails, null, null, statics);
    }

	protected Engine(final Collection<Rule> rules, String inputName, boolean throwExceptionIfCompilationFails, Integer poolSize, String[] javascriptFilesToLoad, Map<String, Object > statics) throws DuplicateNameException, CompileException, ParseException {
		this.inputName = inputName;
		this.throwExceptionIfCompilationFails = throwExceptionIfCompilationFails;
		this.javascriptFilesToLoad = javascriptFilesToLoad;
		this.poolSize = poolSize;
		this.statics = statics;
		init(rules);
	}
	
	/** handles the initialisation */
	protected void init(Collection<Rule> rules) throws DuplicateNameException, CompileException, ParseException {
		log.info("\r\n\r\n*****Initialising rule engine...*****");

        Set<String> illegalWordSet = initIllegalWords();

		this.rules = new ArrayList<CompiledRule>();
		long start = System.currentTimeMillis();
		Map<String, Rule> names = new HashMap<String, Rule>();
		for(Rule r : rules){
            verifyLegal(r, illegalWordSet);

			String fullyQualifiedName = r.getFullyQualifiedName();
			if(names.containsKey(fullyQualifiedName)){
				throw new DuplicateNameException("The name " + fullyQualifiedName + " was found in a different rule.");
			}
			names.put(r.getFullyQualifiedName(), r);
		    uniqueOutcomes.add(r.getOutcome());
		}
		
		parsedRules = new ArrayList<Rule>();
		
		//now replace all rule references with the actual rule, contained within brackets
		while(true){
			boolean foundRuleReference = false;
			for(Rule r : rules){
				int idx1 = r.getExpression().indexOf('#');
                if(idx1 > -1){
					foundRuleReference = true;

					//search to end of expression for next symbol
					int idx2 = idx1 + 1; //to skip #
					while(true){
						idx2++;
						if(idx2 >= r.getExpression().length()){
							break;
						}
						char c = r.getExpression().charAt(idx2);
						if(
								c == ' ' || c == '&' || 
								c == '|' || c == '.' || 
								c == '(' || c == ')' || 
								c == '[' || c == ']' || 
								c == '{' || c == '}' || 
								c == '+' || c == '-' || 
								c == '/' || c == '*' || 
								c == '=' || c == '!'
						){
							//end of token
							break;
						}
					}
					
					String token = r.getExpression().substring(idx1+1, idx2);
					String fullyQualifiedRuleRef = r.getNamespace() + "." + token;
					Rule toAdd = names.get(fullyQualifiedRuleRef);
					if(toAdd == null){
						throw new ParseException("Error while attempting to add subrule to rule " + r.getFullyQualifiedName() + ".  Unable to replace #" + token + " with subrule " + fullyQualifiedRuleRef + " because no subrule with that fully qualified name was found");
					}
					String newExpression = "";
					if(idx1 > 0){
						newExpression += r.getExpression().substring(0, idx1);
					}
					newExpression += "(" + toAdd.getExpression() + ")";
					if(idx2 < r.getExpression().length()){
						newExpression += r.getExpression().substring(idx2);
					}
					if(r instanceof SubRule){
					    parsedRules.add(new SubRule(r.getName(), newExpression, r.getNamespace(), r.getDescription()));
					}else{
					    parsedRules.add(new Rule(r.getName(), newExpression, r.getOutcome(), r.getPriority(), r.getNamespace(), r.getDescription()));
					}
				}else{
				    parsedRules.add(r);
				}
			}
			if(!foundRuleReference){
				//yay, all done!
				break;
			}else{
			    //go thru again, because there are still rules which need substituting
			    rules = parsedRules;
			    parsedRules = new ArrayList<Rule>();
			}
		}
		
		compile();

		log.info("*****Engine initialisation completed in " + (System.currentTimeMillis()-start) + " ms*****\r\n");
	}

	/** override this if you want to change the illegal words that are checked. only rules without these words may be used. */
	protected Set<String> initIllegalWords() {
        Set<String> illegalWords = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(DEFAULT_ILLEGAL_WORDS, ",");
        while (st.hasMoreTokens()) {
            illegalWords.add(st.nextToken());
        }
        return illegalWords;
    }

    /** override this if you want to change how rules are verified against illegal words, e.g. to make the check case insensitive. */
    protected void verifyLegal(Rule r, Set<String> illegalWords) {
        for (String illegalWord : illegalWords) {
            if (r.getExpression().contains(illegalWord)) {
                throw new IllegalArgumentException(
                        "Rule has an illegal word! None of the following words may be contained in rules: " + illegalWords + ". Alternatively override Engine#initIllegalWords or Engine#verifyLegal.");
            }
        }
    }


    protected void compile() throws CompileException {
		for(Rule r : parsedRules){
			if(r instanceof SubRule){
				continue;
			}
			addCompiledRule(throwExceptionIfCompilationFails, r);
		}
	}

	private void addCompiledRule(boolean throwExceptionIfCompilationFails, Rule r) throws CompileException {
		try{
			this.rules.add(new CompiledRule(r));
			log.info("added rule: " + r);
		}catch(org.mvel2.CompileException ex){
			log.warning("Failed to compile " + r.getFullyQualifiedName() + ": " + ex.getMessage());
			if(throwExceptionIfCompilationFails){
				throw new CompileException(ex.getMessage());
			}
		}
	}

	/**
	 * See {@link #getBestOutcome(String, Object)}, except that all namespaces will be considered.
	 * @param <Input> An input object to match against rules.
	 */
	public <Input> String getBestOutcome(Input input) throws NoMatchingRuleFoundException {
		return getBestOutcome(null, input);
	}

	/**
	 * Evaluates all rules against the input and returns the result of the outcome associated with the rule having the highest priority.
	 * @param <Input> An input object to match against rules.
	 * @param nameSpacePattern optional.  if not null, then only rules with matching namespaces are evaluated.
	 * @param input the Object containing all inputs to the expression language rule.
	 * @return The outcome belonging to the best rule which is found.
	 * @throws NoMatchingRuleFoundException If no matching rule was found.  Rules must evaluate to true in order to be candidates.
	 */
	public <Input> String getBestOutcome(String nameSpacePattern, Input input) throws NoMatchingRuleFoundException {

		List<Rule> matches = getMatchingRules(nameSpacePattern, input);
		if(matches == null || matches.isEmpty()){
			throw new NoMatchingRuleFoundException();
		}else{
			return matches.get(0).getOutcome();
		}
	}
	
	/**
	 * See {@link #executeBestAction(String, Object, Collection)}, except that all namespaces will be considered.
	 */
	public <Input, Output> Output executeBestAction(Input input, Collection<? extends IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
		return executeBestAction(null, input, actions);
	}

	/**
	 * Evaluates all rules against the input and returns the result of the action associated with the rule having the highest priority.
	 * @param nameSpacePattern optional.  if not null, then only rules with matching namespaces are evaluated.
	 * @param input the Object containing all inputs to the expression language rule.
	 * @param actions a collection of actions containing one action per possible outcome.  The action whose name is equal to the winning outcome will be executed.
	 * @return The result of the {@link IAction} with the same name as the winning rules outcome.
	 * @throws NoMatchingRuleFoundException If no matching rule was found.  Rules must evaluate to true in order to be candidates.
	 * @throws NoActionFoundException If no action with a name matching the winning rules outcome was found.
	 * @throws DuplicateNameException if any actions have the same name.
	 */
	public <Input, Output> Output executeBestAction(String nameSpacePattern, Input input, Collection<? extends IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
		
		Map<String, IAction<Input, Output>> actionsMap = validateActions(actions);
		
		return actionsMap.get(getBestOutcome(nameSpacePattern, input)).execute(input);
	}
	
	/**
	 * See {@link #executeAllActions(String, Object, Collection)}, except that all namespaces will be considered.
	 * <b>NOTE THAT THIS METHOD DISREGARDS ANY RETURN VALUES OF ACTIONS!!</b>
	 */
	public <Input, Output> void executeAllActions(Input input, Collection<? extends IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
		executeAllActions(null, input, actions);
	}
	
	/**
	 * Evaluates all rules against the input and then executes all action associated with the positive rules outcomes, in order of highest priority first.<br>
	 * <br>
	 * Any outcome is only ever executed once!<br>
	 * <br>
	 * <b>NOTE THAT THIS METHOD DISREGARDS ANY RETURN VALUES OF ACTIONS!!</b>
	 * @param <Input> The type of input to the actions.
	 * @param <Output> The type of output from the actions.
	 * @param nameSpacePattern optional.  if not null, then only rules with matching namespaces are evaluated.
	 * @param input the Object containing all inputs to the expression language rule.
	 * @param actions a collection of actions containing one action per possible outcome.  The actions whose names is equal to the positive outcomes will be executed.
	 * @throws NoActionFoundException If no action with a name matching the winning rules outcome was found.
	 * @throws DuplicateNameException if any actions have the same name.
	 */
	public <Input, Output> void executeAllActions(String nameSpacePattern, Input input, Collection<? extends IAction<Input, Output>> actions) throws NoActionFoundException, DuplicateNameException {
		
		Map<String, IAction<Input, Output>> actionsMap = validateActions(actions);
		
		List<Rule> matchingRules = getMatchingRules(nameSpacePattern, input);
		
		Set<String> executedOutcomes = new HashSet<String>();
		for(Rule r : matchingRules){
			//only run, if not already run!
			if(!executedOutcomes.contains(r.getOutcome())){
				
				actionsMap.get(r.getOutcome()).execute(input);
				
				executedOutcomes.add(r.getOutcome());
			}
		}
		
	
	}
	
	private <Input, Output> Map<String, IAction<Input, Output>> validateActions(Collection<? extends IAction<Input, Output>> actions) throws DuplicateNameException, NoActionFoundException{
		//do any actions have duplicate names?
		Map<String, IAction<Input, Output>> actionsMap = new HashMap<String, IAction<Input, Output>>();
		for(IAction<Input, Output> a : actions){
			if(actionsMap.containsKey(a.getName())){
				throw new DuplicateNameException("The name " + a.getName() + " was found in a different action.  Action names must be unique.");
			}else{
				actionsMap.put(a.getName(), a);
			}
		}
		
		//do we have at least one action for every possible outcome?  
		//better to test now, rather than in production...
		//n.b. subrules have outcome == null, so skip them
		for(String outcome : uniqueOutcomes){
			if(outcome != null && !actionsMap.containsKey(outcome)){
				throw new NoActionFoundException("No action has been associated with the outcome \"" + outcome + "\"");
			}
		}
		
		return actionsMap;
	}
	
	/**
	 * See {@link #getMatchingRules(String, Object)}, except that all namespaces will be considered.
	 */
	public <Input> List<Rule> getMatchingRules(Input input) {
		return getMatchingRules(null, input);
	}
		
	/**
	 * @param <Input> the type of input
	 * @param nameSpacePattern optional.  if not null, then only rules with matching namespaces are evaluated.
	 * @param input the Object containing all inputs to the expression language rule.
	 * @return an ordered list of Rules which evaluated to "true", sorted by {@link Rule#getPriority()}, with the highest priority rules first in the list.
	 */
	public <Input> List<Rule> getMatchingRules(String nameSpacePattern, Input input) {
		
		Pattern pattern = null;
		if(nameSpacePattern != null){
			pattern = Pattern.compile(nameSpacePattern);
		}
		
		Map<String, Object> vars = new HashMap<String, Object>(statics); // initialise with static stuff
		vars.put(inputName, input);

		List<Rule> matchingRules = new ArrayList<Rule>();
		for(CompiledRule r : rules){
			
			if(pattern != null){
				if(!pattern.matcher(r.getRule().getNamespace()).matches()){
					continue;
				}
			}
			
			Object o = MVEL.executeExpression(r.getCompiled(), vars);
			String msg = r.getRule().getFullyQualifiedName() + "-{" + r.getRule().getExpression() + "}";
			if(String.valueOf(o).equals("true")){
				matchingRules.add(r.getRule());
                if(log.isLoggable(Level.INFO)) log.info("matched: " + msg);
			}else{
                if(log.isLoggable(Level.INFO)) log.info("unmatched: " + msg);
			}
		}
		
		//order by priority!
		Collections.sort(matchingRules);
		
		return matchingRules;
	}
	
	private static final class CompiledRule {
		private Rule rule;
		private Serializable compiled;
		private CompiledRule(Rule rule) {
			this.rule = rule;
			this.compiled = MVEL.compileExpression(rule.getExpression());
		}
		private Serializable getCompiled() {
			return compiled;
		}
		private Rule getRule() {
			return rule;
		}
	}

}

		