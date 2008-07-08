package mpicbg.image;

public abstract class AccessStrategy implements Readable, Writable, Operator
{
	final Container container;
	final Cursor cursor;
	
	public AccessStrategy( Container container, Cursor cursor )
	{
		assert container != null : "AccessStrategy(): Container is null";
		
		this.cursor = cursor;
		this.container = container;
	}
	
	final public Cursor getCursor() { return cursor; }	
	public abstract AccessStrategy clone(final Cursor c);
}
