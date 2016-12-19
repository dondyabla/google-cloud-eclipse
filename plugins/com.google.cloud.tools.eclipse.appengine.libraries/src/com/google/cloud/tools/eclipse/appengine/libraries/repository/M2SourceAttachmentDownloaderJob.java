package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.common.annotations.VisibleForTesting;
import javax.inject.Inject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;

public class M2SourceAttachmentDownloaderJob extends AbstractSourceAttachmentDownloaderJob {

  @Inject
  protected MavenCoordinates mavenCoordinates;
  @Inject
  private MavenHelper mavenHelper;

  public M2SourceAttachmentDownloaderJob() {
    super(Messages.M2SourceAttachmentDownloaderJobName);
  }
  
  @VisibleForTesting
  M2SourceAttachmentDownloaderJob(IJavaProject javaProject, IPath classpathEntryPath,
                                  MavenCoordinates mavenCoordinates, MavenHelper mavenHelper,
                                  LibraryClasspathContainerSerializer serializer) {
    super(javaProject, classpathEntryPath, serializer);
    this.mavenCoordinates = mavenCoordinates;
    this.mavenHelper = mavenHelper;
  }

  @Override
  protected IPath getSourcePath() {
    return mavenHelper.getMavenSourceJarLocation(mavenCoordinates);
  }
}
