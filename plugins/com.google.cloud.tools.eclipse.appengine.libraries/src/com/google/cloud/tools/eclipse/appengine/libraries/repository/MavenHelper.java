package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public interface MavenHelper {
  Artifact resolveArtifact(IProgressMonitor monitor, MavenCoordinates coordinates) throws CoreException;

  IPath getMavenSourceJarLocation(MavenCoordinates mavenCoordinates);
}