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
import org.eclipse.jdt.core.JavaModelException;

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

  /**
   * Finds the library entry with path matching {@link #classpathEntryPath} and sets the source attachment path to
   * <code>sourcePath</code>
   */
  private void setSourceAttachmentPath(IJavaProject javaProject, IPath sourcePath, IProgressMonitor monitor) throws IOException, CoreException {
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    for (int i = 0; i < rawClasspath.length; i++) {
      // this method can return null for uninitialized LibraryClasspathContainer instances, in that case
      // this jobs gets rescheduled to let JDT initialize the container in the meantime
      LibraryClasspathContainer libraryContainer = getLibraryClasspathContainer(rawClasspath[i]);
      if (libraryContainer != null) {
        IClasspathEntry[] classpathEntries = libraryContainer.getClasspathEntries();
        for (int j = 0; j < classpathEntries.length; j++) {
          if (classpathEntries[j].getPath().equals(classpathEntryPath)) {
            setSourceAttachmentPathForEntry(javaProject, sourcePath, monitor, libraryContainer, classpathEntries, j);
            return;
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

  /**
   * Returns the {@link LibraryClasspathContainer} corresponding to the {@link IClasspathEntry} if one exists, otherwise
   * <code>null</code>
   */
  private LibraryClasspathContainer getLibraryClasspathContainer(IClasspathEntry entry) throws JavaModelException {
    if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
      IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), javaProject);
      if (container instanceof LibraryClasspathContainer) {
        return (LibraryClasspathContainer) container;
      }
    }
    return null;
  }

  private void setSourceAttachmentPathForEntry(IJavaProject javaProject, IPath sourcePath, IProgressMonitor monitor,
      LibraryClasspathContainer libraryContainer, IClasspathEntry[] classpathEntries, int j)
      throws JavaModelException, IOException, CoreException {
    classpathEntries[j] = JavaCore.newLibraryEntry(classpathEntries[j].getPath(),
                                                   sourcePath,
                                                   null,
                                                   classpathEntries[j].getAccessRules(),
                                                   classpathEntries[j].getExtraAttributes(),
                                                   classpathEntries[j].isExported());
    LibraryClasspathContainer newLibraryContainer = new LibraryClasspathContainer(libraryContainer.getPath(),
                                                                                  libraryContainer.getDescription(),
                                                                                  classpathEntries);
    JavaCore.setClasspathContainer(libraryContainer.getPath(), new IJavaProject[]{ javaProject },
                                   new IClasspathContainer[]{ newLibraryContainer }, monitor);
    serializer.saveContainer(javaProject, newLibraryContainer);
  }

  protected IJavaProject getJavaProject() {
    return javaProject;
  }

  @PostConstruct
  public void init() {
    setRule(javaProject.getSchedulingRule());
  }
}