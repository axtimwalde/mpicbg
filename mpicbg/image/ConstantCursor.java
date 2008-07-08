package mpicbg.image;

public abstract class ConstantCursor extends Cursor implements Readable, Writable, Operator
{
	final int dim;

	ConstantCursor(final int dim)
	{
		super(null, null, null);
		this.dim = dim;
	}
	
	final public int getDim() { return dim; }
	
	@Override
	final public boolean isInside() { return true; }

}
