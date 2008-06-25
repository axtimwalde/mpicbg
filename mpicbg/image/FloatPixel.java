package mpicbg.image;

/**
 * @author Saalfeld and Preibisch
 *
 */
public class FloatPixel implements PixelType
{

	@Override
	final public int getNumChannels(){ return 1; }

	@Override
	public byte toByte(Readable cursor){ return cursor.getByteChannel( 0 ); }

	/* (non-Javadoc)
	 * @see mpicbg.image.PixelType#toFloat(mpicbg.image.Readable)
	 */
	@Override
	public float toFloat(Readable cursor)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see mpicbg.image.PixelType#toRGBA(mpicbg.image.Readable)
	 */
	@Override
	public int toRGBA(Readable cursor)
	{
		// TODO Auto-generated method stub
		return 0;
	}

}
