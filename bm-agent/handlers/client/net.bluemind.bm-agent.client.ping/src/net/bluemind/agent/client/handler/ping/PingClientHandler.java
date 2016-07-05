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
package net.bluemind.agent.client.handler.ping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.agent.client.AgentClientHandler;
import net.bluemind.agent.client.ClientConnection;

public class PingClientHandler implements AgentClientHandler {

	Logger logger = LoggerFactory.getLogger(PingClientHandler.class);

	@Override
	public void onMessage(byte[] data) {
		logger.debug("Got a message: {}", new String(data));
	}

	@Override
	public void onInitialize(final String command, final ClientConnection connection) {
		ping(command, connection);
		new Thread(() -> {
			while (true) {
				sleep(30000);
				ping(command, connection);
			}
		}).start();
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	private void ping(String command, ClientConnection connection) {
		logger.debug("Pinging server");
		try {
			connection.send(command, "ping".getBytes());
		} catch (Exception e) {
			logger.warn("Cannot ping server", e);
		}
	}

}
