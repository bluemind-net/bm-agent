/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.agent.server;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import net.bluemind.agent.VertxHolder;
import net.bluemind.agent.server.internal.AgentServer;
import net.bluemind.agent.server.internal.AgentServerVerticle;
import net.bluemind.agent.server.internal.config.ConfigReader;
import net.bluemind.agent.server.internal.config.ServerConfig;
import net.bluemind.agent.server.internal.handler.CommandReplyHandler;

public class AgentServerModule implements BundleActivator {

	private static Logger logger = LoggerFactory.getLogger(AgentServerModule.class);

	@Override
	public void start(BundleContext context) throws Exception {
		logger.info("Starting BlueMind Agent Server");

		try {
			ServerConfig config = ConfigReader.readConfig("bm-agent-server-config", "/etc/bm/agent/server-config.json");
			deployVerticles(config, () -> logger.info("Agent Server is running..."));
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("Could not start bm-agent-server", e);
			throw e;
		}
	}

	private void deployVerticles(ServerConfig config, Runnable doneHandler) {
		PlatformManager pm = PlatformLocator.factory.createPlatformManager();
		VertxHolder.vertices.put(VertxHolder.DEFAULT, pm.vertx());
		VertxHolder.pms.put(VertxHolder.DEFAULT, pm);
		CompletableFuture<String> doneAgent = new CompletableFuture<>();
		CompletableFuture<String> doneCommHandler = new CompletableFuture<>();
		pm.deployVerticle("net.bluemind.agent.server.internal.AgentServer", config.toJsonObject(), new URL[0], 1, null,
				(s) -> doneAgent.complete(s.result()));
		pm.deployVerticle("net.bluemind.agent.server.internal.AgentServerVerticle", null, new URL[0], 1, null,
				(s) -> doneCommHandler.complete(s.result()));
		doneAgent.thenAcceptBoth(doneCommHandler, (a, b) -> {
			logger.info("Deployed verticles {} and {}", a, b);
			doneHandler.run();
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		logger.info("Stopping BlueMind Agent Server");
	}

	public static void main(String[] args) throws Exception {
		new AgentServerModule().start(null);
	}

	/*
	 * methods used in library mode
	 */

	public static void run(ServerConfig config, Runnable doneHandler) {
		new AgentServerModule().deployVerticles(config, doneHandler);
	}

	public static String command(Command command) {
		// use some high id to not interfere with vert.x timer ids
		command.id = Long.MAX_VALUE - Math.round((Math.random() * Integer.MAX_VALUE));
		VertxHolder.vertices.get(VertxHolder.DEFAULT).eventBus().send(AgentServerVerticle.address_command,
				command.toJsonObject());
		CountDownLatch await = new CountDownLatch(1);
		AtomicReference<String> ref = new AtomicReference<>();
		CommandReplyHandler replyHandler = new CommandReplyHandler(ref, command.id, await);
		VertxHolder.vertices.get(VertxHolder.DEFAULT).eventBus().registerHandler(AgentServer.address_command_reply,
				replyHandler);
		try {
			await.await(30000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
		return ref.get();
	}

	public static void stop() {
		VertxHolder.pms.keySet()
				.forEach((id) -> VertxHolder.pms.get(id).undeployAll((r) -> VertxHolder.vertices.get(id).stop()));
		VertxHolder.reset();
	}

}
