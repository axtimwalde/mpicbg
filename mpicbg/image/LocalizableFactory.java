package mpicbg.image;


public interface LocalizableFactory< F extends Localizable >
{
	//public < T extends Localizable >T createLocalizable( Class< T > type );
	
	/**
	 * Create a RandomAccessible cursor at the floor location of the
	 * implementing cursor.
	 */
	public RandomAccess toRandomAccessible();
	
	/**
	 * Create a IteratableByDimension cursor at the floor location of the
	 * implementing cursor.
	 */
	public IteratorByDimension toIteratableByDimension();
}
