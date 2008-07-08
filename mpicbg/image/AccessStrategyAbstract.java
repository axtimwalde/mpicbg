/**
 * 
 */
package mpicbg.image;

/**
 * @author Stephan
 *
 */
public abstract class AccessStrategyAbstract extends AccessStrategy
{
	WritableCursor writeCursor;
	ReadableCursor readCursor;
	
	public AccessStrategyAbstract(Container container, Cursor cursor)
	{
		super(container, cursor);
	}

	//
	// Readable Methods
	//
	final public void read( final Object[] v )
	{
		readCursor.read(v);
	}
	final public void read( final byte[] v )
	{
		readCursor.read(v);
	}
	final public void read( final short[] v )
	{
		readCursor.read(v);
	}
	final public void read( final int[] v )
	{
		readCursor.read(v);
	}
	final public void read( final long[] v )
	{
		readCursor.read(v);
	}
	final public void read(final float[] v)
	{ 
		readCursor.read(v);
	}
	final public void read( final double[] v )
	{
		readCursor.read(v);
	}
	
	
	final public Object[] get()
	{
		Object[] c = new Object[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public byte[] getBytes()
	{
		byte[] c = new byte[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public short[] getShorts()
	{
		short[] c = new short[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public int[] getInts()
	{
		int[] c = new int[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public long[] getLongs()
	{
		long[] c = new long[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public float[] getFloats()
	{
		final float[] f = new float[ container.getNumDim() ];
		read( f );
		return f;
	}
	final public double[] getDoubles()
	{
		double[] c = new double[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	
	final public Object getChannel( final int c )
	{
		return readCursor.getChannel(c);
	}
	final public byte getByteChannel( final int c )
	{
		return readCursor.getByteChannel(c);
	}
	final public short getShortChannel( final int c )
	{
		return readCursor.getShortChannel(c);
	}
	final public int getIntChannel( final int c )
	{
		return readCursor.getIntChannel(c);
	}
	final public long getLongChannel( final int c )
	{
		return readCursor.getLongChannel(c);
	}
	public final float getFloatChannel(final int c) 
	{ 
		return readCursor.getFloatChannel(c);
	}
	final public double getDoubleChannel( final int c )
	{
		return readCursor.getDoubleChannel(c);
	}
	
	//
	// operator methods
	// 
	final public void add( final Readable c )
	{		
		writeCursor.add(c);
	}
	final public void add( final byte c )
	{ 		
		writeCursor.add(c);
	}
	final public void add( final short c ) 
	{
		writeCursor.add(c);		
	}
	final public void add( final int c ) 
	{ 
		writeCursor.add(c);
	}
	final public void add( final long c ) 
	{	
		writeCursor.add(c);
	}
	final public void add( final float c )
	{
		writeCursor.add(c);
	}
	final public void add( final double c ) 
	{ 
		writeCursor.add(c);
	}
	
	final public void sub( final Readable c )
	{
		writeCursor.sub(c);
	}
	final public void sub( final byte c )
	{
		writeCursor.sub(c);
	}
	final public void sub( final short c ) 
	{ 
		writeCursor.sub(c);		
	}
	final public void sub( final int c ) 
	{ 
		writeCursor.sub(c);		
	}
	final public void sub( final long c ) 
	{
		writeCursor.sub(c);		
	}
	final public void sub( final float c )
	{
		writeCursor.sub(c);
	}
	final public void sub( final double c ) 
	{
		writeCursor.sub(c);		
	}
	
	final public void mul( final Readable c )
	{
		writeCursor.mul(c);
	}
	final public void mul( final byte c )
	{ 
		writeCursor.mul(c);		
	}
	final public void mul( final short c ) 
	{ 
		writeCursor.mul(c);		
	}
	final public void mul( final int c ) 
	{
		writeCursor.mul(c);
	}
	final public void mul( final long c ) 
	{
		writeCursor.mul(c);
	}
	final public void mul( final float c )
	{
		writeCursor.mul(c);
	}
	final public void mul( final double c ) 
	{
		writeCursor.mul(c);
	}
	
	final public void div( final Readable c )
	{
		writeCursor.div(c);
	}	
	final public void div( final byte c )
	{ 
		writeCursor.div(c);		
	}
	final public void div( final short c ) 
	{
		writeCursor.div(c);
	}
	final public void div( final int c )
	{ 
		writeCursor.div(c);		
	}
	final public void div( final long c ) 
	{
		writeCursor.div(c);
	}
	final public void div( final float c )
	{
		writeCursor.div(c);
	}		
	 public void div( double c ) 
	{ 
		 writeCursor.div(c);		
	}
	
	public void set( Object[] a )
	{
		writeCursor.set(a);
	}
	public void set( byte[] a )
	{
		writeCursor.set(a);
	}
	public void set( short[] a )
	{
		writeCursor.set(a);
	}
	public void set( int[] a )
	{
		writeCursor.set(a);
	}
	public void set( long[] a )
	{
		writeCursor.set(a);
	}
	public final void set( final float[] v)	
	{ 
		writeCursor.set(v);
	}	
	public void set( double[] a )
	{
		writeCursor.set(a);
	}

	public void setChannel( Object v, int c )
	{ 
		writeCursor.setChannel(v, c); 
	}
	public void setChannel( byte v, int c )
	{
		writeCursor.setChannel(v, c); 		
	}
	public void setChannel( short v, int c )
	{
		writeCursor.setChannel(v, c); 
	}
	public void setChannel( int v, int c )
	{
		writeCursor.setChannel(v, c); 
	}
	public void setChannel( long v, int c )
	{
		writeCursor.setChannel(v, c); 
	}
	public void setChannel(final float v, final int channel) 
	{ 
		writeCursor.setChannel(v, channel);		
	}	
	public void setChannel( double v, int c )
	{
		writeCursor.setChannel(v, c); 
	}
	
}
