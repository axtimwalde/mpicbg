package mpicbg.image;

public interface Iteratable< I extends Container< ? extends PixelType, ? extends Cursor > >
{
	public abstract Iterator< I > createIterator();
}
