package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.util.io.FileDownloader;
import java.io.IOException;
import java.net.URL;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.core.runtime.IPath;

public class RemoteFileSourceAttachmentDownloaderJob extends AbstractSourceAttachmentDownloaderJob {

  @Inject
  @Named("downloadFolder")
  private IPath downloadFolder;
  @Inject
  private URL sourceUrl;
  
  public RemoteFileSourceAttachmentDownloaderJob() {
    super(Messages.RemoteFileSourceAttachmentDownloaderJobName);
  }

  @Override
  protected IPath getSourcePath() {
    try {
      IPath path = new FileDownloader(downloadFolder).download(sourceUrl);
      return path;
    } catch (IOException e) {
      // source file is failed to download, this is not an error
      return null;
    }
  }

}
