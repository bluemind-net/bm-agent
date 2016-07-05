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
package net.bluemind.agent.server.handler.ping;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.agent.server.AgentServerHandler;
import net.bluemind.agent.server.ServerConnection;

public class PingServerHandler implements AgentServerHandler {

	Logger logger = LoggerFactory.getLogger(PingServerHandler.class);

	@Override
	public void onMessage(String agentId, String command, byte[] data, ServerConnection connection) {
		logger.debug("Received a ping message from {}: {}", agentId, new String(data));
		try {
			connection.send(agentId, command, "pong".getBytes());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onCommand(String agentId, String method, String command, List<String> pathParams,
			Map<String, String> queryParameters, ServerConnection connection) {

	}

}
