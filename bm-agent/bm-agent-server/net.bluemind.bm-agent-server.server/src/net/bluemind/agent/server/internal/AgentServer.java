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
package net.bluemind.agent.server.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.agent.BmMessage;
import net.bluemind.agent.MessageParser;
import net.bluemind.agent.server.Command;
import net.bluemind.agent.server.ServerStore;
import net.bluemind.agent.server.internal.config.ServerConfig;
import net.bluemind.agent.server.internal.connection.ConnectionRegistry;
import net.bluemind.agent.server.internal.handler.HandlerRegistry;
import net.bluemind.agent.server.internal.handler.PluginLoader;
import net.bluemind.agent.server.internal.handler.PluginLoader.ServerHandler;

public class AgentServer extends Verticle {

	public static final String address = "agent.reply";
	public static final String address_command_reply = "command.reply";
	private MessageParser parser;
	private static final int WS_FRAMESIZE = 65536 * 4;
	private static Map<Long, HttpServerResponse> responseMap = new HashMap<>();

	private final Logger logger = LoggerFactory.getLogger(AgentServer.class);

	@Override
	public void start() {
		ServerConfig config = new ServerConfig(container.config());
		ServerStore.storePath = config.storePath;
		logger.info("Starting BM Agent Server on port {}", config.port);
		this.parser = new MessageParser();
		registerHandlers();
		HttpServer server = vertx.createHttpServer() //
				.setMaxWebSocketFrameSize(WS_FRAMESIZE);

		if (config.sslConfig.isSsl()) {
			configureSSL(config, server);
		}

		server.websocketHandler(ws -> {
			logger.info("Connection to websocket established from client: {}",
					ws.remoteAddress().getAddress().toString());
			ws.dataHandler(data -> {
				handleIncomingMessage(ws, data);
			});
		}).requestHandler(request -> {
			handleCommand(request);
		}).listen(config.port, config.listenerAddress);

		vertx.eventBus().registerHandler(address_command_reply, (Message<JsonObject> event) -> {
			handleCommandResponse(event);
		});

		vertx.eventBus().registerHandler(address, (Message<JsonObject> event) -> {
			handleOutgoingMessage(event);
		});
	}

	private void handleIncomingMessage(ServerWebSocket ws, Buffer data) {
		logger.trace("Read {} bytes from websocket", data.length());
		String value = new String(data.getBytes());
		try {
			BmMessage message = parser.read(value);
			logger.debug("Incoming Message: {}", message);

			JsonObject obj = new JsonObject() //
					.putString("agentId", message.getAgentId()) //
					.putString("command", message.getCommand()) //
					.putBinary("data", message.getData()) //
					.asObject();
			ConnectionRegistry.getInstance().register(message.getAgentId(), ws);
			vertx.eventBus().send(AgentServerVerticle.address, obj);

		} catch (Exception e) {
			logger.warn("Error while handling message", e);
		}
	}

	private void handleOutgoingMessage(Message<JsonObject> event) {
		String commandId = event.body().getString("commandId");
		String command = event.body().getString("command");
		String agentId = event.body().getString("agentId");
		byte[] data = event.body().getBinary("data");

		logger.debug("handling reply to client {}, command: {}", agentId, command);
		Optional<ServerWebSocket> con = ConnectionRegistry.getInstance().get(agentId);
		if (!con.isPresent()) {
			logger.warn("Cannot send message to client {}, command: {}. Agent is not connected", agentId, command);
		} else {
			sendToClient(command, agentId, data, con.get());
		}
		JsonObject obj = new JsonObject().putString("commandId", commandId);
		vertx.eventBus().send(AgentServerVerticle.address_command_done, obj);
	}

	private void sendToClient(String command, String agentId, byte[] data, ServerWebSocket con) {
		BmMessage message = new BmMessage();
		message.setCommand(command);
		message.setData(data);
		try {
			Buffer buffer = new Buffer(parser.write(message));
			logger.trace("Writing {} bytes to websocket", buffer.length());
			con.write(buffer);
		} catch (Exception e) {
			logger.warn("Cannot send reply to client {}", agentId, e);
		}
	}

	private void handleCommand(HttpServerRequest request) {
		Command command = RequestParser.parse(request);
		logger.info("Handling command {}:{}", command.agentId, command.command);
		command.id = vertx.setTimer(30000, (id) -> {
			logger.info("Command {} timed out, ending request", id);
			responseMap.remove(id).end();
		});
		responseMap.put(command.id, request.response());
		vertx.eventBus().send(AgentServerVerticle.address_command, command.toJsonObject());
	}

	private void handleCommandResponse(Message<JsonObject> event) {
		long id = event.body().getLong("id");
		if (responseMap.containsKey(id)) {
			vertx.cancelTimer(id);
			String response = event.body().getString("response");
			if (!response.isEmpty()) {
				responseMap.remove(id).end(response);
			} else {
				responseMap.remove(id).end();
			}
		}
	}

	private void registerHandlers() {
		List<ServerHandler> plugins = PluginLoader.load();
		plugins.forEach(plugin -> {
			logger.info("Registering plugin {} for command {}", plugin.name, plugin.command);
			HandlerRegistry.getInstance().register(plugin.command, plugin.handler, plugin.name);
			JsonObject obj = new JsonObject() //
					.putString("commandId", plugin.command).asObject();
			vertx.eventBus().send(AgentServerVerticle.address_init, obj);
		});
	}

	private void configureSSL(ServerConfig config, HttpServer server) {
		server.setSSL(true) //
				.setKeyStorePath(config.sslConfig.getKeyStore()) //
				.setKeyStorePassword(config.sslConfig.getKeyStorePassword());
		if (config.getSslConfig().isAuthRequired()) {
			server.setClientAuthRequired(true) //
					.setTrustStorePath(config.sslConfig.getTrustStore()) //
					.setTrustStorePassword(config.sslConfig.getTrustStorePassword());
		}
	}

}
