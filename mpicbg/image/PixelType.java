/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class PixelType
{
	/**
	 * 
	 * @return number of channels
	 */
	abstract int getSize();
	
	/**
	 * 
	 * @return type of all channels 
	 */
	abstract Class getType();
	
	/**
	 * To be imnplemented as a static method.
	 * 
	 * @param pixel raw pixel data
	 * 
	 * @return rgba integer
	 */
	abstract int toRGBA( Object pixel );
	
	private void test()
	{
		Object t =  null;
		float[] s = (float[])t;
		t = s;
		
		Float[] g = (Float[])t;
	}
}
