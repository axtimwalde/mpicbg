/**
 * 
 */
package mpicbg.image;

/**
 * @author Stephan
 *
 */
public abstract class AccessStrategyAbstract extends Access
{
	Cursor write;
	Cursor read;
		
	abstract void update(Cursor cursor);

	//
	// Readable Methods
	//
	final public void read( final Cursor c, final Object[] v )
	{
		update( c );
		read.read( v );
	}
	final public void read( final Cursor c, final byte[] v )
	{
		update( c );
		read.read( v );
	}
	final public void read( final Cursor c, final short[] v )
	{
		update( c );
		read.read( v );
	}
	final public void read( final Cursor c, final int[] v )
	{
		update( c );
		read.read( v );
	}
	final public void read( final Cursor c, final long[] v )
	{
		update( c );
		read.read( v );
	}
	final public void read( final Cursor c, final float[] v)
	{ 
		update( c );
		read.read( v );
	}
	final public void read( final Cursor c, final double[] v )
	{
		update( c );
		read.read( v );
	}
		
	final public Object getChannel( final Cursor c, final int v )
	{
		update( c );
		return read.getChannel( v );
	}
	final public byte getByteChannel( final Cursor c, final int v )
	{
		update( c );
		return read.getByteChannel( v );
	}
	final public short getShortChannel( final Cursor c, final int v )
	{
		update( c );
		return read.getShortChannel( v );
	}
	final public int getIntChannel( final Cursor c, final int v )
	{
		update( c );
		return read.getIntChannel( v );
	}
	final public long getLongChannel( final Cursor c, final int v )
	{
		update( c );
		return read.getLongChannel( v );
	}
	public final float getFloatChannel( final Cursor c, final int v ) 
	{ 
		update( c );
		return read.getFloatChannel( v );
	}
	final public double getDoubleChannel( final Cursor c, final int v )
	{
		update( c );
		return read.getDoubleChannel( v );
	}
	
	public void set( final Cursor c, final Object[] v )
	{
		update( c );
		write.set( v );
	}
	public void set( final Cursor c, final byte[] v )
	{
		update( c );
		write.set( v );
	}
	public void set( final Cursor c, final short[] v )
	{
		update( c );
		write.set( v );
	}
	public void set( final Cursor c, final int[] v )
	{
		update( c );
		write.set( v );
	}
	public void set( final Cursor c, final long[] v )
	{
		update( c );
		write.set( v );
	}
	public final void set( final Cursor c, final float[] v)	
	{ 
		update( c );
		write.set( v );
	}	
	public void set( final Cursor c, final double[] v )
	{
		update( c );
		write.set( v );
	}

	public void setChannel( final Cursor c, final int i, final Object v )
	{ 
		update( c );
		write.setChannel( i, v ); 
	}
	public void setChannel( final Cursor c, final int i, final byte v )
	{
		update( c );
		write.setChannel( i, v ); 		
	}
	public void setChannel( final Cursor c, final int i, final short v )
	{
		update( c );
		write.setChannel( i, v ); 
	}
	public void setChannel( final Cursor c, final int i, final int v )
	{
		update( c );
		write.setChannel( i, v ); 
	}
	public void setChannel( final Cursor c, final int i, final long v )
	{
		update( c );
		write.setChannel( i, v ); 
	}
	public void setChannel( final Cursor c, final int i, final float v ) 
	{ 
		update( c );
		write.setChannel( i, v );		
	}	
	public void setChannel( final Cursor c, final int i, final double v )
	{
		update( c );
		write.setChannel( i, v ); 
	}
	
}
