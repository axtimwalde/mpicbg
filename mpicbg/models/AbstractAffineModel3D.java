package mpicbg.models;

public abstract class AbstractAffineModel3D < M extends AbstractAffineModel3D< M > > extends AbstractModel< M > implements InvertibleModel< M >, Affine3D< M > 
{
	public abstract float[] getMatrix( final float[] m );
}
