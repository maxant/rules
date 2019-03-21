package ch.maxant.rules;

/**
 * This is an abstract implementation of the {@link IAction} interface.
 * Its implementation simply provides a mechanism for naming the object, 
 * and bases the {@link #equals(Object)} and {@link #hashCode()}
 * on that name.
 * 
 * @see IAction
 */
public abstract class AbstractAction<Input, Output> implements IAction<Input, Output> {

	private final String name;

	public AbstractAction(String name) {
		this.name = name;
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		AbstractAction<Input, Output> other = (AbstractAction<Input, Output>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Action [name=" + name + "]";
	}
	
}
