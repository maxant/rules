package ch.maxant.rules.blackbox;

import ch.maxant.rules.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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
