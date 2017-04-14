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
package net.bluemind.bm.agent.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.agent.client.AgentClientModule;
import net.bluemind.agent.client.internal.config.ClientConfig;
import net.bluemind.agent.config.SSLConfig;
import net.bluemind.agent.server.AgentServerModule;
import net.bluemind.agent.server.Command;
import net.bluemind.agent.server.internal.config.ServerConfig;

public class TestApplication {

	/*
	 * This example embeds bm-agent-server and bm-agent-client. To demonstrate
	 * the port forwarding feature, the method "startPortForwarding" will
	 * redirect localhost:8181 to localhost:22
	 */

	private static final String serverHost = "localhost";
	private static final String serverListenerAddress = "localhost";
	private static final int serverPort = 8080;

	// port forwarding config
	private static final String portForwardingTargetHost = "localhost";
	private static final int portForwardingTargetPort = 22;
	private static final int portForwardingLocalPort = 8181;

	private static final Logger logger = LoggerFactory.getLogger(TestApplication.class);

	public static void main(String[] args) throws Exception {

		TestApplication.startServer() //
				.thenCompose(TestApplication::startClient) //
				.thenRun(TestApplication::startPortForwarding) //
				.exceptionally((e) -> {
					logger.warn("Error while executing Test application", e);
					return null;
				});

		Thread.sleep(2000);
		listActiveForwardings();
		waitForQuit();
	}

	private static void waitForQuit() {
		logger.info("Application started...");
		logger.info("the port-forwarding example will open port {} on locahost and redirecting all input to {}:{}",
				portForwardingLocalPort, portForwardingTargetHost, portForwardingTargetPort);
		logger.info("Type anything to QUIT");
		try {
			System.in.read();
		} catch (IOException e) {

		}
		AgentClientModule.stopAll();
		AgentServerModule.stop();
	}

	private static CompletableFuture<Void> startServer() {
		CompletableFuture<Void> future = new CompletableFuture<>();
		String tmpDir = null;
		// activate this if you want the port-redirction to be persistent
		// String tmpDir = System.getProperty("java.io.tmpdir");
		ServerConfig serverConfig = new ServerConfig(serverListenerAddress, serverPort, SSLConfig.noSSL(), tmpDir);
		AgentServerModule.run(serverConfig, () -> {
			future.complete(null);
		});
		return future;
	}

	private static CompletableFuture<Void> startClient(Void ret) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		ClientConfig clientConfig = new ClientConfig(serverHost, serverPort, "agent-1", SSLConfig.noSSL());
		AgentClientModule.run(clientConfig, () -> {
			future.complete(null);
		});
		return future;
	}

	private static void listActiveForwardings() {
		Command command = new Command("OPTIONS", "port-redirect", "agent-1", new String[0], new HashMap<>());
		String response = AgentServerModule.command(command);
		logger.info("Active commands: {}", response);
	}

	private static void startPortForwarding() {
		Command command = portForwardingExample();
		AgentServerModule.command(command);
	}

	private static Command portForwardingExample() {
		String[] pathParameters = new String[0];
		Map<String, String> queryParameters = new HashMap<>();
		queryParameters.put("port", String.valueOf(portForwardingTargetPort));
		queryParameters.put("host", portForwardingTargetHost);
		queryParameters.put("localPort", String.valueOf(portForwardingLocalPort));
		Command command = new Command("GET", "port-redirect", "agent-1", pathParameters, queryParameters);
		return command;
	}

}
