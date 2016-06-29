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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.agent.Connection;
import net.bluemind.agent.client.internal.handler.HandlerRegistry;
import net.bluemind.agent.client.internal.handler.HandlerRegistry.AgentHandler;

public class AgentClientVerticle extends Verticle implements Connection {
	public static final String address_init = "agent.init";
	public static final String address_message = "agent.message";

	private static final Logger logger = LoggerFactory.getLogger(AgentClientVerticle.class);

	@Override
	public void start() {
		super.start();

		EventBus eventBus = vertx.eventBus();

		eventBus.registerHandler(address_init, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				String command = event.body().getString("command");
				Optional<AgentHandler> handler = HandlerRegistry.getInstance().get(command);
				handler.ifPresent(h -> {
					logger.info("Found handler {} for command {}", h.info, command);
					h.handler.onInitialize(command, AgentClientVerticle.this);
				});

			}
		});

		eventBus.registerHandler(address_message, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				String command = event.body().getString("command");
				byte[] data = event.body().getBinary("data");
				Optional<AgentHandler> handler = HandlerRegistry.getInstance().get(command);
				handler.ifPresent(h -> {
					logger.info("Found handler {} for command {}", h.info, command);
					h.handler.onMessage(data);
				});

			}
		});

	}

	@Override
	public void send(String command, byte[] data) {
		JsonObject obj = new JsonObject() //
				.putString("command", command) //
				.putBinary("data", data) //
				.asObject();
		vertx.eventBus().send(AgentClient.address, obj);
	}

}
