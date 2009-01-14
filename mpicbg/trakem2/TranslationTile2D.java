/**
 * 
 */
package mpicbg.trakem2;

import java.awt.geom.AffineTransform;

import ini.trakem2.display.Patch;

public class TranslationTile2D extends AbstractAffineTile2D< mpicbg.models.TranslationModel2D >
{
	public TranslationTile2D( final mpicbg.models.TranslationModel2D model, final Patch patch )
	{
		super( model, patch );
	}
	
	/**
	 * Initialize the model with the parameters of the {@link AffineTransform}
	 * of the {@link Patch}.  The {@link AffineTransform} should be a
	 * Translation, otherwise the results will not be what you might expect.
	 * This means, that:
	 * <pre>
	 *   {@link AffineTransform#getScaleX()} == {@link AffineTransform#getScaleY()} == 1
	 *   {@link AffineTransform#getShearX()} == {@link AffineTransform#getShearY()} == 0
	 * </pre>
	 */
	@Override
	protected void initModel()
	{
		final AffineTransform a = patch.getAffineTransform();
		model.set( ( float )a.getTranslateX(), ( float )a.getTranslateY() );
	}

}
