/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.trakem2;

import mpicbg.models.AffineModel2D;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;

public class MovingLeastSquaresTransform extends mpicbg.models.MovingLeastSquaresTransform implements CoordinateTransform
{

	final public void init( final String data ) throws NumberFormatException
	{
		matches.clear();
		
		final String[] fields = data.split( "\\s+" );
		if ( fields.length > 2 && fields.length % 4 == 2 )
		{
			if ( fields[ 0 ].equals( "translation" ) ) model = new TranslationModel2D();
			else if ( fields[ 0 ].equals( "rigid" ) ) model = new RigidModel2D();
			else if ( fields[ 0 ].equals( "similarity" ) ) model = new SimilarityModel2D();
			else if ( fields[ 0 ].equals( "affine" ) ) model = new AffineModel2D();
			else throw new NumberFormatException( "Inappropriate parameters for " + this.getClass().getCanonicalName() );
			
			alpha = Float.parseFloat( fields[ 1 ] );
			
			int i = 1;
			while ( i < fields.length - 1 )
			{
				final float[] p1 = new float[]{
						Float.parseFloat( fields[ ++i ] ),
						Float.parseFloat( fields[ ++i ] ) };
				final float[] p2 = new float[]{
						Float.parseFloat( fields[ ++i ] ),
						Float.parseFloat( fields[ ++i ] ) };
				final PointMatch m = new PointMatch( new Point( p1 ), new Point( p2 ) );
				matches.add( m );
			}
		}
		else throw new NumberFormatException( "Inappropriate parameters for " + this.getClass().getCanonicalName() );

	}

	public String toDataString()
	{
		String data = "";
		if ( model.getClass() == TranslationModel2D.class ) data += "translation";
		else if ( model.getClass() == RigidModel2D.class ) data += "rigid";
		else if ( model.getClass() == SimilarityModel2D.class ) data += "similarity";
		else if ( model.getClass() == AffineModel2D.class ) data += "affine";
		else data += "unknown";
		
		data += " " + alpha;
		
		for ( PointMatch m : matches )
		{
			final float[] p1 = m.getP1().getL();
			final float[] p2 = m.getP2().getW();
			data += " " + p1[ 0 ] + " " + p1[ 1 ] + " " + p2[ 0 ] + " " + p2[ 1 ];
		}
		return data;
	}

	final public String toXML( final String indent )
	{
		return indent + "<ict_transform class=\"" + this.getClass().getCanonicalName() + "\" data=\"" + toDataString() + "\"/>";
	}

}
