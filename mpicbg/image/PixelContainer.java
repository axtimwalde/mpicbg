package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class PixelContainer
{
	final PixelType type;
	int[] size;
	double[] resolution;
	Object data = null;
	
	
	PixelContainer( int[] size, double[] resolution, PixelType pt )
	{
		this.size = size.clone();
		this.resolution = resolution.clone();
		type = pt;
	}
	
	final public PixelType getPixelType()
	{
		return type;
	}
	
	public int getNumDimensions(){ return size.length; }
	public int[] getSize(){ return size.clone(); }
	public double[] getResolution(){ return resolution.clone(); }

}
