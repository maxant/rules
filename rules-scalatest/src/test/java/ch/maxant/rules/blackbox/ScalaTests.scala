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

import org.junit.Test
import org.junit.Assert._
import ch.maxant.rules.Engine
import ch.maxant.rules.Rule
import ch.maxant.rules.Action
import java.math.BigDecimal
import ch.maxant.rules.blackbox.EngineTest.MyInput
import ch.maxant.rules.blackbox.EngineTest.Person
import ch.maxant.rules.ScalaEngine

class ScalaTests {

    @Test
    def testActions {

        val rule1 = new Rule("R1", """input.p1.name == "ant" && input.p2.name == "clare" """, "outcome1", 1, "ch.maxant.produkte", "Spezi Regel für Familie Kutschera")
        val rule2 = new Rule("R2", "true", "outcome2", 0, "ch.maxant.produkte", "Default Regel")
        val rules = List(rule1, rule2)

        def f(i: MyInput) = new BigDecimal("100.0")
        
        val action1 = new Action("outcome1")(f)

        val action2 = new Action("outcome2")( (i: MyInput) =>
            new BigDecimal("101.0")
        )
        
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
}