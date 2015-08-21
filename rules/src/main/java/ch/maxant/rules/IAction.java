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
 * This interface is used to create classes capable of being executed when 
 * the winning rules outcome is equal to the actions name.
 * @param <Input> The input type, see {@link #execute(Object)}
 * @param <Output> The output type, see {@link #execute(Object)}
 * See also {@link AbstractAction}, which is typically what applications override, and {@link ExecutableAction}.
 */
public interface IAction<Input, Output> extends ExecutableAction<Input, Output> {

	/** @return the unique name of this action.  the engine compares this name to the outcome from rules to decide if the action should be executed. */
	String getName();
}
