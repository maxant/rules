/*
 * Copyright (c) 2011-2017 Ant Kutschera
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
 * allows you to construct an action using a lambda.
 * @param <I> the input type of this action
 * @param <O> the output type of this action
 */
public class SamAction<I, O> extends AbstractAction<I, O> {

	private final ExecutableAction<I, O> action;

	/**
	 * @param name the name of this action
	 * @param action a lambda containing the function which should be 
	 * 		executed when <code>execute(Object)</code> is called by the <code>Engine</code>.
	 */
	public SamAction(String name, ExecutableAction<I, O> action) {
		super(name);
		this.action = action;
	}

	@Override
	public O execute(I input) {
		return action.execute(input);
	}

}
