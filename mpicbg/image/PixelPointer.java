package mpicbg.image;

public abstract class PixelPointer
{
	final PixelContainer container;
	abstract int[] getLocation();
	
	PixelPointer( PixelContainer pc )
	{
		container = pc;
	}
	
	abstract Object getRawPixel();
}
