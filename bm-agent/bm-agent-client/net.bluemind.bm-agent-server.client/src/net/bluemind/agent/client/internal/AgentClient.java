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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.agent.BmMessage;
import net.bluemind.agent.MessageParser;
import net.bluemind.agent.client.internal.config.ClientConfig;
import net.bluemind.agent.client.internal.config.ConfigReader;
import net.bluemind.agent.client.internal.handler.HandlerRegistry;
import net.bluemind.agent.client.internal.handler.HandlerRegistry.AgentHandler;
import net.bluemind.agent.client.internal.handler.PluginLoader;
import net.bluemind.agent.client.internal.handler.PluginLoader.ClientHandler;

public class AgentClient extends Verticle {

	public static final String address = "agent.send";
	private static final int WS_FRAMESIZE = 65536 * 4;
	private WebSocket ws;
	private final MessageParser parser;

	private final Logger logger = LoggerFactory.getLogger(AgentClient.class);

	public AgentClient() {
		this.parser = new MessageParser();
	}

	@Override
	public void start() {
		logger.info("Starting BM Agent Client");
		connect();

		vertx.eventBus().registerHandler(address, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				String command = event.body().getString("command");
				String id = event.body().getString("id");
				byte[] data = event.body().getBinary("data");

				logger.info("handling message to server {}, command: {}", id, command);
				send(command, id, data);
			}
		});

	}

	private void send(String command, String id, byte[] data) {
		BmMessage message = new BmMessage();
		message.setId(id);
		message.setCommand(command);
		message.setData(data);
		try {
			Buffer buffer = new Buffer(parser.write(message));
			ws.write(buffer);
		} catch (Exception e) {
			logger.warn("Cannot send reply to client", e);
		}
	}

	private void connect() {
		ClientConfig config = ConfigReader.readConfig("bm-agent-client-config", "/etc/bm/agent/client-config.json");
		HttpClient client = vertx.createHttpClient() //
				.setMaxWebSocketFrameSize(WS_FRAMESIZE) //
				.setHost(config.host) //
				.setPort(config.port);

		client.connectWebsocket("/", (ws -> {
			logger.info("Connected to websocket");
			this.ws = ws;
			ws.dataHandler(data -> {
				String value = new String(data.getBytes());
				handleMessage(ws, value);
			});

			registerHandlers();
		}));
	}

	private void handleMessage(WebSocket ws, String value) {
		try {
			BmMessage message = parser.read(value);
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
			JsonObject obj = new JsonObject() //
					.putString("id", id) //
					.putString("command", plugin.command) //
					.asObject();

			vertx.eventBus().send(AgentClientVerticle.address_init, obj);
		});
	}

}
