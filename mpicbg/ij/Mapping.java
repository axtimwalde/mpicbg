package mpicbg.ij;

import ij.process.ImageProcessor;

public interface Mapping
{
	abstract public void map(
			ImageProcessor source,
			ImageProcessor target );
	
	abstract public void mapInterpolated(
			ImageProcessor source,
			ImageProcessor target );
}
