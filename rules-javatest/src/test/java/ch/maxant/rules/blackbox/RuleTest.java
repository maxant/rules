/*
 * Copyright (c) 2011 Ant Kutschera
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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.maxant.rules.Rule;

public class RuleTest {

    @Test
    public void testRuleValidation(){
        boolean success = false;
        try{
            new Rule(null, "expression", "outcome", 1, "namespace", "description");
            success = true;
        }catch(AssertionError e){
            //expected
        }catch(Exception e){
            fail("wrong type of exception");
        }
        assertFalse(success);

        success = false;
        try{
            new Rule("name", null, "outcome", 1, "namespace", "description");
            success = true;
        }catch(AssertionError e){
            //expected
        }catch(Exception e){
            fail("wrong type of exception");
        }
        assertFalse(success);

        success = false;
        try{
            new Rule("name", "expression", "outcome", 1, null, "description");
            success = true;
        }catch(AssertionError e){
            //expected
        }catch(Exception e){
            fail("wrong type of exception");
        }
        assertFalse(success);
    }
    
	@Test
	public void testAll() {
		Rule r01 = new Rule("name", "expression", "outcome", 1, "namespace", "description");
		Rule r02 = new Rule("name", "expression", "outcome", 1, "namespace", "description");
        Rule r03 = new Rule("name", "expression", null, 1, "namespace", "description");
		Rule r04 = new Rule("name2", "expression", "outcome", 1, "namespace", "description");
		Rule r06 = new Rule("name", "expression2", "outcome", 1, "namespace", "description");
		Rule r08 = new Rule("name", "expression", "outcome2", 1, "namespace", "description");
		Rule r10 = new Rule("name", "expression", "outcome", 1, "namespace2", "description");
		Rule r11 = new Rule("name", "expression", "outcome", 2, "namespace", "descriptor");
		Rule r12 = new Rule("name", "expression", "outcome", -1, "namespace", "description");
		
		assertEquals("name", r01.getName());
		assertEquals("expression", r01.getExpression());
		assertEquals("outcome", r01.getOutcome());
		assertEquals(1, r01.getPriority());
		assertEquals("namespace", r01.getNamespace());
		assertEquals("description", r01.getDescription());
		assertEquals("Rule [name=name, expression=expression, outcome=outcome, priority=1, namespace=namespace, description=description]", r01.toString());
		assertEquals(r01, r02);
		assertEquals(r01.hashCode(), r02.hashCode());
		
		assertTrue(!r01.equals(null));
		assertTrue(!r01.equals(new Integer(1)));
		
		List<Rule> rules = Arrays.asList(/*no r01 since its equal to r02*/r02, r03, r04, r06, r08, r10, r11, r12);
		for(Rule r1 : rules){
			for(Rule r2 : rules){
				if(r1 == r2) {
					assertTrue("eq" + r1 + "/" + r2, r1.equals(r2));
					assertTrue("eq" + r2 + "/" + r1, r2.equals(r1));
					assertTrue("ha" + r1 + "/" + r2, r1.hashCode() == r2.hashCode());
				}else{
					assertTrue(r1 + "/" + r2, !r1.equals(r2));
					assertTrue(r2 + "/" + r1, !r2.equals(r1));
					assertTrue("ha" + r1 + "/" + r2, r1.hashCode() != r2.hashCode());
				}
			}
		}
	}
	
}
