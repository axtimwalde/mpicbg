package mpicbg.image;

public interface IteratableByDimension< I extends Container< ? extends PixelType, ? extends Cursor > >
{
	public IteratorByDimension< I > createIteratorByDimension();
}
