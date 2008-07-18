package mpicbg.image;

public class AccessStrategyMirror extends AccessStrategyAbstract
{
	final ConstantCursor backgroundValueCursor; 
	final AccessStrategy directAccessStrategy;
	
	public AccessStrategyMirror(final Container container, final ConstantCursor backgroundValueCursor, final Cursor cursor)
	{
		super(container, (Cursor)cursor);
		this.backgroundValueCursor = backgroundValueCursor;
		directAccessStrategy = container.createDirectAccessStrategy();
		
		if (cursor != null)
			update();
	}	
	
	public AccessStrategyMirror(final Container container, final ConstantCursor backgroundValueCursor)
	{
		this(container, backgroundValueCursor, null);
	}

	@Override
	public AccessStrategy clone(final Cursor c)
	{
		return new AccessStrategyMirror(container, backgroundValueCursor, cursor);
	}	
	
	@Override
	protected void update()
	{
		if (cursor.isInside())
		{
			read = directAccessStrategy;
			write = directAccessStrategy;
			operator = directAccessStrategy;
		}
		else
		{
			read = backgroundValueCursor;
			write = backgroundValueCursor;
			operator = backgroundValueCursor;
		}
	}
}
