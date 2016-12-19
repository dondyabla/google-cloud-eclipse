package com.google.cloud.tools.eclipse.util.jobs;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.FrameworkUtil;

public class JobUtil {

  private JobUtil() { }
  
  public interface ContextParameterSupplier {
    void setParameters(IEclipseContext context);
  }
  
  public static <J extends Job> J createJob(Class<J> jobClass, ContextParameterSupplier parameterSupplier) {
    IEclipseContext context = EclipseContextFactory 
        .getServiceContext(FrameworkUtil.getBundle(JobUtil.class).getBundleContext());
    final IEclipseContext childContext = context.createChild(jobClass.getName());
    parameterSupplier.setParameters(childContext);
    J job = ContextInjectionFactory.make(jobClass, childContext);
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        childContext.dispose();
      }
    });
    return job;
  }
}
