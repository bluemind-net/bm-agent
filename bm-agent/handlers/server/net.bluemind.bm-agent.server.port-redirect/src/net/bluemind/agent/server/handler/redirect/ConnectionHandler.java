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

import java.util.concurrent.CountDownLatch;

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
import net.bluemind.agent.server.handler.redirect.PortRedirectServerHandler.PortRedirectionConnection;

public class ConnectionHandler {

	protected final String clientId;
	protected final int clientPort;
	protected final int serverDestPort;
	protected final String id;
	protected final String command;
	protected final PortRedirectionConnection connection;
	protected final String serverHost;
	NetSocket socket;

	private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

	public ConnectionHandler(String id, String command, PortRedirectionConnection connection, String clientId,
			String serverHost, int clientPort, int serverDestPort) {
		this.id = id;
		this.command = command;
		this.clientId = clientId;
		this.clientPort = clientPort;
		this.serverDestPort = serverDestPort;
		this.connection = connection;
		this.serverHost = serverHost;
	}

	public void connect() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Vertx vertx = VertxHolder.vertx;

		NetClient client = vertx.createNetClient();

		client.connect(serverDestPort, serverHost, new Handler<AsyncResult<NetSocket>>() {
			public void handle(AsyncResult<NetSocket> asyncResult) {
				if (asyncResult.succeeded()) {
					logger.info("Connected to server {}:{}", serverHost, serverDestPort);
					socket = asyncResult.result();
					socket.dataHandler(new Handler<Buffer>() {
						public void handle(Buffer event) {
							byte[] data = event.getBytes();
							logger.info("Received {} bytes from server, redirecting to client: {}", data.length);
							logger.debug("data: {}", new String(data));
							byte[] messageData = new JsonObject() //
									.putNumber("client-port", clientPort) //
									.putString("client-id", clientId) //
									.putBinary("data", data).asObject().encode().getBytes();
							connection.send(id, command, messageData);

						}
					});
					socket.closeHandler(new Handler<Void>() {

						@Override
						public void handle(Void event) {
							logger.info("Socket to {}:{} has been closed", serverHost, serverDestPort);
							connection.remove(ConnectionHandler.this.clientId);
						}
					});
					socket.exceptionHandler(new Handler<Throwable>() {

						@Override
						public void handle(Throwable event) {
							logger.warn("Error while talking to {}:{}", serverHost, serverDestPort, event);
						}
					});
				} else {
					logger.warn("Cannot connect to server {}:{}", serverHost, serverDestPort, asyncResult.cause());
				}
				latch.countDown();
			}
		});
		latch.await();
	}

	public void write(byte[] value) {
		Buffer buffer = new Buffer(value);
		socket.write(buffer);
	}

}
