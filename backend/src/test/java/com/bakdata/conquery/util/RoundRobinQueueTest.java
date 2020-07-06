package com.bakdata.conquery.util;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@Slf4j
class RoundRobinQueueTest {

	private static List<Throwable> EXCEPTIONS = new ArrayList<>();

	private static ThreadFactory threadFactory =
			new ThreadFactoryBuilder()
					.setUncaughtExceptionHandler((t, e) -> {
						log.error("Exception Thread.", e);
						EXCEPTIONS.add(e);
					}).build();

	@BeforeEach
	public void reset() {
		EXCEPTIONS.clear();
	}

	@AfterEach
	public void testEmpty() {
		assertThat(EXCEPTIONS).isEmpty();
	}

	@Test
	public void test() {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>(100);

		final Queue<Integer> first = queue.createQueue();
		final Queue<Integer> second = queue.createQueue();

		first.add(1);
		second.add(2);

		assertThat(queue.contains(1)).isTrue();
		assertThat(queue.contains(2)).isTrue();
		assertThat(queue.contains(3)).isFalse();


		assertThat(queue.poll()).isEqualTo(1);
		assertThat(first).isEmpty();
		assertThat(queue.poll()).isEqualTo(2);
		assertThat(second).isEmpty();
		assertThat(queue.poll()).isEqualTo(null);

		assertThat(queue.isEmpty()).isTrue();
	}


	@Test
	public void testNewQueue() {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>(100);

		final Queue<Integer> first = queue.createQueue();
		final Queue<Integer> second = queue.createQueue();

		first.add(1);
		second.add(2);

		assertThat(queue.poll()).isEqualTo(1);
		assertThat(queue.poll()).isEqualTo(2);
		assertThat(queue.poll()).isEqualTo(null);

		final Queue<Integer> third = queue.createQueue();
		third.add(3);


		assertThat(queue.poll()).isEqualTo(3);
	}


	@Test
	public void parPutSynTake() throws InterruptedException {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>(100);

		final Queue<Integer> first = queue.createQueue();
		final Queue<Integer> second = queue.createQueue();

		first.add(1);
		second.add(2);

		threadFactory.newThread(() -> {
			try {
				TimeUnit.MILLISECONDS.sleep(100);
				first.add(3);
			}
			catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}).start();


		assertThat(queue.take()).isEqualTo(1);
		assertThat(queue.take()).isEqualTo(2);
		assertThat(queue.take()).isEqualTo(3);
	}

	@RepeatedTest(20)
	public void parPutParTake() throws InterruptedException {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>(100);

		final Set<Integer> found = Collections.synchronizedSet(new HashSet<>());

		final List<Thread> threads = new ArrayList<>();


		for (int value = 0; value < 20; value++) {

			final int _value = value;
			final Thread thread = threadFactory.newThread(() -> {
				try {
					TimeUnit.MILLISECONDS.sleep(_value % 2 == 0 ? 4 : 8);
					queue.createQueue().offer(_value);
					log.info("Offered {}", _value);
				}
				catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			});

			threads.add(thread);
			thread.start();
		}

		for (int value = 0; value < 20; value++) {
			final int _value = value;

			final Thread thread1 = threadFactory.newThread(() -> {
				try {
					TimeUnit.MILLISECONDS.sleep(_value % 2 == 0 ? 2 : 10);
					final Integer taken = queue.take();
					log.info("Received {}", taken);
					assertThat(found.add(taken))
							.describedAs("Value=%d", taken)
							.isTrue();
				}
				catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			});

			thread1.start();

			threads.add(thread1);
		}


		for (Thread thread1 : threads) {
			thread1.join(1000);

			// it's possible some threads were too late to the party.
			if (queue.isEmpty()) {
				thread1.interrupt();
			}
		}

		assertThat(queue).isEmpty();
	}

	@RepeatedTest(20)
	public void parPutParTakeRemDelayed() throws InterruptedException {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>(5);

		final Set<Integer> found = Collections.synchronizedSet(new HashSet<>());

		final List<Thread> threads = new ArrayList<>();


		for (int value = 0; value < 20; value++) {

			final int _value = value;
			final Thread thread = threadFactory.newThread(() -> {
				try {
					TimeUnit.MILLISECONDS.sleep(_value % 2 == 0 ? 2 : 10);
					final Queue<Integer> q = queue.createQueue();
					q.offer(_value);
					log.info("Offered {}", _value);
					if (_value % 3 == 0) {
						TimeUnit.MILLISECONDS.sleep(_value % 2 == 0 ? 2 : 10);

						if (q.isEmpty()) {
							log.info("Dropped {} before it was accessed", _value);
						}
						else {
							log.info("Dropped {} after it was accessed", _value);
						}

						queue.removeQueue(q);
					}
				}
				catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			});

			threads.add(thread);
			thread.start();
		}


		for (int value = 0; value < 40; value++) {
			final int _value = value;

			final Thread thread1 = threadFactory.newThread(() -> {
				try {
					TimeUnit.MILLISECONDS.sleep(_value % 2 == 0 ? 4 : 10);
					final Integer taken = queue.poll(100, TimeUnit.MILLISECONDS);
					log.info("Received {}", taken);

					if (taken == null) {
						return;
					}

					assertThat(found.add(taken))
							.describedAs("Value=%d", taken)
							.isTrue();
				}
				catch (InterruptedException e) {
					// Ignored
				}
			});

			thread1.start();

			threads.add(thread1);
		}


		for (Thread thread1 : threads) {
			thread1.join();
		}

		assertThat(queue.isEmpty()).isTrue();
	}


}