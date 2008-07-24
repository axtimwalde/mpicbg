package mpicbg.image;

public class AccessStrategyBackgroundValue extends AccessStrategyAbstract
{
	final ConstantCursor backgroundValueCursor; 
	
	public AccessStrategyBackgroundValue(final ConstantCursor backgroundValueCursor)
	{
		this.backgroundValueCursor = backgroundValueCursor;
	}	
		
	@Override
	protected void update(final Cursor cursor)
	{
		if (cursor.isInside())
		{			
			read = cursor;
			write = cursor;
		}
		else
		{
			read = backgroundValueCursor;
			write = null;
		}
	}

}
