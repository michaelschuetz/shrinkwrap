/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shrinkwrap.impl.base;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ExtensionLoader;
import org.jboss.shrinkwrap.api.Assignable;
import org.jboss.shrinkwrap.api.UnknownExtensionTypeExceptionDelegator;

/**
 * ServiceExtensionLoader
 *
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @author <a href="mailto:ken@glxn.net">Ken Gullaksen</a>
 * @version $Revision: $
 */
public class ServiceExtensionLoader implements ExtensionLoader
{
   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(ServiceExtensionLoader.class.getName());

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||
   
   private ClassLoader classLoader = SecurityActions.getThreadContextClassLoader();
   private Map<Class<?>, Class<?>> cache = new HashMap<Class<?>, Class<?>>();
   private Map<Class<?>, ExtensionWrapper> extensionMappings = new HashMap<Class<?>, ExtensionWrapper>();

   
   //-------------------------------------------------------------------------------------||
   // Required Implementations - ExtensionLoader -----------------------------------------||
   //-------------------------------------------------------------------------------------||
   
   /* (non-Javadoc)
    * @see org.jboss.shrinkwrap.api.ExtensionLoader#load(java.lang.Class, org.jboss.shrinkwrap.api.Archive)
    */
   @Override
   public <T extends Assignable> T load(Class<T> extensionClass, Archive<?> baseArchive)
   {
      if(isCached(extensionClass))
      {
         return createFromCache(extensionClass, baseArchive);
      }
      T object = createFromLoadExtension(extensionClass, baseArchive);
      
      addToCache(extensionClass, object.getClass());
      
      return object;
   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods - Cache ----------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   boolean isCached(Class<?> extensionClass) 
   {
      return cache.containsKey(extensionClass);
   }
   
   private <T extends Assignable> T createFromCache(Class<T> extensionClass, Archive<?> archive) 
   {
      Class<T> extensionImplClass = getFromCache(extensionClass);
      return createExtension(extensionImplClass, archive);
   }

   void addToCache(Class<?> extensionClass, Class<?> extensionImplClass)
   {
      cache.put(extensionClass, extensionImplClass);
   }

   @SuppressWarnings("unchecked")
   <T extends Assignable> Class<T> getFromCache(Class<T> extensionClass) 
   {
      return (Class<T>)cache.get(extensionClass);
   }
   
   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods - Override -------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /* (non-Javadoc)
    * @see org.jboss.shrinkwrap.api.ExtensionLoader#addOverride(java.lang.Class, java.lang.Class)
    */
   public <T extends Assignable> ServiceExtensionLoader addOverride(Class<T> extensionClass, Class<? extends T> extensionImplClass)
   {
      addToCache(extensionClass, extensionImplClass);
      return this;
   }

   /* (non-Javadoc)
    * @see org.jboss.shrinkwrap.api.ExtensionLoader#getExtensionFromExtensionMapping(java.lang.Class)
    */
   public <T extends Assignable> String getExtensionFromExtensionMapping(Class<T> type)
   {

      ExtensionWrapper extensionWrapper = extensionMappings.get(type);
      if (extensionWrapper == null) {
         loadExtensionMapping(type);
      }
      extensionWrapper = extensionMappings.get(type);
      if (extensionWrapper == null)
      {
         throw UnknownExtensionTypeExceptionDelegator.newExceptionInstance(type);
      }
      return extensionWrapper.getProperty("extension");
   }

   /**
    * Check to see if a specific extension interface is beeing overloaded
    * 
    * @param extensionClass The ExtensionType interface class
    * @return true if found
    */
   public boolean isOverriden(Class<?> extensionClass) 
   {
      return isCached(extensionClass);
   }

   
   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods - Loading --------------------------------------------------||
   //-------------------------------------------------------------------------------------||
   
   private <T extends Assignable> T createFromLoadExtension(Class<T> extensionClass, Archive<?> archive)
   {
      ExtensionWrapper extensionWrapper = loadExtensionMapping(extensionClass);
      if (extensionWrapper == null)
      {
         throw new RuntimeException("Failed to load ExtensionMapping");
      }

      Class<T> extensionImplClass = loadExtension(extensionWrapper);

      if (!extensionClass.isAssignableFrom(extensionImplClass)) {
         throw new RuntimeException(
            "Found extension impl class " + extensionImplClass.getName() +
               " not assignable to extension interface " + extensionClass.getName());
      }
      return createExtension(extensionImplClass, archive);
   }

   private <T extends Assignable> Class<T> loadExtension(ExtensionWrapper extensionWrapper)
   {
      return loadExtensionClass(extensionWrapper.implementingClassName);
   }

   private <T extends Assignable> ExtensionWrapper loadExtensionMapping(Class<T> extensionClass)   
   {
      URL extensionImplUrl = findExtensionImpl(extensionClass);

      ExtensionWrapper extensionWrapper = loadExtensionWrapper(extensionImplUrl, extensionClass);
      this.extensionMappings.put(extensionClass, extensionWrapper);
      return extensionWrapper;
   }

   
   private <T extends Assignable> URL findExtensionImpl(Class<T> extensionClass) 
   {
      try 
      {
         Enumeration<URL> urls  = getClassLoader().getResources(
               "META-INF/services/" + extensionClass.getName());
   
         List<URL> foundExtensions = Collections.list(urls);
         if(foundExtensions.size() == 0)
         {
            throw new RuntimeException(
                  "No extension implementation found for " + extensionClass.getName() + 
                  ", please verify classpath or add a extensionOverride");
         }
         if(foundExtensions.size() > 1) 
         {
            log.warning(
                  "Multiple extension implementations found for " + extensionClass.getName() + 
                   ", please verify classpath or add a extensionOverride");
//            throw new RuntimeException(
//                "Multiple extension implementations found for " + extensionClass.getName() + 
//                ", please verify classpath or add a extensionOverride");
         }
         return foundExtensions.get(0);
      } 
      catch (Exception e) 
      {
         throw UnknownExtensionTypeExceptionDelegator.newExceptionInstance(extensionClass);
      }
   }

   private <T extends Assignable> ExtensionWrapper loadExtensionWrapper(URL extensionURL, Class<T> extensionClass)
   {
      Properties properties = new Properties();
      try
      {
         properties.load(extensionURL.openStream());
      } catch (IOException e) {
         throw new RuntimeException("Could not open stream for extensionURL " + extensionURL, e);
      }
      String implementingClassName = (String) properties.get("implementingClassName");
      if(implementingClassName == null)
      {
         throw new RuntimeException("Property implementingClassName is not present in " + extensionURL);
      }
      final Map<String, String> map = new HashMap<String, String>(properties.size());
      final Enumeration<Object> keys = properties.keys();
      while (keys.hasMoreElements())
      {
         final String key = (String) keys.nextElement();
         final String value = (String) properties.get(key);
         map.put(key, value);
      }
      return new ExtensionWrapper(implementingClassName, map, extensionClass);
   }

   @SuppressWarnings("unchecked")
   private <T extends Assignable> Class<T> loadExtensionClass(String extensionClassName)
   {
      try 
      {
         return (Class<T>)getClassLoader().loadClass(extensionClassName);
      }
      catch (ClassNotFoundException e) {
         throw new RuntimeException("Could not load class " + extensionClassName, e);
      }
   }

   private <T extends Assignable> T createExtension(Class<T> extensionImplClass, Archive<?> archive)
   {

      T extension;
      Constructor<T> extensionImplConstructor = findConstructor(extensionImplClass);

      @SuppressWarnings("unchecked")
      Class<T> constructorArg = (Class<T>) extensionImplConstructor.getParameterTypes()[0];
      try
      {

         if (constructorArg.isInstance(archive))
         {
            extension = extensionImplConstructor.newInstance(archive);
         } else
         {
            extension = extensionImplConstructor.newInstance(
               load(constructorArg, archive));
         }
      }
      catch (InstantiationException e)
      {
         throw new ExtensionLoadingException("Failed to instantiate class of type " + archive.getClass() +
            ". The underlying class can not be abstract.", e);
      }
      catch (IllegalAccessException e)
      {
         throw new ExtensionLoadingException("Failed to instantiate class of type " + archive.getClass() +
            ". The underlying constructor is inaccessible.", e);
      }
      catch (InvocationTargetException e)
      {
         throw new ExtensionLoadingException("Failed to instantiate class of type " + archive.getClass() +
            ". The underlying constructor threw an exception.", e);
      }

      return extension;
   }

   @SuppressWarnings("unchecked")
   private <T extends Assignable> Constructor<T> findConstructor(Class<T> extensionImplClass) 
   {
      Constructor<?>[] constructors = SecurityActions.getConstructors(extensionImplClass);
      for(Constructor<?> constructor : constructors)
      {
         Class<?>[] parameters = constructor.getParameterTypes();
         if(parameters.length != 1) 
         {
            continue;
         }
         Class<?> parameter = parameters[0];
         if(Archive.class.isAssignableFrom(parameter)) 
         {
            return (Constructor<T>)constructor;
         }
      }
      throw new ExtensionLoadingException("No constructor with a single argument of type " +
            Archive.class.getName() + " could be found");
   }
   
   private ClassLoader getClassLoader() 
   {
      return classLoader;
   }
}
