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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.agent.server.AgentServerHandler;
import net.bluemind.agent.server.ServerConnection;
import net.bluemind.agent.server.handler.redirect.config.HostPortConfig;

public class PortRedirectServerHandler implements AgentServerHandler {

	Logger logger = LoggerFactory.getLogger(PortRedirectServerHandler.class);

	public static final Map<Integer, Listener> localServers = new ConcurrentHashMap<>();

	@Override
	public void onMessage(String agentId, String command, byte[] data, ServerConnection connection) {
		JsonObject obj = new JsonObject(new String(data));
		String clientId = obj.getString("client-id");
		int clientPort = obj.getInteger("client-port");
		byte[] value = obj.getBinary("data");

		logger.info("Received data for from server for client port {}, id: {}", clientPort, clientId);
		localServers.get(clientPort).receive(clientId, value);

	}

	@Override
	public void onInitialize(String agentId, String command, List<String> pathParams,
			Map<String, String> queryParameters, ServerConnection connection) {
		String host = queryParameters.get("host");
		int port = Integer.parseInt(queryParameters.get("port"));
		int localPort = Integer.parseInt(queryParameters.get("localPort"));

		logger.info("Initializing Port Redirection. LocalPort: {}, Host: {}, Port: {}", localPort, host, port);

		HostPortConfig hostPortConfig = new HostPortConfig(host, port, localPort);

		Listener listener = new Listener(agentId, command, connection, hostPortConfig);
		localServers.put(hostPortConfig.localPort, listener);
		try {
			listener.start();
		} catch (Exception e) {
			logger.warn("Cannot start port redirection listener");
		}
	}

}
