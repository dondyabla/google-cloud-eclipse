package com.google.cloud.tools.eclipse.appengine.libraries.repository.impl;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.M2SourceAttachmentDownloaderJob;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.MavenHelper;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.RemoteFileSourceAttachmentDownloaderJob;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.SourceDownloaderJobFactory;
import com.google.cloud.tools.eclipse.util.io.FileDownloader;
import com.google.cloud.tools.eclipse.util.jobs.JobUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.framework.FrameworkUtil;

public class DefaultSourceDownloaderJobFactory implements SourceDownloaderJobFactory {

  @Override
  public Job createM2SourceDownloaderJob(final IJavaProject javaProject, final MavenCoordinates mavenCoordinates,
      final IPath classpathEntryPath, final MavenHelper mavenHelper) {
    return JobUtil.createJob(M2SourceAttachmentDownloaderJob.class, new JobUtil.ContextParameterSupplier() {
      @Override
      public void setParameters(IEclipseContext context) {
        context.set(IJavaProject.class, javaProject);
        context.set(MavenCoordinates.class, mavenCoordinates);
        context.set("classpathEntryPath", classpathEntryPath);
        context.set(MavenHelper.class, mavenHelper);
      }
    });
  }

  @Override
  public Job createRemoteFileSourceDownloaderJob(final IJavaProject javaProject, final IPath classpathEntryPath,
      final MavenCoordinates mavenCoordinates, final URL sourceUrl) {
    return JobUtil.createJob(RemoteFileSourceAttachmentDownloaderJob.class, new JobUtil.ContextParameterSupplier() {
      @Override
      public void setParameters(IEclipseContext context) {
        context.set(IJavaProject.class, javaProject);
        context.set("classpathEntryPath", classpathEntryPath);
        context.set("downloadFolder", getDownloadedFilesFolder(mavenCoordinates));
        context.set(URL.class, sourceUrl);
      }
    });
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
}
