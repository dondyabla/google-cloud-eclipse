package com.google.cloud.tools.eclipse.util.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import javax.inject.Inject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Test;

public class JobUtilTest {

  private static volatile boolean jobExecuted = false;
  
  @Test
  public void test() throws InterruptedException {
    TestJob job = JobUtil.createJob(TestJob.class, new JobUtil.ContextParameterSupplier() {
      @Override
      public void setParameters(IEclipseContext context) {
        context.set(Integer.class, new Integer(42));
        context.set(String.class, "test");
      }
    });
    job.schedule();
    job.join();
    assertThat(job.getResult(), is(Status.OK_STATUS));
    assertTrue(jobExecuted);
  }

  private static class TestJob extends Job {

    @Inject
    private Integer testInteger;
    @Inject
    private String testString;

    public TestJob() {
      this("test");
    }
    
    public TestJob(String name) {
      super(name);
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
