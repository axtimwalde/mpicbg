package mpicbg.ij;

import ij.process.ImageProcessor;

public interface InverseMapping
{
	abstract public void mapInverse(
			ImageProcessor source,
			ImageProcessor target );
	
	abstract public void mapInverseInterpolated(
			ImageProcessor source,
			ImageProcessor target );
}
