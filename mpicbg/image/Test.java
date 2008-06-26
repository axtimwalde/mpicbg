package mpicbg.image;

public class Test
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Define the PixelType we want to use
		FloatPixel pixelType = new FloatPixel();
		
		// Define the Container to work with
		int[] dim = new int[]{10, 11};		
		FloatStream container = new FloatStream(pixelType, dim);
		
		// Create some iterators for this container
		FloatStreamIterator i = new FloatStreamIterator(container);
		FloatStreamIteratorByDimension iDim = new FloatStreamIteratorByDimension(container);
		FloatStreamRandomAccess iRnd = new FloatStreamRandomAccess(container);
		
		float val = 0;
		final int[] pos = new int[2];
		
		for (pos[1] = 0; pos[1] < dim[1]; pos[1]++)
			for (pos[0] = 0; pos[0] < dim[0]; pos[0]++)
			{
				iRnd.to(pos);
				iRnd.setChannel(val, 0);
				val += 1;
			}

		do
		{
			i.localize(pos);
			System.out.println(pos[0] + " " + pos[1] + ": " + i.getFloatChannel(0));

			i.next();
		} 
		while (i.isInside());
		i.prev();
		
		do
		{
			iDim.localize(pos);
			System.out.println(pos[0] + " " + pos[1] + ": " + iDim.getFloatChannel(0));

			iDim.next(0);
			iDim.next(1);
		} 
		while (iDim.isInside(0) && iDim.isInside(1));
		iDim.prev(0);
		iDim.prev(1);		
	}

}
