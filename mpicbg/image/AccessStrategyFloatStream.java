package mpicbg.image;

public class AccessStrategyFloatStream extends AccessStrategyFloat
{
	final FloatStream floatStream;
	final StreamCursor streamCursor;

	public AccessStrategyFloatStream(FloatStream stream, StreamCursor cursor)
	{
		super(stream, (Cursor)cursor);
		floatStream = stream;
		streamCursor = cursor;
	}
	
	@Override
	public AccessStrategy clone(Cursor cursor)
	{
		return new AccessStrategyFloatStream(floatStream, (StreamCursor)cursor);
	}

	public final float getFloatChannel(final int channel)  { return floatStream.data[streamCursor.getStreamIndex() + channel]; }	
	public void setChannel(final float v, final int channel) { floatStream.data[streamCursor.getStreamIndex() + channel] = v; }	
	public void read(final float[] v)
	{
		for (int c = 0; c < v.length; c++)
			v[c] = floatStream.data[streamCursor.getStreamIndex() + c];		
	}	
	public final void set(final float[] v)
	{
		for (int c = 0; c < v.length; c++)
			floatStream.data[streamCursor.getStreamIndex() + c] = v[c];
	}	
	
}
