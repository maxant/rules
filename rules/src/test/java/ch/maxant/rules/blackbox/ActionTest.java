/*
 * Copyright (c) 2011-2014 Ant Kutschera
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.maxant.rules.AbstractAction;

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
