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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IPrimaryRuntimeChangedEvent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

/**
 * Listens for events where the App Engine standard runtime has been made the primary
 * runtime for a project. Installs the App Engine facet if necessary.
 */
public class AppEngineStandardRuntimeChangeListener implements IFacetedProjectListener {

  @Override
  public void handleEvent(IFacetedProjectEvent event) {
    // PRIMARY_RUNTIME_CHANGED occurs in scenarios such as selecting runtimes on the
    // "New Faceted Project" wizard and the "New Dynamic Web Project" wizard.
    // IFacetedProjectEvent.Type.TARGETED_RUNTIMES_CHANGED does not happen
    System.out.printf("AppEngineStandardRuntimeChangeListener: event=%s\n", event.getType());
    if (event.getType() != IFacetedProjectEvent.Type.PRIMARY_RUNTIME_CHANGED) {
      return;
    }

    IPrimaryRuntimeChangedEvent runtimeChangeEvent = (IPrimaryRuntimeChangedEvent)event;
    final IRuntime newRuntime = runtimeChangeEvent.getNewPrimaryRuntime();
    System.out.printf("AppEngineStandardRuntimeChangeListener: newRuntime=%s\n", newRuntime);
    if (newRuntime == null) {
      return;
    }

    if (!AppEngineStandardFacet.isAppEngineStandardRuntime(newRuntime)) {
      System.out.printf("AppEngineStandardRuntimeChangeListener: skipping: not an AES Runtime\n",
          newRuntime);
      return;
    }

    // Check if the App Engine facet has been installed in the project
    final IFacetedProject facetedProject = runtimeChangeEvent.getProject();
    if (AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
      System.out.printf(
          "AppEngineStandardRuntimeChangeListener: skipping as project has AppEngineFacet: %s\n",
          facetedProject);
      return;
    }

    // Add the App Engine facet
    IProject project = facetedProject.getProject();
    Job addFacetJob = new Job("Add App Engine facet to " + project.getName()) {

      @Override
      protected IStatus run(IProgressMonitor monitor) {

        IStatus installStatus = Status.OK_STATUS;

        try {
          System.out.printf(
              ">> AppEngineStandardRuntimeChangeListener: about to installAppEngineFacet(%s, noDepFacets)\n",
              facetedProject);
          AppEngineStandardFacet.installAppEngineFacet(facetedProject, false /* installDependentFacets */, monitor);
          System.out.printf(
              "<< OK: installAppEngineFacet(%s, noDepFacets)\n",
              facetedProject);
          return installStatus;
        } catch (CoreException ex1) {
          System.out.printf("++ EX: installAppEngineFacet: %s\n", ex1);
          // Displays missing constraints that prevented facet installation
          installStatus = ex1.getStatus();
          System.out.printf(
              "++ EX: installAppEngineFacet: installStatus=%s\n",
              installStatus);

          // Remove App Engine as primary runtime
          try {
            System.out.printf(">>> About to remove targeted runtime: %s\n", newRuntime);
            facetedProject.removeTargetedRuntime(newRuntime, monitor);
            System.out.printf("<<< OK: removed targeted runtime: %s\n", newRuntime);
            return installStatus;
          } catch (CoreException ex2) {
            System.out.printf("+++ EX: unable to targeted runtime: %s\n", ex2);
            MultiStatus multiStatus;
            if (installStatus instanceof MultiStatus) {
              multiStatus = (MultiStatus) installStatus;
            } else {
              multiStatus = new MultiStatus(installStatus.getPlugin(), installStatus.getCode(),
                  installStatus.getMessage(), installStatus.getException());
            }
            multiStatus.merge(ex2.getStatus());
            return multiStatus;
          }
        }

      }
    };
    addFacetJob.schedule();
  }

}
