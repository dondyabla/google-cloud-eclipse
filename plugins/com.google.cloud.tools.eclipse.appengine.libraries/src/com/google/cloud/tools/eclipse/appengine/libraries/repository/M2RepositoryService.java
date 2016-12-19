/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Filter;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.impl.DefaultSourceDownloaderJobFactory;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.impl.M2EclipseMavenHelper;
import com.google.cloud.tools.eclipse.util.io.FileDownloader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the artifacts and store
 * them in the local Maven repository pointed to by M2Eclipse's M2_REPO variable.
 */
@Component
public class M2RepositoryService implements ILibraryRepositoryService {

  private static final String CLASSPATH_ATTRIBUTE_SOURCE_URL =
      "com.google.cloud.tools.eclipse.appengine.libraries.sourceUrl";

  private MavenHelper mavenHelper;
  private MavenCoordinatesClasspathAttributesTransformer transformer;
  private SourceDownloaderJobFactory sourceDownloaderJobFactory;

  @Override
  public IClasspathEntry getLibraryClasspathEntry(IJavaProject javaProject, LibraryFile libraryFile) 
                                                                            throws LibraryRepositoryServiceException {
    verifyDependencies();
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    try {
      Artifact artifact = mavenHelper.resolveArtifact(null, mavenCoordinates);
      IClasspathAttribute[] libraryFileClasspathAttributes = getClasspathAttributes(libraryFile, artifact);
      URL sourceUrl = getSourceUrlFromUri(libraryFile.getSourceUri());
      Path classpathEntryPath = new Path(artifact.getFile().getAbsolutePath());
      return JavaCore.newLibraryEntry(classpathEntryPath,
                                      getSourceLocation(mavenCoordinates, sourceUrl, javaProject, classpathEntryPath),
                                      null /*  sourceAttachmentRootPath */,
                                      getAccessRules(libraryFile.getFilters()),
                                      libraryFileClasspathAttributes,
                                      true /* isExported */);
    } catch (CoreException ex) {
      throw new LibraryRepositoryServiceException(NLS.bind(Messages.ResolveArtifactError, mavenCoordinates), ex);
    }
  }

  @Override
  public IClasspathEntry rebuildClasspathEntry(IJavaProject javaProject, IClasspathEntry classpathEntry) 
                                                                            throws LibraryRepositoryServiceException {
    verifyDependencies();
    MavenCoordinates mavenCoordinates = transformer.createMavenCoordinates(classpathEntry.getExtraAttributes());
    try {
      Artifact artifact = mavenHelper.resolveArtifact(null, mavenCoordinates);
      URL sourceUrl = getSourceUrlFromAttribute(classpathEntry.getExtraAttributes());
      Path classpathEntryPath = new Path(artifact.getFile().getAbsolutePath());
      return JavaCore.newLibraryEntry(classpathEntryPath,
                                      getSourceLocation(mavenCoordinates, sourceUrl, javaProject, classpathEntryPath),
                                      null /*  sourceAttachmentRootPath */,
                                      classpathEntry.getAccessRules(),
                                      classpathEntry.getExtraAttributes(),
                                      true /* isExported */);
    } catch (CoreException ex) {
      throw new LibraryRepositoryServiceException(NLS.bind(Messages.ResolveArtifactError, mavenCoordinates), ex);
    }
  }

  private URL getSourceUrlFromUri(URI sourceUri) {
    try {
      if (sourceUri == null) {
        return null;
      } else {
        return sourceUri.toURL();
      }
    } catch (MalformedURLException | IllegalArgumentException e) {
      // should not cause error in the resolution process, we'll disregard it
      return null;
    }
  }

  private URL getSourceUrlFromAttribute(IClasspathAttribute[] extraAttributes) {
    try {
      for (IClasspathAttribute iClasspathAttribute : extraAttributes) {
        if (CLASSPATH_ATTRIBUTE_SOURCE_URL.equals(iClasspathAttribute.getName())) {
          return new URL(iClasspathAttribute.getValue());
        }
      }
    } catch (MalformedURLException e) {
      // should not cause error in the resolution process, we'll disregard it
    }
    return null;
  }

  private IClasspathAttribute[] getClasspathAttributes(LibraryFile libraryFile, Artifact artifact)
                                                                              throws LibraryRepositoryServiceException {
    try {
      List<IClasspathAttribute> attributes =
          transformer.createClasspathAttributes(artifact, libraryFile.getMavenCoordinates());
      if (libraryFile.isExport()) {
        attributes.add(UpdateClasspathAttributeUtil.createDependencyAttribute(true /* isWebApp */));
      } else {
        attributes.add(UpdateClasspathAttributeUtil.createNonDependencyAttribute());
      }
      if (libraryFile.getSourceUri() != null) {
        addUriAttribute(attributes, CLASSPATH_ATTRIBUTE_SOURCE_URL, libraryFile.getSourceUri());
      }
      if (libraryFile.getJavadocUri() != null) {
        addUriAttribute(attributes, IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, libraryFile.getJavadocUri());
      }
      return attributes.toArray(new IClasspathAttribute[0]);
    } catch (CoreException ex) {
      throw new LibraryRepositoryServiceException("Could not create classpath attributes", ex);
    }
  }

  private void addUriAttribute(List<IClasspathAttribute> attributes, String attributeName, URI uri) {
    try {
      attributes.add(JavaCore.newClasspathAttribute(attributeName, uri.toURL().toString()));
    } catch (MalformedURLException | IllegalArgumentException ex) {
      // disregard invalid URL
    }
  }

  private IPath getSourceLocation(final MavenCoordinates mavenCoordinates, final URL sourceUrl, final IJavaProject javaProject, final IPath classpathEntryPath) {
    if (sourceUrl == null) {
      if (javaProject != null) {
        sourceDownloaderJobFactory.createM2SourceDownloaderJob(javaProject, mavenCoordinates, classpathEntryPath, mavenHelper).schedule();
        return null;
      } else {
        // without project the async job wouldn't know where to add the downloaded jar, let's resolve it synchronized
        return mavenHelper.getMavenSourceJarLocation(mavenCoordinates);
      }
    } else {
      if (javaProject != null) {
        sourceDownloaderJobFactory.createRemoteFileSourceDownloaderJob(javaProject, classpathEntryPath, mavenCoordinates, sourceUrl).schedule();
        return null;
      } else {
        // without project the async job wouldn't know where to add the downloaded jar, let's resolve it synchronized
        return getDownloadedSourceLocation(mavenCoordinates, sourceUrl);
      }
    }
  }

  private IPath getDownloadedSourceLocation(MavenCoordinates mavenCoordinates, URL sourceUrl) {
    try {
      IPath downloadFolder = getDownloadedFilesFolder(mavenCoordinates);
      IPath path = new FileDownloader(downloadFolder).download(sourceUrl);
      return path;
    } catch (IOException e) {
      // source file is failed to download, this is not an error
      return null;
    }
  }

  /**
   * Returns the folder to which the a file corresponding to <code>mavenCoordinates</code> should be downloaded.
   * <p>
   * The folder is created as follows:
   * <code>&lt;bundle_state_location&gt;/downloads/&lt;groupId&gt;/&lt;artifactId&gt;/&lt;version&gt;</code>
   * @return the location of the download folder, may not exist
   */
  private IPath getDownloadedFilesFolder(MavenCoordinates mavenCoordinates) {
    File downloadedSources =
        Platform.getStateLocation(FrameworkUtil.getBundle(getClass()))
        .append("downloads")
        .append(mavenCoordinates.getGroupId())
        .append(mavenCoordinates.getArtifactId())
        .append(mavenCoordinates.getVersion()).toFile();
    return new Path(downloadedSources.getAbsolutePath());
  }
  private static IAccessRule[] getAccessRules(List<Filter> filters) {
    IAccessRule[] accessRules = new IAccessRule[filters.size()];
    int idx = 0;
    for (Filter filter : filters) {
      int accessRuleKind = filter.isExclude() ? IAccessRule.K_NON_ACCESSIBLE : IAccessRule.K_ACCESSIBLE;
      accessRules[idx++] = JavaCore.newAccessRule(new Path(filter.getPattern()), accessRuleKind);
    }
    return accessRules;
  }

  @Activate
  protected void activate() {
    mavenHelper = new M2EclipseMavenHelper();
    transformer = new MavenCoordinatesClasspathAttributesTransformer();
    sourceDownloaderJobFactory = new DefaultSourceDownloaderJobFactory();
  }

  private void verifyDependencies() {
    Preconditions.checkState(mavenHelper != null, "mavenHelper is null");
    Preconditions.checkState(transformer != null, "transformer is null");
    Preconditions.checkState(sourceDownloaderJobFactory != null, "sourceDownloaderJobFactory is null");
  }
  /*
   * To make sure that mavenHelper is not null in production the activate() method must be called.
   */
  @VisibleForTesting
  void setMavenHelper(MavenHelper mavenHelper) {
    this.mavenHelper = mavenHelper;
  }

  @VisibleForTesting
  void setTransformer(MavenCoordinatesClasspathAttributesTransformer transformer) {
    this.transformer = transformer;
  }

  @VisibleForTesting
  void setSourceDownloaderJobFactory(SourceDownloaderJobFactory factory) {
    sourceDownloaderJobFactory = factory;
  }
}
