/*
 * Copyright (c) 2014 Ant Kutschera
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

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class supports Java 8 {@link Stream}s being used with the engine.
 * 
 * @see {@link Engine} for more details.  
 */
public class Java8Engine extends Engine {

	/** @see Engine#Engine(java.util.Collection, boolean).  Convenience constructor for use with Java 8 {@link Stream}s.  
	 * Simply collects all elements from the given {@link Stream}. */
	public Java8Engine(final Stream<Rule> rules, boolean throwExceptionIfCompilationFails) throws DuplicateNameException, CompileException, ParseException {
	    super(rules.collect(Collectors.toList()), throwExceptionIfCompilationFails);
	}

	/** @see #executeBestAction(Object, java.util.Collection), supports {@link Stream}s. */
    public <Input, Output> Output executeBestAction(Input input, Stream<IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
        return executeBestAction(null, input, actions);
    }
    
    /** @see #executeBestAction(String nameSpacePattern, Object, java.util.Collection), supports {@link Stream}s. */
    public <Input, Output> Output executeBestAction(String nameSpacePattern, Input input, Stream<IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	return executeBestAction(nameSpacePattern, input, actions.collect(Collectors.toList()));
    }
    
    /** @see #executeAllActions(Object, java.util.Collection), supports {@link Stream}s. */
    public <Input, Output> void executeAllActions(Input input, Stream<IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	executeAllActions(null, input, actions);
    }
    
    /** @see #executeAllActions(String nameSpacePattern, Object, java.util.Collection), supports {@link Stream}s. */
    public <Input, Output> void executeAllActions(String nameSpacePattern, Input input, Stream<IAction<Input, Output>> actions) throws NoMatchingRuleFoundException, NoActionFoundException, DuplicateNameException {
    	executeAllActions(nameSpacePattern, input, actions.collect(Collectors.toList()));
    }
    
}

		