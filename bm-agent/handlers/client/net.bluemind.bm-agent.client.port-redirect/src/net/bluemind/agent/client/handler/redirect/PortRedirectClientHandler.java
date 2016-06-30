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
package net.bluemind.agent.client.handler.redirect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.agent.Connection;
import net.bluemind.agent.client.AgentClientHandler;

public class PortRedirectClientHandler implements AgentClientHandler {

	Logger logger = LoggerFactory.getLogger(PortRedirectClientHandler.class);
	public static Map<String, ConnectionHandler> handlers = new ConcurrentHashMap<>();
	private PortRedirectionConnection connection;

	@Override
	public void onMessage(byte[] data) {

		logger.info("Received a port redirect message containing {} bytes", data.length);
		logger.trace("data: {}", new String(data));

		JsonObject obj = new JsonObject(new String(data));

		String serverHost = obj.getString("server-host");
		int serverDestPort = obj.getInteger("server-dest-port");
		int clientPort = obj.getInteger("client-port");
		String clientId = obj.getString("client-id");
		byte[] value = obj.getBinary("data");

		ConnectionHandler handler = null;
		if (handlers.containsKey(clientId)) {
			logger.info("handler for id {} is already connected", clientId);
			handler = handlers.get(clientId);
		} else {
			logger.info("handler for id {} is not connected yet", clientId);
			handler = new ConnectionHandler(connection, clientId, serverHost, clientPort, serverDestPort);
			try {
				handler.connect();
				logger.info("Connected to {}:{}", serverHost, serverDestPort);
			} catch (Exception e) {
				logger.warn("Cannot connect to remote server", e);
			}
			handlers.put(clientId, handler);
		}

		handler.write(value);

	}

	@Override
	public void onInitialize(String command, Connection connection) {
		this.connection = new PortRedirectionConnection(connection, command);
	}

	public static class PortRedirectionConnection implements Connection {

		Logger logger = LoggerFactory.getLogger(PortRedirectionConnection.class);

		private final Connection connection;
		private final String command;

		public PortRedirectionConnection(Connection connection, String command) {
			this.connection = connection;
			this.command = command;
		}

		public void send(byte[] data) {
			send(null, this.command, data);
		}

		@Override
		public void send(String agentId, String command, byte[] data) {
			connection.send(agentId, command, data);
		}

		public void remove(String clientId) {
			PortRedirectClientHandler.handlers.remove(clientId);
		}

	}
}
