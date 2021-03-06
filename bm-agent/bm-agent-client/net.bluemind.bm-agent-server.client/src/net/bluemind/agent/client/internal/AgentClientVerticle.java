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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.agent.DoneHandler;
import net.bluemind.agent.NoopHandler;
import net.bluemind.agent.client.ClientConnection;
import net.bluemind.agent.client.internal.handler.HandlerRegistry;
import net.bluemind.agent.client.internal.handler.HandlerRegistry.AgentHandler;

public class AgentClientVerticle extends Verticle implements ClientConnection {
	public static final String address_init = "agent.init";
	public static final String address_message = "agent.message";
	public static String address_command_done = "agent.command.done";

	private static final Map<String, DoneHandler> currentCommandMap = new ConcurrentHashMap<>();
	private static final Logger logger = LoggerFactory.getLogger(AgentClientVerticle.class);

	@Override
	public void start() {
		super.start();

		EventBus eventBus = vertx.eventBus();

		eventBus.registerHandler(address_init, (Message<JsonObject> event) -> {
			String command = event.body().getString("command");
			String agentId = event.body().getString("agentId");
			Optional<AgentHandler> handler = HandlerRegistry.getInstance().get(command);
			handler.ifPresent(h -> {
				logger.debug("Found handler {} for command {}", h.info, command);
				h.handler.onInitialize(command, agentId, AgentClientVerticle.this);
			});

		});

		eventBus.registerHandler(address_message, (Message<JsonObject> event) -> {
			String command = event.body().getString("command");
			byte[] data = event.body().getBinary("data");
			Optional<AgentHandler> handler = HandlerRegistry.getInstance().get(command);
			handler.ifPresent(h -> {
				logger.debug("Found handler {} for command {}", h.info, command);
				h.handler.onMessage(data);
			});

		});

		eventBus.registerHandler(address_command_done, (Message<JsonObject> event) -> {
			String commandId = event.body().getString("commandId");
			currentCommandMap.get(commandId).handle();
			currentCommandMap.remove(commandId);
		});

	}

	@Override
	public void send(String command, byte[] data) {
		send(command, data, NoopHandler.getInstance());
	}

	@Override
	public void send(String command, byte[] data, DoneHandler doneHandler) {
		String commandId = UUID.randomUUID().toString();
		currentCommandMap.put(commandId, doneHandler);
		JsonObject obj = new JsonObject() //
				.putString("command", command) //
				.putString("commandId", commandId) //
				.putBinary("data", data) //
				.asObject();
		vertx.eventBus().send(AgentClient.address, obj);
	}

}
