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

import net.bluemind.agent.Connection;
import net.bluemind.agent.client.AgentClientHandler;

public class PingClientHandler implements AgentClientHandler {

	Logger logger = LoggerFactory.getLogger(PingClientHandler.class);

	@Override
	public void onInitialize(Connection connection) {
		logger.info("Pinging server");
		try {
			connection.send("Hello from the Gutter".getBytes());
		} catch (Exception e) {
			logger.warn("Cannot ping server", e);
		}
	}

	@Override
	public void onMessage(byte[] data) {
		logger.info("Got a message: {}", new String(data));
	}

}
