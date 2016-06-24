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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.agent.Connection;
import net.bluemind.agent.client.AgentClientHandler;

public class PortRedirectClientHandler implements AgentClientHandler {

	Logger logger = LoggerFactory.getLogger(PortRedirectClientHandler.class);
	public final Map<Integer, Listener> localServers = new HashMap<>();

	@Override
	public void onMessage(byte[] data) {

		JsonObject obj = new JsonObject(new String(data));
		String clientId = obj.getString("client-id");
		String clientPort = obj.getString("client-port");
		byte[] value = obj.getBinary("data");

		logger.info("Received data for from server for client port {}, id: {}", clientPort, clientId);
		localServers.get(clientPort).receive(clientId, value);
	}

	@Override
	public void onInitialize(String id, String command, Connection connection) {

		logger.info("Initializing Port Redirections");

		HostPortConfig portConfig = new HostPortConfig("localhost", 8037, 8036);
		Listener listener = new Listener(id, command, connection, portConfig);
		localServers.put(portConfig.localPort, listener);
		Runnable t = () -> {
			try {
				listener.start();
			} catch (Exception e) {
				logger.warn("Cannot start port redirection listener");
			}
		};
		new Thread(t).start();
	}

	public static class HostPortConfig {
		public final String serverHost;
		public final int remotePort;
		public final int localPort;

		public HostPortConfig(String serverHost, int remotePort, int localPort) {
			this.serverHost = serverHost;
			this.remotePort = remotePort;
			this.localPort = localPort;
		}
	}

}
