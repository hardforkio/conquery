package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.bakdata.conquery.metrics.JobMetrics;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.codahale.metrics.Timer;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobExecutor extends Thread {

	private final LinkedBlockingDeque<Job> jobs = new LinkedBlockingDeque<>();
	private final AtomicReference<Job> currentJob = new AtomicReference<>();
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final AtomicBoolean busy = new AtomicBoolean(false);

	public JobExecutor(String name) {
		super(name);
		JobMetrics.createJobQueueGauge(name, jobs);
	}

	public void add(Job job) {
		if(closed.get()) {
			throw new IllegalStateException("Tried to add a job to a closed JobManager");
		}
		jobs.add(job);
	}

	public boolean cancelJob(UUID jobId) {
		for (Job job1 : jobs) {
			if (job1.getJobId().equals(jobId)) {
				job1.cancel();

				return true;
			}
		}

		final Job job = currentJob.get();

		if(job != null && job.getJobId().equals(jobId)){
			job.cancel();
			return true;
		}

		return false;
	}

	public List<Job> getJobs() {
		List<Job> jobs = new ArrayList<>(this.jobs.size() + 1);
		Job current = currentJob.get();
		if (current != null) {
			jobs.add(current);
		}
		jobs.addAll(this.jobs);
		return jobs;
	}
	
	public boolean isBusy() {
		return busy.get() || !jobs.isEmpty();
	}

	public void close() {
		closed.set(true);
		Uninterruptibles.joinUninterruptibly(this);
		JobMetrics.removeJobQueueSizeGauge(getName());
	}

	@Override
	public void run() {
		ConqueryMDC.setLocation(this.getName());

		while(!closed.get()) {
			Job job;
			try {
				while((job =jobs.poll(100, TimeUnit.MILLISECONDS))!=null) {
					busy.set(true);
					currentJob.set(job);
					job.getProgressReporter().start();
					Stopwatch timer = Stopwatch.createStarted();

					final Timer.Context time = JobMetrics.getJobExecutorTimer(job);

					try {
						if(job.isCancelled()){
							log.trace("{} skipping cancelled job {}", this.getName(), job);
							continue;
						}

						log.trace("{} started job {} with Id {}", this.getName(), job, job.getJobId());
						ConqueryMDC.setLocation(this.getName());
						job.execute();
						ConqueryMDC.setLocation(this.getName());

					}
					catch (Throwable e) {
						ConqueryMDC.setLocation(this.getName());

						log.error("Job "+job+" failed", e);
					}finally {
						ConqueryMDC.setLocation(this.getName());

						log.trace("Finished job {} within {}", job, timer.stop());
						time.stop();
					}
				}
				busy.set(false);
				currentJob.set(null);
			} catch (InterruptedException e) {
				log.warn("Interrupted JobManager polling", e);
			}
		}
	}

}
