/*
 * Copyright (c) 2011-2017 Ant Kutschera
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
package ch.maxant.rules;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * This class supports Java 8 {@link Stream}s being used with the engine.
 * 
 * <code>
		//to use a lambda, construct a SamAction and pass it a lambda.
		IAction<MyInput, BigDecimal> action1 = new SamAction<MyInput, BigDecimal>("outcome1", i -> new BigDecimal("100.0"));
		IAction<MyInput, BigDecimal> action2 = new SamAction<MyInput, BigDecimal>("outcome2", i -> new BigDecimal("101.0"));

		List<IAction<MyInput, BigDecimal>> actions = Arrays.asList(action1, action2);
		
		Engine e = new Engine(rules, true);
 * </code>
 * 
 * See <code>Engine</code> for more details.  
 */
public class Java8JavascriptEngine extends JavascriptEngine {

	/** See <code>#JavascriptEngine(java.util.Collection, boolean, String...)</code>.  Convenience constructor for use with Java 8 {@link Stream}s.  
	 * Simply collects all elements from the given {@link Stream}. 
	 */
	public Java8JavascriptEngine(final Stream<Rule> rules, boolean throwExceptionIfCompilationFails, String... javascriptFilesToLoad) throws DuplicateNameException, CompileException, ParseException {
	    super(rules.collect(Collectors.toList()), throwExceptionIfCompilationFails, javascriptFilesToLoad);
	}
	
	/** See <code>#JavascriptEngine(java.util.Collection, boolean, String...)</code>.  Convenience constructor for use with Java 8 {@link Stream}s.  
	 * Simply collects all elements from the given {@link Stream}. 
	 */
	public Java8JavascriptEngine(final Collection<Rule> rules, boolean throwExceptionIfCompilationFails, String... javascriptFilesToLoad) throws DuplicateNameException, CompileException, ParseException {
		super(rules, throwExceptionIfCompilationFails, javascriptFilesToLoad);
	}
	
	/** See <code>#JavascriptEngine(java.util.Collection, String, boolean, Integer, boolean, String...)</code>.  Convenience constructor for use with Java 8 {@link Stream}s.  
	 */
	public Java8JavascriptEngine(final Stream<Rule> rules, String inputName, boolean throwExceptionIfCompilationFails, Integer poolSize, boolean preloadPool, String... javascriptFilesToLoad) throws DuplicateNameException, CompileException, ParseException {
		super(rules.collect(Collectors.toList()), inputName, throwExceptionIfCompilationFails, poolSize, preloadPool, javascriptFilesToLoad);
	}
	
	/** See <code>#JavascriptEngine(java.util.Collection, String, boolean, Integer, boolean, String...)</code>.  Convenience constructor for use with Java 8 {@link Stream}s.  
	 */
	public Java8JavascriptEngine(final Collection<Rule> rules, String inputName, boolean throwExceptionIfCompilationFails, Integer poolSize, boolean preloadPool, String... javascriptFilesToLoad) throws DuplicateNameException, CompileException, ParseException {
		super(rules, inputName, throwExceptionIfCompilationFails, poolSize, preloadPool, javascriptFilesToLoad);
	}
	
	/** See <code>#executeBestAction(Object, java.util.Collection)</code>, supports {@link Stream}s. */
    public <Input, Output> Output executeBestAction(Input input, Stream<IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
        return executeBestAction(null, input, actions);
    }
    
    /** See <code>#executeBestAction(String nameSpacePattern, Object, java.util.Collection)</code>, supports {@link Stream}s. 
     */
    public <Input, Output> Output executeBestAction(String nameSpacePattern, Input input, Stream<IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	return executeBestAction(nameSpacePattern, input, actions.collect(Collectors.toList()));
    }
    
    /** See <code>#executeAllActions(Object, java.util.Collection)</code>, supports {@link Stream}s. */
    public <Input, Output> void executeAllActions(Input input, Stream<IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	executeAllActions(null, input, actions);
    }
    
    /** See <code>#executeAllActions(String nameSpacePattern, Object, java.util.Collection)</code>, supports {@link Stream}s. */
    public <Input, Output> void executeAllActions(String nameSpacePattern, Input input, Stream<IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	executeAllActions(nameSpacePattern, input, actions.collect(Collectors.toList()));
    }


	public static final class Builder {
		
		private final Collection<Rule> rules;
		private String inputName = JavascriptEngine.DEFAULT_INPUT_NAME;
		private boolean throwExceptionIfCompilationFails = true;
		private Integer poolSize = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;
		private boolean preloadPool = false;
		private String[] javascriptFilesToLoad = {};

		public Builder(Collection<Rule> rules){
			this.rules = rules;
		}

		public Builder(Stream<Rule> rules){
			this.rules = rules.collect(Collectors.toList());
		}
		
		public Builder withInputName(String inputName){
			this.inputName = inputName;
			return this;
		}
		
		public Builder withThrowExceptionIfCompilationFails(boolean throwExceptionIfCompilationFails){
			this.throwExceptionIfCompilationFails = throwExceptionIfCompilationFails;
			return this;
		}
		
		public Builder withPoolSize(Integer poolSize){
			this.poolSize = poolSize;
			return this;
		}
		
		public Builder withPreloadPool(boolean preloadPool){
			this.preloadPool = preloadPool;
			return this;
		}
	
		public Builder withJavascriptFilesToLoad(String... javascriptFilesToLoad) {
			this.javascriptFilesToLoad = javascriptFilesToLoad;
			return this;
		}
		
		public JavascriptEngine build() throws DuplicateNameException, CompileException, ParseException {
			return new Java8JavascriptEngine(rules, inputName, throwExceptionIfCompilationFails, poolSize, preloadPool, javascriptFilesToLoad);
		}
	}
    
}