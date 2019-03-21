package ch.maxant.rules.blackbox;

import ch.maxant.rules.AbstractAction;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ActionTest {

	@Test
	public void testAll() {
		MyAction a = new MyAction("one");
		MyAction b = new MyAction("one");
		MyAction c = new MyAction("two");
		MyAction d = new MyAction(null);
		
		assertTrue(a.equals(b));
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a.getName(), b.getName());
		assertEquals("Action [name=one]", a.toString());

		assertTrue(!a.equals(null));
		assertTrue(a.equals(a));
		assertTrue(!a.equals(new Integer(1)));
		assertTrue(!a.equals(c));

		assertTrue(!d.equals(c));
		assertTrue(!c.equals(d));
		
		
		List<MyAction> actions = Arrays.asList(b, c, d);
		for(MyAction a1 : actions){
			for(MyAction a2 : actions){
				if(a1 == a2) {
					assertTrue("eq" + a1 + "/" + a2, a1.equals(a2));
					assertTrue("eq" + a2 + "/" + a1, a2.equals(a1));
					assertTrue("ha" + a1 + "/" + a2, a1.hashCode() == a2.hashCode());
				}else{
					assertTrue(a1 + "/" + a2, !a1.equals(a2));
					assertTrue(a2 + "/" + a1, !a2.equals(a1));
					assertTrue("ha" + a1 + "/" + a2, a1.hashCode() != a2.hashCode());
				}
			}
		}
		
	}
	
	private static final class MyAction extends AbstractAction<String, String> {
		public MyAction(String name) {
			super(name);
		}
		@Override
		public String execute(String input) {
			return null;
		}
	}

}
