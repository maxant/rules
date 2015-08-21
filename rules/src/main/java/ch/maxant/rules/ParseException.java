/*
 * Copyright (c) 2011-2015 Ant Kutschera
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
 * When replacing {@link SubRule} placeholders (the '#' character) in rules, this exception may
 * be thrown if no suitable subrule can be found.  
 * 
 * @see Engine#Engine(java.util.Collection, boolean)
 */
public class ParseException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public ParseException(String msg) {
		super(msg);
	}

}
