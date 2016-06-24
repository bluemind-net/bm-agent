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
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.agent.BmMessage;
import net.bluemind.agent.MessageParser;
import net.bluemind.agent.server.internal.PluginLoader.ServerHandler;

public class AgentServer extends Verticle {

	public static final String address = "agent.reply";

	private static final int PORT = 8086;
	private MessageParser parser;

	private final Logger logger = LoggerFactory.getLogger(AgentServer.class);

	@Override
	public void start() {
		logger.info("Starting BM Agent Server on port {}", PORT);
		this.parser = new MessageParser();
		registerHandlers();
		HttpServer server = vertx.createHttpServer();

		server.websocketHandler(ws -> {
			logger.info("Connection to websocket established from client: {}",
					ws.remoteAddress().getAddress().toString());
			ws.dataHandler(data -> {
				logger.info("Server thread for message: {}", Thread.currentThread().getId());
				String value = new String(data.getBytes());
				handleMessage(ws, value);
			});
		}).listen(PORT, "localhost");

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

	private void reply(String command, String id, byte[] data, ServerWebSocket con) {
		BmMessage message = new BmMessage();
		message.setId(id);
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
					.putString("id", message.getId()) //
					.putString("command", message.getCommand()) //
					.putBinary("data", message.getData()) //
					.asObject();
			ConnectionRegistry.getInstance().register(message.getId(), ws);
			vertx.eventBus().send(AgentServerVerticle.address, obj);

		} catch (Exception e) {
			logger.warn("Error while handling message", e);
		}

	}

	private void registerHandlers() {
		List<ServerHandler> plugins = PluginLoader.load();
		plugins.forEach(plugin -> {
			logger.info("Registering plugin {} for command {}", plugin.name, plugin.command);
			HandlerRegistry.getInstance().register(plugin.command, plugin.handler, plugin.name);
		});
	}

}
