package com.google.cloud.tools.eclipse.appengine.libraries.repository.impl;

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.MavenHelper;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

public class M2EclipseMavenHelper implements MavenHelper {

  @Override
  public Artifact resolveArtifact(IProgressMonitor monitor,
                                  MavenCoordinates mavenCoordinates) throws CoreException {
    List<ArtifactRepository> repository = getRepository(mavenCoordinates);
    return MavenUtils.resolveArtifact(null, mavenCoordinates.getGroupId(), mavenCoordinates.getArtifactId(),
                                      mavenCoordinates.getType(), mavenCoordinates.getVersion(),
                                      mavenCoordinates.getClassifier(), repository);
  }

  @Override
  public IPath getMavenSourceJarLocation(MavenCoordinates mavenCoordinates) {
    try {
      MavenCoordinates sourceMavenCoordinates = new MavenCoordinates(mavenCoordinates);
      sourceMavenCoordinates.setClassifier("sources");
      Artifact artifact = resolveArtifact(null, sourceMavenCoordinates);
      return new Path(artifact.getFile().getAbsolutePath());
    } catch (CoreException exception) {
      // source file failed to download, this is not an error
      return null;
    }
  }
  
  private List<ArtifactRepository> getRepository(MavenCoordinates mavenCoordinates) throws CoreException {
    if (MavenCoordinates.MAVEN_CENTRAL_REPO.equals(mavenCoordinates.getRepository())) {
      // M2Eclipse will use the Maven Central repo in case null is used
      return null;
    } else {
      return Collections.singletonList(getCustomRepository(mavenCoordinates.getRepository()));
    }
  }

  private ArtifactRepository getCustomRepository(String repository) throws CoreException {
    try {
      URI repoUri = new URI(repository);
      if (!repoUri.isAbsolute()) {
        throw new CoreException(StatusUtil.error(this, NLS.bind(Messages.RepositoryUriNotAbsolute, repository)));
      }
      return createArtifactRepository(repoUri.getHost(), repoUri.toString());
    } catch (URISyntaxException exception) {
      throw new CoreException(StatusUtil.error(this, NLS.bind(Messages.RepositoryUriInvalid, repository), exception));
    }
  }

  private ArtifactRepository createArtifactRepository(String id, String url) throws CoreException {
    return MavenUtils.createRepository(id, url);
  }
}