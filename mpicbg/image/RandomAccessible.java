package mpicbg.image;

public interface RandomAccessible< I extends Container< ? extends PixelType, ? extends Cursor > >
{
	public abstract RandomAccess< I > createRandomAccess();
}
