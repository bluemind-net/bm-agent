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
package net.bluemind.agent.server.handler.redirect;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;

import net.bluemind.agent.VertxHolder;
import net.bluemind.agent.server.ServerConnection;
import net.bluemind.agent.server.handler.redirect.config.HostPortConfig;

public class Listener {

	protected static Logger logger = LoggerFactory.getLogger(Listener.class);

	public final String agentId;
	public final String command;
	public final ServerConnection connection;
	public final HostPortConfig hostPortConfig;
	public final Map<String, ServerHandler> serverHandlers;

	public Listener(String agentId, String command, ServerConnection connection, HostPortConfig hostPortConfig) {
		this.agentId = agentId;
		this.command = command;
		this.connection = connection;
		this.hostPortConfig = hostPortConfig;
		serverHandlers = new ConcurrentHashMap<>();
	}

	public void start() throws Exception {

		NetServer createNetServer = VertxHolder.vertx.createNetServer();
		createNetServer.connectHandler(new Handler<NetSocket>() {

			@Override
			public void handle(NetSocket netSocket) {
				String clientId = UUID.randomUUID().toString();
				ServerHandler serverHandler = new ServerHandler(clientId, netSocket, Listener.this);
				serverHandlers.put(clientId, serverHandler);
			}
		});
		createNetServer.listen(hostPortConfig.localPort);
	}

	public void receive(String clientId, byte[] value) {
		logger.info("Writing to server {}:{} bytes", clientId, value.length);
		serverHandlers.get(clientId).write(new Buffer(value));
	}

	public static class ServerHandler {

		private final String clientId;
		private final NetSocket netSocket;
		private final Listener listener;
		boolean stopped;
		private Buffer buffer = new Buffer();

		public ServerHandler(String clientId, NetSocket netSocket, Listener listener) {
			this.netSocket = netSocket;
			this.listener = listener;
			this.clientId = clientId;

			setupHandlers();
		}

		private void setupHandlers() {
			netSocket.closeHandler(new Handler<Void>() {

				@Override
				public void handle(Void event) {
					logger.info("Disconnecting from local client {}", clientId);
					listener.remove(clientId);
				}

			});
			netSocket.dataHandler(new Handler<Buffer>() {

				@Override
				public void handle(Buffer buffer) {
					byte[] data = buffer.getBytes();
					logger.info("Received {} bytes from local client, redirecting to remote server: {}", data.length,
							clientId);
					byte[] messageData = new JsonObject() //
							.putString("server-host", listener.hostPortConfig.serverHost) //
							.putNumber("server-dest-port", listener.hostPortConfig.remotePort) //
							.putNumber("client-port", listener.hostPortConfig.localPort) //
							.putString("client-id", clientId) //
							.putBinary("data", data).asObject().encode().getBytes();
					listener.connection.send(listener.agentId, listener.command, messageData);

				}
			});
			netSocket.drainHandler(new Handler<Void>() {

				@Override
				public void handle(Void event) {
					stopped = false;
					tryWrite();
				}
			});
			netSocket.exceptionHandler(new Handler<Throwable>() {

				@Override
				public void handle(Throwable event) {
					logger.warn("Error occured while talking to local client {}", clientId, event);
				}
			});
		}

		protected void tryWrite() {
			if (!netSocket.writeQueueFull()) {
				netSocket.write(buffer);
				buffer = new Buffer();
			} else {
				stopped = true;
			}

		}

		public void write(Buffer data) {
			buffer.appendBuffer(data);
			tryWrite();
		}

	}

	protected void remove(String clientId) {
		serverHandlers.remove(clientId);
	}

}
