package com.google.cloud.tools.eclipse.util.jobs;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
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

  /**
   * Creates a new instance of the given job that is initialized using E4 dependency injection.
   * <p>
   * Adds an {@link IJobChangeListener} to the job to ensure the disposal of the {@link IEclipseContext} used for
   * dependency injection upon completion of the job (on the <code>done</code> event).
   * 
   * @param jobClass the class to instantiate the result from
   * @param parameterSupplier can provide additional parameters into the context used for dependency injection
   */
  public static <J extends Job> J createJob(Class<J> jobClass, ContextParameterSupplier parameterSupplier) {
    IEclipseContext context = EclipseContextFactory 
        .getServiceContext(FrameworkUtil.getBundle(JobUtil.class).getBundleContext());
    final IEclipseContext childContext = context.createChild(jobClass.getName());
    if (parameterSupplier != null) {
      parameterSupplier.setParameters(childContext);
    }
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
