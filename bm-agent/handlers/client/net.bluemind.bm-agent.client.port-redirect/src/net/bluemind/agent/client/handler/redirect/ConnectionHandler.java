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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;

import net.bluemind.agent.VertxHolder;
import net.bluemind.agent.client.handler.redirect.PortRedirectClientHandler.PortRedirectionConnection;

public class ConnectionHandler {

	protected final String clientId;
	protected final int clientPort;
	protected final int serverDestPort;
	protected final PortRedirectionConnection connection;
	protected final String serverHost;
	private Buffer buffer = new Buffer();
	NetSocket socket;
	boolean stopped;
	private boolean connected = false;

	private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

	public ConnectionHandler(PortRedirectionConnection connection, String clientId, String serverHost, int clientPort,
			int serverDestPort) {
		this.clientId = clientId;
		this.clientPort = clientPort;
		this.serverDestPort = serverDestPort;
		this.connection = connection;
		this.serverHost = serverHost;
	}

	public void connect() throws Exception {
		logger.info("Going to connect to {}:{}", serverHost, serverDestPort);
		Vertx vertx = VertxHolder.vertx;
		NetClient client = vertx.createNetClient();
		client.connect(serverDestPort, serverHost, (AsyncResult<NetSocket> asyncResult) -> {
			if (asyncResult.succeeded()) {
				logger.info("Connected to server {}:{}", serverHost, serverDestPort);
				socket = asyncResult.result();
				socket.dataHandler(new Handler<Buffer>() {
					public void handle(Buffer event) {
						byte[] data = event.getBytes();
						logger.debug("Received {} bytes from local server, redirecting to agent-server: {}",
								data.length, clientId);
						logger.trace("data: {}", new String(data));
						byte[] messageData = createMessage(data);
						connection.send(messageData);

					}
				});
				socket.closeHandler((Void event) -> {
					logger.info("Socket to {}:{} has been closed", serverHost, serverDestPort);
					connection.remove(ConnectionHandler.this.clientId);
				});
				socket.exceptionHandler((Throwable event) -> {
					logger.warn("Error while talking to {}:{}", serverHost, serverDestPort, event);
				});
				if (buffer.length() > 0) {
					socket.write(buffer);
					buffer = new Buffer();
				}
				connected = true;
			} else {
				logger.warn("Cannot connect to server {}:{}", serverHost, serverDestPort, asyncResult.cause());
			}

		});
	}

	private byte[] createMessage(byte[] data) {
		byte[] messageData = new JsonObject() //
				.putNumber("client-port", clientPort) //
				.putString("client-id", clientId) //
				.putBinary("data", data).asObject().encode().getBytes();
		return messageData;
	}

	public void write(byte[] value) {
		if (new String(value).equals("pause")) {
			logger.info("Server signalized a full queue, Stopping stream");
			socket.pause();
		} else {
			if (new String(value).equals("resume")) {
				logger.info("Server is ready to write, Resuming stream");
				socket.resume();
			} else {
				this.buffer.appendBuffer(new Buffer(value));
				tryWrite();
			}
		}

	}

	protected void tryWrite() {
		if (!stopped) {
			if (buffer.length() > 0) {
				socket.write(buffer);
			}
			buffer = new Buffer();
			if (socket.writeQueueFull()) {
				logger.info("Write queue to port {}:{} is full, pausing stream", serverHost, serverDestPort);
				stopped = true;
				byte[] stopMesssage = createMessage("pause".getBytes());
				connection.send(stopMesssage);
				socket.drainHandler((Void event) -> {
					logger.info("Resuming stream to write queue {}:{}", serverHost, serverDestPort);
					stopped = false;
					byte[] resumeMessage = createMessage("resume".getBytes());
					connection.send(resumeMessage);
					tryWrite();
				});
			}
		}
	}

	public void disconnect() {
		socket.close();
		connection.remove(this.clientId);
	}

}
