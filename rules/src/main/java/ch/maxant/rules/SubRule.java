/*
 * Copyright (c) 2011-2018 Ant Kutschera
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

/**
 * A sub rule can be used to make rules which are built up from partial expressions and expressions 
 * contained in (sub)rules.  A sub rule will
 * <b>never</b> have an associated action executed, rather it will only ever be evaluated, as part of the evaluation
 * of a rule which references it.  Rules may reference sub rules within the same namespace, using the '#'
 * character.  E.g.:<br>
 * <br>
 * SubRule 1: input.age &gt; 30<br>
 * SubRule 2: input.height &lt; 100<br>
 * Rule 3: #1 &amp;&amp; !#2 <br>
 */
public class SubRule extends Rule {

	/**
	 * @param name The name of the rule.  Should be unique within the namespace (checked when adding rules to the engine).
	 * @param expression the rule, expressed in expression language. all variables must come from the bean called "input".  The rule MUST evaluate to "true" if it is to be a candidate for execution.
	 * @param namespace For a sub rule to be used by a rule, it must have the same namespace as the rule.
	 * @param description A description to help manage rules.
	 */
    public SubRule(String name, String expression, String namespace, String description) {
        super(name, expression, null, -1, namespace, description);
    }

    /**
     * See {@link #SubRule(String, String, String, String)}, just without a description.
     */
    public SubRule(String name, String expression, String namespace){
        this(name, expression, namespace, null);
    }
    
}
