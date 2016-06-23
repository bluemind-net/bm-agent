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
package net.bluemind.agent.client.internal;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;

import net.bluemind.agent.AgentConnection;
import net.bluemind.agent.Message;
import net.bluemind.agent.MessageParser;
import net.bluemind.agent.client.internal.HandlerRegistry.AgentHandler;
import net.bluemind.agent.client.internal.PluginLoader.ClientHandler;

public class AgentClient {

	private static final int PORT = 8086;
	private final Vertx vertx;
	private WebSocket ws;
	private final MessageParser parser;

	private final Logger logger = LoggerFactory.getLogger(AgentClient.class);

	public AgentClient() {
		this.vertx = VertxFactory.newVertx();
		this.parser = new MessageParser();
	}

	public void start() throws Exception {
		logger.info("Starting BM Agent Client");
		CountDownLatch latch = new CountDownLatch(1);
		connect(latch);
		latch.await();

		registerHandlers();

		block();
	}

	private void block() {
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void connect(final CountDownLatch latch) {
		HttpClient client = vertx.createHttpClient().setHost("localhost").setPort(PORT);

		client.connectWebsocket("/", (ws -> {
			logger.info("Connected to websocket");
			this.ws = ws;
			ws.dataHandler(data -> {
				String value = new String(data.getBytes());
				handleMessage(ws, value);
			});

			latch.countDown();
		}));
	}

	private void handleMessage(WebSocket ws, String value) {
		try {
			Message message = parser.read(value);
			logger.info("Incoming Message: {}", message);
			Optional<AgentHandler> handler = HandlerRegistry.getInstance().get(message.getCommand());
			handler.ifPresent(h -> {
				logger.info("Found handler {} for command {}", h.info, message.getCommand());
				h.handler.onMessage(message.getData());
			});
		} catch (Exception e) {
			logger.warn("Error while handling message", e);
		}

	}

	private void registerHandlers() {
		List<ClientHandler> plugins = PluginLoader.load();
		plugins.forEach(plugin -> {
			logger.info("Registering plugin {} for command {}", plugin.name, plugin.command);
			String id = UUID.randomUUID().toString();
			HandlerRegistry.getInstance().register(plugin.command, plugin.handler, plugin.name);
			plugin.handler.onInitialize(new AgentConnection<WebSocket>(plugin.command, id, ws));
		});
	}

}
