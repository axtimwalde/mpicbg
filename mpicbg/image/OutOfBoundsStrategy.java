package mpicbg.image;

public interface OutOfBoundsStrategy
{
	public void transformCoordinates(final float[] l);
	public void transformCoordinates(final int[] l);
}
