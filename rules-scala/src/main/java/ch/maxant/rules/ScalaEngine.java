package ch.maxant.rules;

/**
 * This class supports Scala collections being used with the engine.
 * 
 * @see <code>Engine</code> for more details.  
 */
public class ScalaEngine extends Engine {

	/** See <code>#Engine(java.util.Collection, boolean).</code>  Supports Scala collections. */
	public ScalaEngine(final scala.collection.Iterable<Rule> rules, boolean throwExceptionIfCompilationFails) throws DuplicateNameException, CompileException, ParseException {
	    super(scala.collection.JavaConversions.asJavaCollection(rules), throwExceptionIfCompilationFails);
	}

	/** See <code>#executeAllActions(String, Object, java.util.Collection)</code>, supports Scala collections. */
    public <Input, Output> void executeAllActions(String nameSpacePattern, Input input, scala.collection.Iterable<AbstractAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
        executeAllActions(nameSpacePattern, input, scala.collection.JavaConversions.asJavaCollection(actions));
    }
    
    /** See <code>#executeAllActions(Object, java.util.Collection)</code>, supports Scala collections. */
    public <Input, Output> void executeAllActions(Input input, scala.collection.Iterable<AbstractAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	executeAllActions(null, input, actions);
    }
    
    /** See <code>#executeBestAction(Object, java.util.Collection)</code>, supports Scala collections. */
    public <Input, Output> Output executeBestAction(Input input, scala.collection.Iterable<AbstractAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	return executeBestAction(null, input, actions);
    }
    
    /** See <code>#executeBestAction(String, Object, java.util.Collection)</code>, supports Scala collections. */
    public <Input, Output> Output executeBestAction(final String namespace, final Input input, final scala.collection.Iterable<AbstractAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	return executeBestAction(namespace, input, scala.collection.JavaConversions.asJavaCollection(actions));
    }
    
}

		