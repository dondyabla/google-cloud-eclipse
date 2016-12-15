/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

/**
 * Utility classes for processing WTP Web Projects (jst.web and jst.utility).
 */
public class WebProjectUtil {
  // This is the folder created by default when the Dynamic Web facet is installed
  // (unless the user changes the default.)
  private static final String DEFAULT_DYNAMIC_WEB_FACET_WEB_INF_PATH = "WebContent/WEB-INF/";

  private final static String DEFAULT_WEB_PATH = "src/main/webapp";

  private final static String WEB_INF = "WEB-INF/";

  /**
   * Return the project's <code>WEB-INF</code> directory. There is no guarantee that the contents
   * are actually published.
   *
   * @return the <code>IFolder</code> or null if not present
   */
  public static IFolder getWebInfDirectory(IProject project) {
    // Try to obtain the directory as if it was a Dynamic Web Project
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      IVirtualFolder root = component.getRootFolder();
      // the root should exist, but the WEB-INF may not yet exist
      if (root.exists()) {
        return (IFolder) root.getFolder(WEB_INF).getUnderlyingFolder();
      }
    }
    // Otherwise it's seemingly fair game
    IFolder defaultLocation = project.getFolder(DEFAULT_WEB_PATH).getFolder(WEB_INF);
    if (defaultLocation.exists()) {
      return defaultLocation;
    }
    return null;
  }

  /**
   * Attempt to resolve the given file within the project's <code>WEB-INF</code>.
   *
   * @return the file location or {@code null} if not found
   */
  public static IFile findInWebInf(IProject project, IPath filePath) {
    IFolder webInfFolder = getWebInfDirectory(project);
    if (webInfFolder == null) {
      return null;
    }
    IFile file = webInfFolder.getFile(filePath);
    return file.exists() ? file : null;
  }

  static boolean isDefaultWebContentFolderBogus(IProject project) {
    IFolder webFacetDefaultWebInf = project.getFolder(DEFAULT_DYNAMIC_WEB_FACET_WEB_INF_PATH);
    if (!webFacetDefaultWebInf.exists()) {
      return false;
    }
    if (findAllWebInfFolders(project).size() == 1) {
      return false;
    }

    IFolder webFacetDefaultWebContent = (IFolder) webFacetDefaultWebInf.getParent();
    if (webFacetDefaultWebContent contains no other files than "WEB-INF/web.xml" and "META-INF/MANIFEST.MF"
        && "web.xml" contains no element, i.e., empty) {
      return true;
    }
    return false;
  }

  private static List<IFolder> findAllWebInfFolders(IContainer container) {
    List<IFolder> webInfFolders = new ArrayList<>();
    try {
      for (IResource resource : container.members()) {
        if (resource.exists() && resource.getType() == IResource.FOLDER) {
          if ("WEB-INF".equals(resource.getName())) {
            webInfFolders.add((IFolder) resource);
          } else {
            webInfFolders.addAll(findAllWebInfFolders((IFolder) resource));
          }
        }
      }
    } catch (CoreException ex) {}
    return webInfFolders;
  }

}
