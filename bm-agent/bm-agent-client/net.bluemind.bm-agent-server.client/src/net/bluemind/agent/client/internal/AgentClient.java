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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.agent.BmMessage;
import net.bluemind.agent.MessageParser;
import net.bluemind.agent.client.internal.config.ClientConfig;
import net.bluemind.agent.client.internal.handler.HandlerRegistry;
import net.bluemind.agent.client.internal.handler.PluginLoader;
import net.bluemind.agent.client.internal.handler.PluginLoader.ClientHandler;

public class AgentClient extends Verticle {

	public static final String address = "agent.send";
	private static final int WS_FRAMESIZE = 65536 * 4;
	private WebSocket ws;
	private final MessageParser parser;
	private ClientConfig config;
	private boolean connected = false;
	private boolean connecting = false;

	private final Logger logger = LoggerFactory.getLogger(AgentClient.class);

	public AgentClient() {
		this.parser = new MessageParser();
	}

	@Override
	public void start() {
		logger.info("Starting BM Agent Client");
		config = new ClientConfig(container.config());
		registerHandlers();
		connect();

		vertx.eventBus().registerHandler(address, (Message<JsonObject> event) -> {
			String command = event.body().getString("command");
			String commandId = event.body().getString("commandId");
			byte[] data = event.body().getBinary("data");

			logger.debug("handling message to server {}, command: {}", config.agentId, command);
			send(command, data);
			JsonObject obj = new JsonObject().putString("commandId", commandId);
			vertx.eventBus().send(AgentClientVerticle.address_command_done, obj);
		});

	}

	private void send(String command, byte[] data) {
		BmMessage message = new BmMessage();
		message.setAgentId(config.agentId);
		message.setCommand(command);
		message.setData(data);
		try {
			Buffer dataBuffer = new Buffer(parser.write(message));
			if (connected) {
				logger.trace("Writing {} bytes to websocket", dataBuffer.length());
				ws.write(dataBuffer);
			} else {
				if (!connecting && command.equals("ping")) {
					logger.info("Lost connection to server...Trying to reconnect");
					connect();
				}
			}
		} catch (Exception e) {
			logger.warn("Cannot send reply to server", e);
		}
	}

	private void connect() {
		logger.info("Connecting to {}:{}", config.host, config.port);
		connecting = true;

		HttpClient client = vertx.createHttpClient() //
				.setMaxWebSocketFrameSize(WS_FRAMESIZE) //
				.setHost(config.host) //
				.setPort(config.port);

		if (config.sslConfig.isSsl()) {
			client.setSSL(true) //
					.setTrustStorePath(config.sslConfig.getTrustStore()) //
					.setTrustStorePassword(config.sslConfig.getTrustStorePassword());
			if (config.getSslConfig().isAuthRequired()) {
				client.setKeyStorePath(config.sslConfig.getKeyStore()) //
						.setKeyStorePassword(config.sslConfig.getKeyStorePassword());
			}
		}

		client.exceptionHandler((t) -> {
			logger.warn("Error on Connection: {}", t.getMessage());
			connecting = false;
		});

		client.connectWebsocket("/", (ws -> {
			logger.info("Connected to websocket");
			connecting = false;
			connected = true;
			this.ws = ws;
			ws.dataHandler(data -> {
				logger.trace("Read {} bytes from websocket", data.length());
				String value = new String(data.getBytes());
				handleMessage(ws, value);
			});
			ws.closeHandler((v) -> {
				connected = false;
				connect();
			});
			ws.exceptionHandler((t) -> {
				logger.warn("Error on Websocket: {}", t.getMessage());
			});
			// register the agent by sending a message
			send("ping", "ping".getBytes());
		}));

	}

	private void handleMessage(WebSocket ws, String value) {
		try {
			BmMessage message = parser.read(value);
			logger.debug("Incoming Message: {}", message);

			JsonObject obj = new JsonObject() //
					.putString("agentId", config.agentId) //
					.putString("command", message.getCommand()) //
					.putBinary("data", message.getData()) //
					.asObject();
			vertx.eventBus().send(AgentClientVerticle.address_message, obj);
		} catch (Exception e) {
			logger.warn("Error while handling message", e);
		}

	}

	private void registerHandlers() {
		List<ClientHandler> plugins = PluginLoader.load();
		plugins.forEach(plugin -> {
			logger.info("Registering plugin {} for command {}", plugin.name, plugin.command);
			HandlerRegistry.getInstance().register(plugin.command, plugin.handler, plugin.name);
			JsonObject obj = new JsonObject() //
					.putString("command", plugin.command) //
					.putString("agentId", config.agentId) //
					.asObject();

			vertx.eventBus().send(AgentClientVerticle.address_init, obj);
		});
	}

}
