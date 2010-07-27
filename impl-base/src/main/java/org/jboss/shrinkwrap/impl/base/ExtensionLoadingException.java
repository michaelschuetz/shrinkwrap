package org.jboss.shrinkwrap.impl.base;

import java.lang.reflect.InvocationTargetException;

/**
 * ExtensionLoadingException
 * This Unchecked Exception Class is thrown from the {@link org.jboss.shrinkwrap.impl.base.ServiceExtensionLoader}
 * when something wrong has happened that we can not recover from. 
 *
 * @author <a href="mailto:ken@glxn.net">Ken Gullaksen</a>
 * @version $Revision: $
 */
public class ExtensionLoadingException extends RuntimeException
{
   /**
    * @param message user-friendly description of why message is thrown
    */
   public ExtensionLoadingException(String message)
   {
      super(message);
   }

   /**
    * @param message user-friendly description of why message is thrown
    * @param cause Underlying cause, add this when using Exception Translation / rethrowing
    */
   public ExtensionLoadingException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
