package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;

public interface SourceDownloaderJobFactory {

  public Job createM2SourceDownloaderJob(IJavaProject javaProject, MavenCoordinates mavenCoordinates, IPath classpathEntryPath, MavenHelper mavenHelper);
  
  public Job createRemoteFileSourceDownloaderJob(IJavaProject javaProject, IPath classpathEntryPath, MavenCoordinates mavenCoordinates, URL sourceUrl);
}
