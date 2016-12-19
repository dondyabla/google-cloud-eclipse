package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public abstract class AbstractSourceAttachmentDownloaderJob extends Job {

  private long retryDelayFactor = 3000L;
  private int retryAttempts = 10;

  @Inject
  private IJavaProject javaProject;
  @Inject
  @Named("classpathEntryPath")
  private IPath classpathEntryPath;
  @Inject
  private LibraryClasspathContainerSerializer serializer;

  public AbstractSourceAttachmentDownloaderJob(String name) {
    super(name);
  }

  @VisibleForTesting
  AbstractSourceAttachmentDownloaderJob(IJavaProject javaProject, IPath classpathEntryPath,
                                        LibraryClasspathContainerSerializer serializer) {
    super("TestSourceAttachmentDownloaderJob");
    this.javaProject = javaProject;
    this.classpathEntryPath = classpathEntryPath;
    this.serializer = serializer;
  }

  @VisibleForTesting
  AbstractSourceAttachmentDownloaderJob(IJavaProject javaProject, IPath classpathEntryPath,
                                        LibraryClasspathContainerSerializer serializer, int retryAttempts,
                                        long retryDelayFactor) {
    super("TestSourceAttachmentDownloaderJob");
    this.javaProject = javaProject;
    this.classpathEntryPath = classpathEntryPath;
    this.serializer = serializer;
    this.retryAttempts = retryAttempts;
    this.retryDelayFactor = retryDelayFactor;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      IPath sourcePath = getSourcePath();
      if (sourcePath != null) {
        setSourceAttachmentPath(getJavaProject(), sourcePath, monitor);
      }
      return Status.OK_STATUS;
    } catch (IOException | CoreException ex) {
      return StatusUtil.error(this, "Could not attach source path", ex);
    }
  }

  protected abstract IPath getSourcePath();

  private void setSourceAttachmentPath(IJavaProject javaProject, IPath sourcePath, IProgressMonitor monitor) throws IOException, CoreException {
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    for (int i = 0; i < rawClasspath.length; i++) {
      IClasspathEntry entry = rawClasspath[i];
      if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
        IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), javaProject);
        if (container instanceof LibraryClasspathContainer) {
          LibraryClasspathContainer libraryContainer = (LibraryClasspathContainer) container;
          IClasspathEntry[] classpathEntries = libraryContainer.getClasspathEntries();
          for (int j = 0; j < classpathEntries.length; j++) {
            IClasspathEntry libraryEntry = libraryContainer.getClasspathEntries()[j];
            if (libraryEntry.getPath().equals(classpathEntryPath)) {
              classpathEntries[j] = JavaCore.newLibraryEntry(libraryEntry.getPath(),
                                                                           sourcePath,
                                                                           null,
                                                                           libraryEntry.getAccessRules(),
                                                                           libraryEntry.getExtraAttributes(),
                                                                           libraryEntry.isExported());
              LibraryClasspathContainer newLibraryContainer = new LibraryClasspathContainer(libraryContainer.getPath(),
                                                                                      libraryContainer.getDescription(),
                                                                                      classpathEntries);
              JavaCore.setClasspathContainer(libraryContainer.getPath(), new IJavaProject[]{ javaProject },
                                             new IClasspathContainer[]{ newLibraryContainer }, monitor);
              serializer.saveContainer(javaProject, newLibraryContainer);
              return;
            }
          }
        }
      }
    }
    if (retryAttempts-- > 0) {
      schedule((long) (Math.random() * retryDelayFactor));
    } else {
      new CoreException(StatusUtil.error(this, "Could not set source attachment path"));
    }
  }

  protected IJavaProject getJavaProject() {
    return javaProject;
  }

  @PostConstruct
  public void init() {
    setRule(javaProject.getSchedulingRule());
  }
}