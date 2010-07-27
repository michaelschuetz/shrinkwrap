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
package org.jboss.shrinkwrap.tar.impl.importer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.impl.base.importer.ContentAssertionDelegateBase;
import org.jboss.shrinkwrap.impl.base.importer.StreamImporterImplTestBase;
import org.jboss.shrinkwrap.tar.api.exporter.TarGzExporter;
import org.jboss.shrinkwrap.tar.api.importer.TarGzImporter;
import org.jboss.tarbarian.api.TarGzInputStream;

/**
 * TestCase to verify the {@link TarGzImporterImpl} functionality.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
public class TarGzImporterImplTestCase extends StreamImporterImplTestBase<TarGzImporter, TarGzInputStream>
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   @SuppressWarnings("unused")
   private static final Logger log = Logger.getLogger(TarGzImporterImplTestCase.class.getName());

   /**
    * Delegate for performing TAR.GZ content assertions
    */
   private static final TarGzContentAssertionDelegate delegate = new TarGzContentAssertionDelegate();

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.importer.StreamImporterImplTestBase#getDelegate()
    */
   @Override
   protected ContentAssertionDelegateBase getDelegate()
   {
      return delegate;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.importer.StreamImporterImplTestBase#getImporterClass()
    */
   @Override
   protected Class<TarGzImporter> getImporterClass()
   {
      return TarGzImporter.class;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.importer.StreamImporterImplTestBase#getExporterClass()
    */
   @Override
   protected Class<? extends StreamExporter> getExporterClass()
   {
      return TarGzExporter.class;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.importer.StreamImporterImplTestBase#importFromStream(org.jboss.shrinkwrap.api.importer.StreamImporter, java.io.InputStream)
    */
   @Override
   protected TarGzImporter importFromStream(final TarGzImporter importer, final InputStream in)
         throws IllegalArgumentException, IOException
   {
      // Precondition checks
      if (importer == null)
      {
         throw new IllegalArgumentException("importer must be specified");
      }
      if (in == null)
      {
         throw new IllegalArgumentException("stream must be specified");
      }

      // Import
      final TarGzInputStream tarGzIn = new TarGzInputStream(in);
      return importer.importFrom(tarGzIn);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.importer.StreamImporterImplTestBase#getExceptionThrowingInputStream()
    */
   @Override
   protected TarGzInputStream getExceptionThrowingInputStream()
   {
      try
      {
         return ExceptionThrowingTarGzInputStream.create();
      }
      catch (final IOException e)
      {
         throw new RuntimeException("Should not occur in test setup", e);
      }
   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Members ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Test {@link TarGzInputStream} extension which throws errors when read
    * in order to test exception handling of the inport process
    * 
    * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
    */
   private static final class ExceptionThrowingTarGzInputStream extends TarGzInputStream
   {

      static ExceptionThrowingTarGzInputStream create() throws IOException
      {
         // First provide real GZIP content so we don't err out when initialized
         final byte[] test = "Something".getBytes();
         final ByteArrayOutputStream stream = new ByteArrayOutputStream();
         new GZIPOutputStream(stream).write(test);
         final InputStream in = new ByteArrayInputStream(stream.toByteArray());
         return new ExceptionThrowingTarGzInputStream(in);
      }

      private ExceptionThrowingTarGzInputStream(final InputStream in) throws IOException
      {
         super(in);
      }

      /**
       * Generates an exception when read
       * @see org.jboss.javatar.TarInputStream#read()
       */
      @Override
      public int read() throws IOException
      {
         throw new RuntimeException("Mock Exception, should be wrapped in the import process");
      }

   }
}
