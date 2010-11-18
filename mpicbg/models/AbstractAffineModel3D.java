package mpicbg.models;

public abstract class AbstractAffineModel3D < M extends AbstractAffineModel3D< M > > extends InvertibleModel< M > implements Affine3D< M > 
{
	public abstract float[] getMatrix( final float[] m );
}
