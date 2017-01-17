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

import net.bluemind.agent.DoneHandler;
import net.bluemind.agent.client.AgentClientHandler;
import net.bluemind.agent.client.ClientConnection;

public class PortRedirectClientHandler implements AgentClientHandler {

	Logger logger = LoggerFactory.getLogger(PortRedirectClientHandler.class);
	public static Map<String, ConnectionHandler> handlers = new ConcurrentHashMap<>();
	private PortRedirectionConnection connection;
	private String agentId;

	@Override
	public void onMessage(byte[] data) {

		logger.debug("Received a port redirect message containing {} bytes", data.length);

		JsonObject obj = new JsonObject(new String(data));
		String serverHost = obj.getString("server-host");
		int serverDestPort = obj.getInteger("server-dest-port");
		int clientPort = obj.getInteger("client-port");
		String clientId = obj.getString("client-id");
		String control = obj.getString("control");
		byte[] value = obj.getBinary("data");

		logger.trace("data: {}", new String(value));

		ConnectionHandler handler = null;
		if (handlers.containsKey(clientId)) {
			logger.debug("handler for id {} is already connected", clientId);
			handler = handlers.get(clientId);
			if (control.equals("ack/end")) {
				handler.disconnect();
			}
		} else {
			logger.info("handler for id {} is not connected yet", clientId);
			if (!control.equals("ack/end")) {
				handler = new ConnectionHandler(connection, clientId, serverHost, clientPort, serverDestPort, agentId);
				try {
					handler.connect();
					logger.info("Connected to {}:{}", serverHost, serverDestPort);
				} catch (Exception e) {
					logger.warn("Cannot connect to remote server", e);
				}
				handlers.put(clientId, handler);
			}
		}

		if (!control.equals("syn/ack") && !control.equals("ack/end")) {
			handler.write(control, value);
		}

	}

	@Override
	public void onInitialize(String command, String agentId, ClientConnection connection) {
		this.agentId = agentId;
		this.connection = new PortRedirectionConnection(connection, command);
	}

	public static class PortRedirectionConnection implements ClientConnection {

		Logger logger = LoggerFactory.getLogger(PortRedirectionConnection.class);

		private final ClientConnection connection;
		private final String command;

		public PortRedirectionConnection(ClientConnection connection, String command) {
			this.connection = connection;
			this.command = command;
		}

		public void send(byte[] data) {
			send(this.command, data);
		}

		@Override
		public void send(String command, byte[] data) {
			connection.send(command, data);
		}

		public void remove(String clientId) {
			PortRedirectClientHandler.handlers.remove(clientId);
		}

		@Override
		public void send(String command, byte[] data, DoneHandler doneHandler) {
			connection.send(command, data, doneHandler);
		}

	}
}
