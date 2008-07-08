package mpicbg.image;

import java.lang.reflect.Constructor;

public class Test
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		/*try
		{
			Class cfoo = Foo.class;
			Constructor con = cfoo.getConstructor(Integer.TYPE, String.class);
			Object o = con.newInstance(new Object[]{new Integer(27), "Hi !"});
			Foo foo = (Foo)o;
		}
		catch (Exception e){e.printStackTrace();};

		
		if (true)
			return;*/
	
		// Define the PixelType we want to use
		FloatPixel pixelType = new FloatPixel();
		
		// Define the Container to work with
		int[] dim = new int[]{10, 11};		
		//int[] dim = new int[]{2, 2};
		FloatStream container = new FloatStream(pixelType, dim);
		
		// Create some iterators for this container
		StreamIterator i = null;
		StreamIteratorByDimension iDim = null;
		StreamRandomAccess iRnd = null;
		try
		{
			i = new StreamIterator(container);
			iDim = new StreamIteratorByDimension(container);
			iRnd = new StreamRandomAccess(container);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
		
		System.out.println("Initial position: " + iDim.localize()[0] + " " + iDim.localize()[1]);		
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
