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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;

/**
 * For https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155. The purpose of the
 * class is to prevent the second ConvertJob from running. The second ConvertJob is triggered by
 * the first ConvertJob when the latter job installs the JSDT facet.
 *
 * Not recommended to use for other situations, although the workings of the class is general.
 */
public class FutureJobSuspender {
  private static boolean suspended;

  private List<Job> exceptionalJobs = new ArrayList<>();
  private List<Job> sleepingJobs = new ArrayList<>();

  private JobChangeAdaptor jobChangeListener = new JobChangeAdaptor() {
    @Override
    public void scheduled(IJobChangeEvent event) {
      if (!exceptionalJobs.contains(event.getJob())) {
        event.getJob().sleep();  // This will always succeed since the job is not running yet.
        sleepingJobs.add(event.getJob());
      }
    }
  };

  /** @param exceptionalJob job that will not be suspended */
  public synchronized void addExceptionalJob(Job exception) {
    Preconditions.checkArgument(!suspended);
    exceptionalJobs.add(exception);
  }

  public synchronized void suspendFutureJobs() {
    Preconditions.checkArgument(!suspended, "Already suspended.");
    suspended = true;
    Job.getJobManager().addJobChangeListener(jobChangeListener);
  }

  public synchronized void resume() {
    Preconditions.checkArgument(suspended, "Not suspended.");
    suspended = false;
    Job.getJobManager().removeJobChangeListener(jobChangeListener);

    for (Job job : sleepingJobs) {
      job.wakeUp();
    }
    sleepingJobs.clear();
    exceptionalJobs.clear();
  }

  /** Empty implementation of {@link IJobChangeListener} for convenience. */
  private class JobChangeAdaptor implements IJobChangeListener {
    @Override
    public void aboutToRun(IJobChangeEvent event) {}

    @Override
    public void awake(IJobChangeEvent event) {}

    @Override
    public void done(IJobChangeEvent event) {}

    @Override
    public void running(IJobChangeEvent event) {}

    @Override
    public void scheduled(IJobChangeEvent event) {}

    @Override
    public void sleeping(IJobChangeEvent event) {}
  }
};
