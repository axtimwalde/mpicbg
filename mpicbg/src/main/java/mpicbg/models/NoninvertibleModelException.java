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
 */
package mpicbg.models;

/**
 * Signalizes that a {@link AbstractModel} is not invertible.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class NoninvertibleModelException extends Exception
{
	private static final long serialVersionUID = 8790171367047404173L;


	public NoninvertibleModelException()
	{
		super( "Non invertible Model." );
	}


	public NoninvertibleModelException( final String message )
	{
		super( message );
	}


	public NoninvertibleModelException( final Throwable cause )
	{
		super( cause );
	}


	public NoninvertibleModelException( final String message, final Throwable cause )
	{
		super( message, cause );
	}
}
