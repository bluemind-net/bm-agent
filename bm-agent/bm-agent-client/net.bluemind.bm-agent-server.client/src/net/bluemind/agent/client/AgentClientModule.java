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
package net.bluemind.agent.client;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import net.bluemind.agent.VertxHolder;
import net.bluemind.agent.client.internal.config.ClientConfig;
import net.bluemind.agent.client.internal.config.ConfigReader;

public class AgentClientModule implements BundleActivator {

	private static Logger logger = LoggerFactory.getLogger(AgentClientModule.class);

	@Override
	public void start(BundleContext context) throws Exception {
		logger.info("Starting BlueMind Agent Client");

		ClientConfig config = ConfigReader.readConfig("bm-agent-client-config", "/etc/bm/agent/client-config.json");
		deployVerticles(config, () -> logger.info("Agent Client is running..."));

	}

	private void deployVerticles(ClientConfig config, Runnable doneHandler) {
		PlatformManager pm = PlatformLocator.factory.createPlatformManager();
		VertxHolder.vertices.put(config.agentId, pm.vertx());
		VertxHolder.pms.put(config.agentId, pm);
		CompletableFuture<String> doneAgent = new CompletableFuture<>();
		CompletableFuture<String> doneCommHandler = new CompletableFuture<>();
		pm.deployVerticle("net.bluemind.agent.client.internal.AgentClient", config.toJsonObject(), new URL[0], 1, null,
				(s) -> doneAgent.complete(s.result()));
		pm.deployVerticle("net.bluemind.agent.client.internal.AgentClientVerticle", null, new URL[0], 1, null,
				(s) -> doneCommHandler.complete(s.result()));
		doneAgent.thenAcceptBoth(doneCommHandler, (a, b) -> {
			logger.info("Deployed verticles {} and {}", a, b);
			doneHandler.run();
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		logger.info("Stopping BlueMind Agent Client");
	}

	/*
	 * methods used in library mode
	 */

	public static void run(ClientConfig config, Runnable doneHandler) {
		new AgentClientModule().deployVerticles(config, doneHandler);
	}

	public static void stop() {
		VertxHolder.pms.keySet()
				.forEach((id) -> VertxHolder.pms.get(id).undeployAll((r) -> VertxHolder.vertices.get(id).stop()));
		VertxHolder.reset();
	}

}
