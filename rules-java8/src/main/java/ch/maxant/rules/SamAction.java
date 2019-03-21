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
