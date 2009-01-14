/**
 * 
 */
package mpicbg.trakem2;

import java.awt.geom.AffineTransform;

import ini.trakem2.display.Patch;

import mpicbg.models.SimilarityModel2D;

public class SimilarityTile2D extends AbstractAffineTile2D< SimilarityModel2D >
{
	public SimilarityTile2D( final SimilarityModel2D model, final Patch patch )
	{
		super( model, patch );
	}
	
	/**
	 * Initialize the model with the parameters of the {@link AffineTransform}
	 * of the {@link Patch}.  The {@link AffineTransform} should be a
	 * Similarity, otherwise the results will not be what you might expect.
	 * This means, that:
	 * <pre>
	 *   {@link AffineTransform#getScaleX()} == {@link AffineTransform#getScaleY()}
	 *   {@link AffineTransform#getShearX()} == -{@link AffineTransform#getShearY()}
	 * </pre>
	 */
	@Override
	protected void initModel()
	{
		final AffineTransform a = patch.getAffineTransform();
		model.set( ( float )a.getScaleX(), ( float )a.getShearY(), ( float )a.getTranslateX(), ( float )a.getTranslateY() );
	}

}
