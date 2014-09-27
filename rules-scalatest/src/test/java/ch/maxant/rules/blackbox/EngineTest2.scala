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
package ch.maxant.rules.blackbox

import org.junit.Assert._
import java.math.BigDecimal
import org.junit.Test
import ch.maxant.rules._
import scala.collection.immutable.List
import ch.maxant.rules.blackbox.EngineTest.MyInput
import ch.maxant.rules.blackbox.EngineTest.Person
import ch.maxant.rules.blackbox.EngineTest.TravelRequest
import java.util.Arrays

class EngineTest2 {
    /**
     * tests that when two rules exist, the rule with the highest priority is run.
     * @throws ParseException 
     * @throws CompileException 
     */
    @Test
    def test1() {
        val rule1 = new Rule("R1", """input.p1.name == "ant" && input.p2.name == "clare" """, "outcome1", 1, "ch.maxant.produkte", "Spezi Regel für Familie Kutschera")
        val rule2 = new Rule("R2", "true", "outcome2", 0, "ch.maxant.produkte", "Default Regel")
        val rules = List(rule1, rule2)

        val action1 = new Action("outcome1") ( (input: MyInput) =>
            new BigDecimal("100.0")
        )

        val action2 = new AbstractAction[EngineTest.MyInput, BigDecimal]("outcome2") {
            def execute(input: MyInput) = {
                new BigDecimal("101.0")
            }
        }

        val actions = List(action1, action2)

        val e = new ScalaEngine(rules, true)
        
        val input = new MyInput()
        val p1 = new Person("ant")
        val p2 = new Person("clare")
        input.setP1(p1)
        input.setP2(p2)
        
        try {
            val price = e.executeBestAction(input, actions)
            assertEquals(new BigDecimal("100.0"), price)
        } catch {
            case ex: Exception => fail(ex.getMessage)
        }
    }

    @Test
    def testRulesInRules1(){

        val rule1 = new SubRule("1", "input.distance > 100", "ch.maxant.produkte")
        val rule2 = new SubRule("2", "input.map[\"travelClass\"] == 1", "ch.maxant.produkte")
        val rule3 = new SubRule("3", "true", "ch.maxant.produkte")
        val rule4 = new Rule("4", "#1 && #3 && #2", "SomeCommand", 3, "ch.maxant.produkte")
        val rules = List(rule1, rule2, rule3, rule4)

        try{
            val e = new ScalaEngine(rules, true)
            
            val request = new TravelRequest(150)
            request.put("travelClass", 1)
            val rs = e.getMatchingRules(null,  request)
            
            assertEquals(1, rs.size)
            assertEquals(rule4.getName(), rs.get(0).getName())
        }catch{
            case ex: Exception => fail(ex.getMessage)
        }
    }

    @Test
    def testRulesInRules2(){
        val rule1 = new SubRule("1", "input.distance > 100", "ch.maxant.produkte")
        val rule2 = new Rule("2", "#1", "SomeCommand", 3, "ch.maxant.produkte")
        val rules = Arrays.asList(rule1, rule2)

        try{
            val e = new Engine(rules, true)
            
            val request = new TravelRequest(150)
            val rs = e.getMatchingRules(null,  request)
            
            assertEquals(1, rs.size());
            assertEquals(rule2.getName(), rs.get(0).getName())
        }catch{
            case ex: Exception => fail(ex.getMessage)
        }
    }
    
    @Test
    def testRulesInRules3(){
        val rule1 = new SubRule("1", "input.distance > 100", "ch.maxant.produkte")
        val rule2 = new Rule("2", "!#1", "SomeCommand", 3, "ch.maxant.produkte")
        val rules = Arrays.asList(rule1, rule2)

        try{
            val e = new Engine(rules, true)
            
            val request = new TravelRequest(150)
            val rs = e.getMatchingRules(null,  request)
            
            assertEquals(0, rs.size)
        }catch{
            case ex: Exception => fail(ex.getMessage)
        }
    }
    
    @Test
    def testSubRuleInDifferentNamespace(){
        val rule1 = new SubRule("1", "input.distance > 100", "ch.maxant.fahrplan")
        val rule2 = new Rule("2", "#1", "SomeCommand", 3, "ch.maxant.produkte")
        val rules = Arrays.asList(rule1, rule2)
        
        try{
            new Engine(rules, true)
            fail("exception wasnt thrown..")
        }catch{
            case ex: Exception => {
                assertTrue(ex.isInstanceOf[ParseException])
                assertEquals("Error while attempting to add subrule to rule ch.maxant.produkte.2.  Unable to replace #1 with subrule ch.maxant.produkte.1 because no subrule with that fully qualified name was found", ex.getMessage)
            }
        }
    }
    
    @Test
    def testDuplicateNames(){
        val rule1 = new SubRule("1", "input.distance > 100", "ch.maxant.produkte")
        val rule2 = new Rule("1", "#1", "SomeCommand", 3, "ch.maxant.produkte")
        val rules = Arrays.asList(rule1, rule2)
        
        try{
            new Engine(rules, true)
            fail("exception wasnt thrown..")
        }catch{
            case ex: Exception => {
                assertTrue(ex.isInstanceOf[DuplicateNameException])
                assertEquals("The name ch.maxant.produkte.1 was found in a different rule.", ex.getMessage)
            }
        }
    }
    
    
    
}