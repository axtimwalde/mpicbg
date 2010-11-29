package mpicbg.models;

public abstract class AbstractAffineModel3D < M extends AbstractAffineModel3D< M > > extends AbstractModel< M > implements Model< M >, InvertibleBoundable, Affine3D< M > 
{
	public abstract float[] getMatrix( final float[] m );
}
