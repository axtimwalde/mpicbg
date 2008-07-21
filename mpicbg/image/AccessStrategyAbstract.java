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
	Operator operator;
	Writable write;
	Readable read;
	
	public AccessStrategyAbstract(Container container, OldCursor cursor)
	{
		super(container, cursor);
	}
	
	abstract void update();

	//
	// Readable Methods
	//
	final public void read( final Object[] v )
	{
		update();
		read.read(v);
	}
	final public void read( final byte[] v )
	{
		read.read(v);
	}
	final public void read( final short[] v )
	{
		read.read(v);
	}
	final public void read( final int[] v )
	{
		read.read(v);
	}
	final public void read( final long[] v )
	{
		read.read(v);
	}
	final public void read(final float[] v)
	{ 
		read.read(v);
	}
	final public void read( final double[] v )
	{
		read.read(v);
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
		return read.getChannel(c);
	}
	final public byte getByteChannel( final int c )
	{
		return read.getByteChannel(c);
	}
	final public short getShortChannel( final int c )
	{
		return read.getShortChannel(c);
	}
	final public int getIntChannel( final int c )
	{
		return read.getIntChannel(c);
	}
	final public long getLongChannel( final int c )
	{
		return read.getLongChannel(c);
	}
	public final float getFloatChannel(final int c) 
	{ 
		return read.getFloatChannel(c);
	}
	final public double getDoubleChannel( final int c )
	{
		return read.getDoubleChannel(c);
	}
	
	//
	// operator methods
	// 
	final public void add( final Readable c )
	{		
		operator.add(c);
	}
	final public void add( final byte c )
	{ 		
		operator.add(c);
	}
	final public void add( final short c ) 
	{
		operator.add(c);		
	}
	final public void add( final int c ) 
	{ 
		operator.add(c);
	}
	final public void add( final long c ) 
	{	
		operator.add(c);
	}
	final public void add( final float c )
	{
		operator.add(c);
	}
	final public void add( final double c ) 
	{ 
		operator.add(c);
	}
	
	final public void sub( final Readable c )
	{
		operator.sub(c);
	}
	final public void sub( final byte c )
	{
		operator.sub(c);
	}
	final public void sub( final short c ) 
	{ 
		operator.sub(c);		
	}
	final public void sub( final int c ) 
	{ 
		operator.sub(c);		
	}
	final public void sub( final long c ) 
	{
		operator.sub(c);		
	}
	final public void sub( final float c )
	{
		operator.sub(c);
	}
	final public void sub( final double c ) 
	{
		operator.sub(c);		
	}
	
	final public void mul( final Readable c )
	{
		operator.mul(c);
	}
	final public void mul( final byte c )
	{ 
		operator.mul(c);		
	}
	final public void mul( final short c ) 
	{ 
		operator.mul(c);		
	}
	final public void mul( final int c ) 
	{
		operator.mul(c);
	}
	final public void mul( final long c ) 
	{
		operator.mul(c);
	}
	final public void mul( final float c )
	{
		operator.mul(c);
	}
	final public void mul( final double c ) 
	{
		operator.mul(c);
	}
	
	final public void div( final Readable c )
	{
		operator.div(c);
	}	
	final public void div( final byte c )
	{ 
		operator.div(c);		
	}
	final public void div( final short c ) 
	{
		operator.div(c);
	}
	final public void div( final int c )
	{ 
		operator.div(c);		
	}
	final public void div( final long c ) 
	{
		operator.div(c);
	}
	final public void div( final float c )
	{
		operator.div(c);
	}		
	 public void div( double c ) 
	{ 
		 operator.div(c);		
	}
	
	public void set( Object[] a )
	{
		write.set(a);
	}
	public void set( byte[] a )
	{
		write.set(a);
	}
	public void set( short[] a )
	{
		write.set(a);
	}
	public void set( int[] a )
	{
		write.set(a);
	}
	public void set( long[] a )
	{
		write.set(a);
	}
	public final void set( final float[] v)	
	{ 
		write.set(v);
	}	
	public void set( double[] a )
	{
		write.set(a);
	}

	public void setChannel( Object v, int c )
	{ 
		write.setChannel(v, c); 
	}
	public void setChannel( byte v, int c )
	{
		write.setChannel(v, c); 		
	}
	public void setChannel( short v, int c )
	{
		write.setChannel(v, c); 
	}
	public void setChannel( int v, int c )
	{
		write.setChannel(v, c); 
	}
	public void setChannel( long v, int c )
	{
		write.setChannel(v, c); 
	}
	public void setChannel(final float v, final int channel) 
	{ 
		write.setChannel(v, channel);		
	}	
	public void setChannel( double v, int c )
	{
		write.setChannel(v, c); 
	}
	
}
