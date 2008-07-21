package mpicbg.image;

public abstract class AccessStrategy< I extends Container< ? extends PixelType, ? extends ContainerRead, ? extends ContainerWrite >, C extends Cursor >
	implements ContainerRead< C >, ContainerWrite< C >
{
	final I container;
	
	public AccessStrategy( I container )
	{
		this.container = container;
	}
}
