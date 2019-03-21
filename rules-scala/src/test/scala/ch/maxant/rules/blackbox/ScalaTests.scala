package ch.maxant.rules.blackbox

import org.junit.Test
import org.junit.Assert._
import ch.maxant.rules.Engine
import ch.maxant.rules.Rule
import ch.maxant.rules.Action
import java.math.BigDecimal
import ch.maxant.rules.blackbox.AbstractEngineTest.MyInput
import ch.maxant.rules.blackbox.AbstractEngineTest.Person
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
