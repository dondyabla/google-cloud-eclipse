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

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.cloud.tools.eclipse.util.templates.appengine.AppEngineTemplateUtility;
import com.google.common.base.Stopwatch;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class StandardFacetInstallDelegate extends AppEngineFacetInstallDelegate {
  private final static String APPENGINE_WEB_XML = "appengine-web.xml";

  @Override
  public void execute(IProject project,
                      IProjectFacetVersion version,
                      Object config,
                      IProgressMonitor monitor) throws CoreException {
    super.execute(project, version, config, monitor);
    createConfigFiles(project, monitor);
    installAppEngineRuntimes(project);
  }

  private void installAppEngineRuntimes(IProject project) throws CoreException {
    ConvertJob simulatedConvertJob = new ConvertJob(project);
    simulatedConvertJob.schedule();

    // Modifying targeted runtimes while installing/uninstalling facets is not allowed,
    // so schedule a job as a workaround.
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    final Job installJob = new InstallAppEngineRuntimesJob(facetedProject);
    installJob.schedule();
  }

  /**
   * Creates an appengine-web.xml file in the WEB-INF folder if it doesn't exist
   */
  private void createConfigFiles(IProject project, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);

    // The virtual component model is very flexible, but we assume that
    // the WEB-INF/appengine-web.xml isn't a virtual file remapped elsewhere
    IFolder webInfDir = WebProjectUtil.getWebInfDirectory(project);
    IFile appEngineWebXml = webInfDir.getFile(APPENGINE_WEB_XML);

    if (appEngineWebXml.exists()) {
      return;
    }

    ResourceUtils.createFolders(webInfDir, progress.newChild(2));

    appEngineWebXml.create(new ByteArrayInputStream(new byte[0]), true, progress.newChild(2));
    String configFileLocation = appEngineWebXml.getLocation().toString();
    AppEngineTemplateUtility.createFileContent(
        configFileLocation, AppEngineTemplateUtility.APPENGINE_WEB_XML_TEMPLATE,
        Collections.<String, String>emptyMap());
    progress.worked(6);
  }

  Semaphore waitUntilConvertJobTakesEmptySnapshot = new Semaphore(0);
  Semaphore waitUntilInstallJobInstallsFacet = new Semaphore(0);

  private class InstallAppEngineRuntimesJob extends WorkspaceJob {
    private IFacetedProject project;
    private IFacetedProjectWorkingCopy snapshot;
    private long delayTime = 50;
    private Stopwatch timer = Stopwatch.createStarted();

    InstallAppEngineRuntimesJob(IFacetedProject project) {
      super("Install App Engine runtimes in " + project.getProject().getName());
      this.project = project;
      this.snapshot = project.createWorkingCopy();
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
      if (waitUntilConvertJobTakesEmptySnapshot.availablePermits() == 0) {
        System.out.println("ConvertJob hasn't taken a snapshot yet. Will wail until it does.");
      }
      waitUntilConvertJobTakesEmptySnapshot.acquireUninterruptibly();

      try {
        IProjectFacet jsdt = ProjectFacetsManager.getProjectFacet("wst.jsdt.web");
        if (snapshot.isDirty() || !snapshot.hasProjectFacet(jsdt)) {
          System.out.printf(">> InstallAppEngineRuntimesJob[%s]: is dirty, will try again\n",
              timer);
          snapshot.revertChanges();
          schedule(delayTime);
          return Status.OK_STATUS;
        }

        System.out.printf(">> InstallAppEngineRuntimesJob[%s]: GO: installing runtimes\n", timer);
        AppEngineStandardFacet.installAllAppEngineRuntimes(project, monitor);
        snapshot.dispose();

        System.out.println("InstallJob: installed App Engine runtime.");
        System.out.println("InstallJob: will wake up ConvertJob to overwrite the runtime list.");
        waitUntilInstallJobInstallsFacet.release();

        return Status.OK_STATUS;
      } catch (CoreException ex) {
        return ex.getStatus();
      }
    }
  };

  class ConvertJob extends WorkspaceJob {
    //final static String JSDT_FACET = "wst.jsdt.web";
    private IProject fProject;
    //private boolean fInstall = true;
    //private boolean fUseExplicitWorkingCopy = false;

    ConvertJob(IProject project /*, boolean install, boolean useExplicitWorkingCopy */) {
      super("My simulated ConvertJob");
      fProject = project;
      //fInstall = install;
      //fUseExplicitWorkingCopy = useExplicitWorkingCopy;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
      //try {
        //IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(JSDT_FACET);
        IFacetedProject facetedProject = ProjectFacetsManager.create(fProject);

        //if (facetedProject != null && fProject.isAccessible()) {
        //  if (fInstall) {
        //    IProjectFacetVersion latestVersion = projectFacet.getLatestVersion();
        //    facetedProject.installProjectFacet(latestVersion, null, monitor);
        //  }


        //  if (fUseExplicitWorkingCopy) {
            IFacetedProjectWorkingCopy copy = facetedProject.createWorkingCopy();
        //    Set fixed = new HashSet(facetedProject.getFixedProjectFacets());
        //    fixed.add(projectFacet);
        //    copy.setFixedProjectFacets(fixed);

            System.out.println("ConvertJob: took a snapshot with an empty runtime list.");
            assert (copy.getTargetableRuntimes().isEmpty());
            waitUntilConvertJobTakesEmptySnapshot.release(99999);

            System.out.println("ConvertJob: will wait until installJob installs App Engine runtime.");
            waitUntilInstallJobInstallsFacet.acquireUninterruptibly();

            copy.commitChanges(new NullProgressMonitor());
            copy.dispose();

            System.out.println("ConvertJob: reset the runtime list. It's empty now.");
            waitUntilInstallJobInstallsFacet.acquireUninterruptibly();
        //  }
        //  else {
        //    Set fixed = new HashSet(facetedProject.getFixedProjectFacets());
        //    if (!fixed.contains(projectFacet)) {
        //      fixed.add(projectFacet);
        //      facetedProject.setFixedProjectFacets(fixed);
        //    }
        //  }
        //}
      //}
      //catch (IllegalArgumentException e) {
        // unknown facet ID, bad installation configuration?
      //}
      //catch (Exception e) {
      //  Logger.logException(e);
      //}
      return Status.OK_STATUS;
    }
  }
}
