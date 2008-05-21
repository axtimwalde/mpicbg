package mpicbg.image;

public class FloatStreamCursor implements Iteratable, IteratableByDimension, RandomAccessible
{
	final FloatStream image;
	
	int i = 0;
	boolean hasNext = false;
	boolean hasPrev = false;
	
	final int[] iByDim;
	final boolean[] hasNextByDim;
	final boolean[] hasPrevByDim;
	
	FloatStreamCursor( FloatStream image )
	{
		this.image = image;
		int nd = image.getNumDim();
		iByDim = new int[ nd ];
		hasNextByDim = new boolean[ nd ];
		hasPrevByDim = new boolean[ nd ];
	}
	

	public boolean hasNext(){ return hasNext; }
	public boolean hasPrev(){ return hasPrev; }

	public void next() throws OutOfBoundsException
	{
		for ( int j = image.getNumDim() - 1; j >= 0; --j )
		{
			iByDim[ j ] = ++iByDim[ j ] % image.getDim( j );
			hasNextByDim[ j ] = ( iByDim[ j ] != image.getDim( j ) - 1 );
			hasPrevByDim[ j ] = ( iByDim[ j ] != 0 );
			...
		}
	}

	public void prev() throws OutOfBoundsException
	{
		// TODO Auto-generated method stub

	}

	public boolean hasNext( int dimension )
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasPrev( int dimension )
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void next( int dimension ) throws OutOfBoundsException
	{
		// TODO Auto-generated method stub

	}

	public void prev( int dimension ) throws OutOfBoundsException
	{
		// TODO Auto-generated method stub

	}

	public void to( int[] location )
	{
		// TODO Auto-generated method stub

	}

}
