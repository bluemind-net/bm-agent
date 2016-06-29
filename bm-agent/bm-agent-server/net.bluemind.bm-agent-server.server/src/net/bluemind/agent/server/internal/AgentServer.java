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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.agent.BmMessage;
import net.bluemind.agent.MessageParser;
import net.bluemind.agent.server.internal.RequestParser.Command;
import net.bluemind.agent.server.internal.config.ConfigReader;
import net.bluemind.agent.server.internal.config.ServerConfig;
import net.bluemind.agent.server.internal.connection.ConnectionRegistry;
import net.bluemind.agent.server.internal.handler.HandlerRegistry;
import net.bluemind.agent.server.internal.handler.PluginLoader;
import net.bluemind.agent.server.internal.handler.PluginLoader.ServerHandler;

public class AgentServer extends Verticle {

	public static final String address = "agent.reply";
	private MessageParser parser;
	private static final int WS_FRAMESIZE = 65536 * 4;

	private final Logger logger = LoggerFactory.getLogger(AgentServer.class);

	@Override
	public void start() {
		ServerConfig config = ConfigReader.readConfig("bm-agent-server-config", "/etc/bm/agent/server-config.json");
		logger.info("Starting BM Agent Server on port {}", config.port);
		this.parser = new MessageParser();
		registerHandlers();
		HttpServer server = vertx.createHttpServer() //
				.setMaxWebSocketFrameSize(WS_FRAMESIZE);

		server.websocketHandler(ws -> {
			logger.info("Connection to websocket established from client: {}",
					ws.remoteAddress().getAddress().toString());
			ws.dataHandler(data -> {
				String value = new String(data.getBytes());
				handleMessage(ws, value);
			});
		}).requestHandler(request -> {
			Command command = RequestParser.parse(request);
			logger.info("Handling command {}:{}", command.agentId, command.command);
			request.response().end();
			handleCommand(command);
		}).listen(config.port, config.listenerAddress);

		vertx.eventBus().registerHandler(address, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				String command = event.body().getString("command");
				String id = event.body().getString("id");
				byte[] data = event.body().getBinary("data");

				logger.info("handling reply to client {}, command: {}", id, command);
				ConnectionRegistry.getInstance().get(id).ifPresent(con -> {
					reply(command, id, data, con);
				});
			}

		});

	}

	private void reply(String command, String agentId, byte[] data, ServerWebSocket con) {
		BmMessage message = new BmMessage();
		message.setCommand(command);
		message.setData(data);
		try {
			Buffer buffer = new Buffer(parser.write(message));
			con.write(buffer);
		} catch (Exception e) {
			logger.warn("Cannot send reply to client", e);
		}
	}

	private void handleMessage(ServerWebSocket ws, String value) {
		try {
			BmMessage message = parser.read(value);
			logger.info("Incoming Message: {}", message);

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

	private void handleCommand(Command command) {
		JsonArray pathParameters = new JsonArray();
		for (String param : command.pathParameters) {
			pathParameters.add(param);
		}

		String writeValueAsString = null;
		try {
			writeValueAsString = new ObjectMapper().writeValueAsString(command.queryParameters);
		} catch (JsonProcessingException e) {
			logger.warn("Cannot process query parameters", e);
		}
		JsonObject obj = new JsonObject() //
				.putString("agentId", command.agentId) //
				.putString("command", command.command) //
				.putString("queryParameters", writeValueAsString).putArray("pathParameters", pathParameters);

		vertx.eventBus().send(AgentServerVerticle.address_init, obj);
	}

	private void registerHandlers() {
		List<ServerHandler> plugins = PluginLoader.load();
		plugins.forEach(plugin -> {
			logger.info("Registering plugin {} for command {}", plugin.name, plugin.command);
			HandlerRegistry.getInstance().register(plugin.command, plugin.handler, plugin.name);
		});
	}

}
