package com.bakdata.conquery.models.worker;

import java.util.Collections;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.messages.network.SlaveMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

public class SlaveInformation extends MessageSender.Simple<SlaveMessage> {
	@JsonIgnore @Getter
	private transient JobManagerStatus jobManagerStatus = new JobManagerStatus(Collections.emptySet());
	@JsonIgnore
	private final transient Object jobManagerSync = new Object();
	
	public SlaveInformation(NetworkSession session) {
		super(session);
	}

	public void setJobManagerStatus(JobManagerStatus status) {
		this.jobManagerStatus = status;
		if (status.size() < 100) {
			synchronized (jobManagerSync) {
				jobManagerSync.notifyAll();
			}
		}
	}

	public void waitForFreeJobqueue() throws InterruptedException {
		if (jobManagerStatus.size() >= 100) {
			synchronized (jobManagerSync) {
				jobManagerSync.wait();
			}
		}
	}
}
