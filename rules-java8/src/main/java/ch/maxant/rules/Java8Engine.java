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
public class Java8Engine extends Engine {

	/** See <code>#Engine(java.util.Collection, boolean)</code>.  Convenience constructor for use with Java 8 {@link Stream}s.  
	 * Simply collects all elements from the given {@link Stream}. 
	 */
	public Java8Engine(final Stream<Rule> rules, boolean throwExceptionIfCompilationFails) throws DuplicateNameException, CompileException, ParseException {
	    super(rules.collect(Collectors.toList()), throwExceptionIfCompilationFails);
	}

	/** See <code>#Engine(java.util.Collection, boolean)</code>.  Convenience constructor for use with Java 8 {@link Stream}s.  
	 * Simply collects all elements from the given {@link Stream}. 
	 */
	public Java8Engine(final Collection<Rule> rules, boolean throwExceptionIfCompilationFails) throws DuplicateNameException, CompileException, ParseException {
		super(rules, throwExceptionIfCompilationFails);
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
    
}

		