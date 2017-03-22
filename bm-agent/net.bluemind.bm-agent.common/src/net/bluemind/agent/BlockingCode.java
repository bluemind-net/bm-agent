/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.agent;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Context;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Helper inspired by vertx 3 to run blocking code from an event loop context.
 *
 */
public class BlockingCode {

	private static final Logger logger = LoggerFactory.getLogger(BlockingCode.class);

	private final ListeningExecutorService workerService;

	private final ExecutorService pool;

	private final Executor contextExecutor;

	public BlockingCode() {
		this(VertxHolder.DEFAULT);
	}

	/**
	 * Provide your own executor where the blocking code will run.
	 * 
	 * @param pool
	 * @return
	 */
	public BlockingCode(String agentId) {
		this.pool = Executors.newFixedThreadPool(5);
		this.workerService = MoreExecutors.listeningDecorator(pool);
		final Context context = VertxHolder.getVertx(agentId).currentContext();
		Executor contextExecutor = command -> {
			context.runOnContext(theVoid -> {
				logger.debug("Executing completion in the originating vertx context.");
				command.run();
			});
		};
		this.contextExecutor = contextExecutor;
	}

	/**
	 * Returns a future that will execute its followers in the vertx context
	 * associated with this instance
	 * 
	 * @param supplier
	 * @return a future for with the result of the given supplier
	 */
	public <T> CompletableFuture<T> run(Supplier<T> supplier) {
		Objects.requireNonNull(workerService,
				"You must supply an executor service through withExecutor before calling run.");
		CompletableFuture<T> result = new CompletableFuture<>();
		ListenableFuture<T> listenable = workerService.submit(() -> supplier.get());
		Futures.addCallback(listenable, new FutureCallback<T>() {

			@Override
			public void onSuccess(T value) {
				result.complete(value);
			}

			@Override
			public void onFailure(Throwable t) {
				logger.error("Complete exceptionally: ", t.getMessage(), t);
				result.completeExceptionally(t);
			}
		}, contextExecutor);
		return result;
	}

}
