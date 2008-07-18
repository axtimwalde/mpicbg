package mpicbg.image;

public abstract class AccessStrategy implements Readable, Writable, Operator
{
	final Container container;
	final Cursor cursor;
	
	public AccessStrategy( Container container, Cursor cursor )
	{
		this.cursor = cursor;
		this.container = container;
	}
	
	//abstract public void move( final Cursor cursor );
	
	final public Cursor getCursor() { return cursor; }	
	public abstract AccessStrategy clone( final Cursor c );
}
