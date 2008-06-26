package mpicbg.image;

/**
 * @author Saalfeld and Preibisch
 *
 */
public class FloatPixel implements PixelType
{
	final private int sizeOfByte = 256;
	private double min = 0, max = sizeOfByte - 1; 	
	
	/**
	 * Stores a RGB value that is not transparent at all
	 */
	final int A = (sizeOfByte - 1) << 24;
	
	@Override
	final public int getNumChannels(){ return 1; }

	@Override
	final public byte toByte(Readable cursor)
	{ 
		// map the desired values to [0..sizeOfByte-1]
		double v = ((toFloat(cursor) - min)/(max - min)) * sizeOfByte;
		
		// cut of values outside that range (numerical instabilities + if v is exactly one it should be 255 as well)
		if (v < 0) v = 0;
		if (v >= sizeOfByte) v = sizeOfByte - 0.5;

		return (byte)v; 		
	}

	@Override
	final public float toFloat(Readable cursor){ return cursor.getFloatChannel( 0 ); }

	@Override
	final public int toRGBA(Readable cursor)
	{
		final byte v = toByte(cursor);
		return A + (v<<16) + (v<<8) + v;
	}

	@Override
	final public void setVisibleRange(double min, double max)
	{
		this.min = min;
		this.max = max;
	}

	@Override
	final public void setVisibleRange(double[] min, double[] max)
	{
		this.min = min[0];
		this.max = max[0];
	}

	@Override
	final public double[] getMinVisibleRange(){ return new double[]{min}; }

	@Override
	final public double[] getMaxVisibleRange(){ return new double[]{max}; }

}
