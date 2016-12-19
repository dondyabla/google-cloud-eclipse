package com.google.cloud.tools.eclipse.util.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.e4.core.internal.contexts.IContextDisposalListener;
import org.junit.Test;

public class JobUtilTest {

  private volatile boolean contextDisposed = false;
  
  @Test
  public void testDependencyInjection() throws InterruptedException {
    TestJob job = JobUtil.createJob(TestJob.class, new JobUtil.ContextParameterSupplier() {
      @Override
      public void setParameters(IEclipseContext context) {
        context.set(Integer.class, new Integer(42));
        context.set(String.class, "test");
        if (context instanceof EclipseContext) {
          EclipseContext eclipseContext = (EclipseContext) context;
          eclipseContext.notifyOnDisposal(new IContextDisposalListener() {
            @Override
            public void disposed(IEclipseContext context) {
              contextDisposed = true;
            }
          });
        }
      }
    });
    // @PostConstruct is not invoked, the annotation class loaded here is different than the one Eclipse DI compares it to
//    assertTrue(job.initialized);
    job.schedule();
    job.join();
    assertThat(job.getResult(), is(Status.OK_STATUS));
    assertTrue(job.jobExecuted);
    assertTrue(contextDisposed);
  }

  private static class TestJob extends Job {

    @Inject
    private Integer testInteger;
    @Inject
    private String testString;
    private boolean jobExecuted = false;
    private boolean initialized = false;

    public TestJob() {
      this("test");
    }
    
    public TestJob(String name) {
      super(name);
    }

    @PostConstruct
    public void init() {
      initialized = true;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      if (!testInteger.equals(42)) {
        return StatusUtil.error(this, "Test integer value was " + testInteger + " expected " + 42);
      }
      if (!testString.equals("test")) {
        return StatusUtil.error(this, "Test string value was \"" + testString + "\" expected \"test\"");
      }
      jobExecuted = true;
      return Status.OK_STATUS;
    }
    
  }
}
