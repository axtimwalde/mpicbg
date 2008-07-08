package mpicbg.image;

public class AccessStrategyBackgroundValue extends AccessStrategyAbstract
{
	final ConstantCursor backgroundValueCursor; 
	final AccessStrategy directAccessStrategy;
	
	public AccessStrategyBackgroundValue(final Container container, final ConstantCursor backgroundValueCursor, final Cursor cursor)
	{
		super(container, (Cursor)cursor);
		this.backgroundValueCursor = backgroundValueCursor;
		directAccessStrategy = container.createDirectAccessStrategy();
		
		if (cursor != null)
		{
			if (cursor.isInside())
			{
				readCursor = (ReadableCursor)cursor;
			}
		}
	}	
	
	public AccessStrategyBackgroundValue(final Container container, final ConstantCursor backgroundValueCursor)
	{
		this(container, backgroundValueCursor, null);
	}

	@Override
	public AccessStrategy clone(final Cursor c)
	{
		return new AccessStrategyBackgroundValue(container, backgroundValueCursor, cursor);
	}	
}
