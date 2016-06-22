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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;

public class AgentClient {

	private static final int PORT = 8086;
	private final Vertx vertx;
	private WebSocket ws;

	private final Logger logger = LoggerFactory.getLogger(AgentClient.class);

	public AgentClient() {
		this.vertx = VertxFactory.newVertx();
	}

	public void start() throws Exception {
		logger.info("Starting BM Agent Client");
		CountDownLatch latch = new CountDownLatch(1);
		connect(latch);
		latch.await();
		send("Hello from the Client");
		send("Hello from the Client2");
		send("Hello from the Client3");

		block();
	}

	private void block() {
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void connect(final CountDownLatch latch) {
		HttpClient client = vertx.createHttpClient().setHost("localhost").setPort(PORT);

		client.connectWebsocket("/", (ws -> {
			logger.info("Connected to websocket");
			this.ws = ws;
			latch.countDown();
		}));
	}

	private void send(String data) {
		send(data.getBytes());
	}

	private void send(byte[] data) {
		logger.info("sending {}", new String(data));
		Buffer buffer = new Buffer(data);
		ws.write(buffer);
	}

}
