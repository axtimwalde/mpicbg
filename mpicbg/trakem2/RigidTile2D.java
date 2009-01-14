/**
 * 
 */
package mpicbg.trakem2;

import java.awt.geom.AffineTransform;

import ini.trakem2.display.Patch;

public class RigidTile2D extends AbstractAffineTile2D< mpicbg.models.RigidModel2D >
{
	public RigidTile2D( final mpicbg.models.RigidModel2D model, final Patch patch )
	{
		super( model, patch );
	}
	
	/**
	 * Initialize the model with the parameters of the {@link AffineTransform}
	 * of the {@link Patch}.  The {@link AffineTransform} should be a
	 * Rigid Transformation, otherwise the results will not be what you might
	 * expect.  This means, that:
	 * <pre>
	 *   {@link AffineTransform#getScaleX()} == {@link AffineTransform#getScaleY()}
	 *   {@link AffineTransform#getShearX()} == -{@link AffineTransform#getShearY()}
	 * </pre>
	 * and
	 * <pre>
	 *   {@link AffineTransform#getScaleX()}<sup>2</sup> + {@link AffineTransform#getShearX()}<sup>2</sup> == 1
	 * </pre>
	 */
	@Override
	protected void initModel()
	{
		final AffineTransform a = patch.getAffineTransform();
		model.set( ( float )a.getScaleX(), ( float )a.getShearY(), ( float )a.getTranslateX(), ( float )a.getTranslateY() );
	}

}
