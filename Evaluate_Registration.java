import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.plugin.*;
import ij.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.io.*;

import mpicbg.models.CoordinateTransformMap2D;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;

public class Evaluate_Registration implements PlugIn, KeyListener
{
	static protected String pattern = ".*\\.tif$";
	static protected int numSamples = 100;
	final protected Random rnd = new Random( 0 );
	final protected List< PointMatch > samples = new ArrayList< PointMatch >();
	
	final protected int listFiles( final String dir, final String pattern, final List< String > list )
	{
		File f = new File(dir);
		if (!f.exists() || !f.isDirectory())
			return 0;
		final String[] l = f.list();
		if ( l == null )
			return 0;
		if ( System.getProperty( "os.name" ).indexOf( "Linux" ) != -1 )
			ij.util.StringSorter.sort( l );
    	File f2;
    	for ( int i = 0; i < l.length; i++ )
		{
			f2 = new File( dir, l[ i ] );
			if ( !f2.isDirectory() && l[ i ].matches( pattern ) )
				list.add( l[ i ] );
		}
		return list.size();
	}	
	
	
	public void run( String args )
	{
		samples.clear();
		rnd.setSeed( 0 );
		
		final GenericDialog gd = new GenericDialog( "Evaluate registration results" );
		gd.addStringField( "File_name_pattern : ", pattern );
		gd.addNumericField( "Number_of_samples_per_tile : ", numSamples, 0 );
		
		gd.showDialog();
		if ( gd.wasCanceled() ) return;
		
		pattern = gd.getNextString();
		numSamples = ( int )gd.getNextNumber();
		
		final DirectoryChooser dc = new DirectoryChooser( "Choose a directory" );
		final String dir = dc.getDirectory();
		
		final List< String > fileNames = new ArrayList< String >();
		if ( listFiles( dir, pattern, fileNames ) == 0 ) return;
		
		for ( final String fileName : fileNames )
		{
			final String mapName = fileName.replaceAll( "\\.[^.]*$", ".map" );
			final String rMapName = fileName.replaceAll( "\\.[^.]*$", ".r.map" );
			
			CoordinateTransformMap2D map, rMap;
			try
			{
				map = new CoordinateTransformMap2D( new FileInputStream( dir + mapName ) );
				rMap = new CoordinateTransformMap2D( new FileInputStream( dir + rMapName ) );
				for ( int i = 0; i < numSamples; ++i )
				{
					float[] m = new float[]{ ( int )rnd.nextFloat() * map.getWidth(), ( int )rnd.nextFloat() * map.getHeight() };
					float[] r = m.clone();
					map.applyInPlace( m );
					rMap.applyInPlace( r );
					
					samples.add(
							new PointMatch(
									new Point( m ),
									new Point( r ) ) );
				}
			}
			catch ( IOException ex ){ ex.printStackTrace(); }
			
			IJ.log( dir + mapName );
		}
		
		RigidModel2D model = new RigidModel2D();
		try
		{
			model.fit( samples );
		}
		catch ( NotEnoughDataPointsException ex ){ ex.printStackTrace(); }
		
		double e = 0.0;
		double eMax = 0.0;
		double eMin = Double.MAX_VALUE;
		for ( PointMatch m : samples )
		{
			m.apply( model );
			final double ee = m.getDistance();
			if ( ee < eMin ) eMin = ee;
			if ( ee > eMax ) eMax = ee;
			e += ee;
		}
		e /= samples.size();
		
		IJ.log( "mean: " + e );
		IJ.log( "min:  " + eMin );
		IJ.log( "max:  " + eMax );
	}

	public void keyPressed(KeyEvent e)
	{
		if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) )
		{
		}
		else if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
		{
			return;
		}
	}

	public void keyReleased(KeyEvent e) { }

	public void keyTyped(KeyEvent e) { }
}
