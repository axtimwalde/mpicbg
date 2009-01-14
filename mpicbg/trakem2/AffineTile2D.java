/**
 * 
 */
package mpicbg.trakem2;

import ini.trakem2.display.Patch;

import mpicbg.models.AffineModel2D;

public class AffineTile2D extends AbstractAffineTile2D< AffineModel2D >
{
	public AffineTile2D( final AffineModel2D model, final Patch patch )
	{
		super( model, patch );
	}
	
	@Override
	protected void initModel()
	{
		model.set( patch.getAffineTransform() );
	}

}
