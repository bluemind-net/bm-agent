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
	public Map<String, ServerHandler> serverHandlers;
	private NetServer server;

	public Listener(String agentId, String command, ServerConnection connection, HostPortConfig hostPortConfig) {
		this.agentId = agentId;
		this.command = command;
		this.connection = connection;
		this.hostPortConfig = hostPortConfig;
		serverHandlers = new ConcurrentHashMap<>();
	}

	public void start() throws Exception {

		server = VertxHolder.vertx.createNetServer();
		server.connectHandler((NetSocket netSocket) -> {
			String clientId = UUID.randomUUID().toString();
			ServerHandler serverHandler = new ServerHandler(clientId, netSocket, Listener.this);
			serverHandlers.put(clientId, serverHandler);
			serverHandler.init();
		});
		server.listen(hostPortConfig.localPort);
	}

	public void stop() {
		server.close();
	}

	public void receive(String clientId, byte[] value) {
		logger.debug("Writing to server {}:{} bytes", clientId, value.length);
		if (!serverHandlers.containsKey(clientId)) {
			logger.warn("Client tries to talk to non-existing server: {}", clientId);
		} else {
			serverHandlers.get(clientId).write(value);
		}
	}

	public static class ServerHandler {

		private final String clientId;
		private final NetSocket netSocket;
		private final Listener listener;
		boolean stopped;
		private Buffer readBuffer = new Buffer();

		public ServerHandler(String clientId, NetSocket netSocket, Listener listener) {
			this.netSocket = netSocket;
			this.listener = listener;
			this.clientId = clientId;

			setupHandlers();
		}

		public void init() {
			byte[] messageData = createMessage("syn/ack".getBytes());
			listener.connection.send(listener.agentId, listener.command, messageData);
		}

		private void setupHandlers() {
			netSocket.closeHandler((Void event) -> {
				logger.info("Disconnecting from local client {}", clientId);
				byte[] messageData = createMessage("ack/end".getBytes());
				listener.connection.send(listener.agentId, listener.command, messageData);
				listener.remove(clientId);
			}

			);
			netSocket.dataHandler((Buffer buffer) -> {
				byte[] data = buffer.getBytes();
				logger.debug("Received {} bytes from local client, redirecting to client-agent: {}", data.length,
						clientId);
				logger.trace("data: {}", new String(data));
				byte[] messageData = createMessage(data);
				listener.connection.send(listener.agentId, listener.command, messageData);
			});
			netSocket.exceptionHandler((Throwable event) -> {
				logger.warn("Error occured while talking to local client {}", clientId, event);
			});
		}

		private byte[] createMessage(byte[] data) {
			byte[] messageData = new JsonObject() //
					.putString("server-host", listener.hostPortConfig.serverHost) //
					.putNumber("server-dest-port", listener.hostPortConfig.remotePort) //
					.putNumber("client-port", listener.hostPortConfig.localPort) //
					.putString("client-id", clientId) //
					.putBinary("data", data).asObject().encode().getBytes();
			return messageData;
		}

		protected void tryWrite() {
			if (!stopped) {
				if (readBuffer.length() > 0) {
					netSocket.write(readBuffer);
				}
				readBuffer = new Buffer();
				if (netSocket.writeQueueFull()) {
					logger.debug("Write queue to port {} is full, pausing stream", listener.hostPortConfig.localPort);
					stopped = true;
					byte[] stopMesssage = createMessage("pause".getBytes());
					listener.connection.send(listener.agentId, listener.command, stopMesssage);
					netSocket.drainHandler((Void event) -> {
						logger.debug("Resuming stream to write queue on port {}", listener.hostPortConfig.localPort);
						stopped = false;
						byte[] resumeMessage = createMessage("resume".getBytes());
						listener.connection.send(listener.agentId, listener.command, resumeMessage);
						tryWrite();
					});
				}
			}
		}

		public void write(byte[] value) {
			if (new String(value).equals("pause")) {
				logger.debug("Client signalized a full queue, Stopping stream");
				netSocket.pause();
			} else {
				if (new String(value).equals("resume")) {
					logger.debug("Client is ready to write, Resuming stream");
					netSocket.resume();
				} else {
					readBuffer.appendBuffer(new Buffer(value));
					tryWrite();
				}
			}

		}

	}

	protected void remove(String clientId) {
		serverHandlers.remove(clientId);
	}

}
