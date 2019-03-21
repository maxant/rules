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
