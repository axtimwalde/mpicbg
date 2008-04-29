package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class PixelContainer
{
	final PixelType type;
	
	PixelContainer( PixelType pt )
	{
		type = pt;
	}
	
	public PixelType getPixelType()
	{
		return type;
	}

}
