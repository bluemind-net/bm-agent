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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.agent.server.AgentServerHandler;
import net.bluemind.agent.server.Command;
import net.bluemind.agent.server.ServerConnection;
import net.bluemind.agent.server.ServerStore;
import net.bluemind.agent.server.handler.redirect.config.HostPortConfig;

public class PortRedirectServerHandler implements AgentServerHandler {

	Logger logger = LoggerFactory.getLogger(PortRedirectServerHandler.class);

	public static final Map<Integer, Listener> localServers = new ConcurrentHashMap<>();
	private static List<Command> activeCommands = new ArrayList<>();
	private static final String pluginStoreIdentifier = "PortRedirectServer";

	@Override
	public void onInitialize(ServerConnection connection) {
		logger.info("Looking up saved commands");
		String savedCommands = ServerStore.getStore(pluginStoreIdentifier);

		if (null != savedCommands) {
			JsonArray cmds = new JsonArray(savedCommands);
			logger.info("Found {} saved commands", cmds.size());
			for (int i = 0; i < cmds.size(); i++) {
				cmds.forEach(command -> {
					Command cmd = new Command((JsonObject) command);
					onCommand(cmd, connection);
				});
			}
		}
	}

	@Override
	public void onMessage(String agentId, String command, byte[] data, ServerConnection connection) {
		JsonObject obj = new JsonObject(new String(data));
		String clientId = obj.getString("client-id");
		int clientPort = obj.getInteger("client-port");
		byte[] value = obj.getBinary("data");
		String control = obj.getString("control");

		logger.debug("Received data for from client-agent for client port {}, id: {}", clientPort, clientId);
		logger.trace("data: {}", new String(value));
		if (!localServers.containsKey(clientPort)) {
			logger.warn("Client talks to non-existing Port handler: {}", clientPort);
		} else {
			localServers.get(clientPort).receive(clientId, value, control);
		}

	}

	@Override
	public String onCommand(Command command, ServerConnection connection) {
		logger.info("Handling port-redirect command {}:{}", command.method.name(), command.command);
		switch (command.method) {
		case GET:
			if (activeCommands.contains(command)) {
				logger.info("Skipping command {}:{}, command is already active", command.agentId, command.command);
				return null;
			}
			initializePortRedirection(command.agentId, command.command, command.queryParameters, connection);
			activeCommands.add(command);
			break;
		case DELETE:
			deletePortRedirection(command.agentId, command.command, command.queryParameters, connection);
			activeCommands.remove(command);
			break;
		case OPTIONS:
			JsonArray commands = new JsonArray();
			activeCommands.forEach(cmd -> {
				commands.addObject(cmd.toJsonObject());
			});
			return commands.encode();
		default:
		}
		syncSavedCommands();
		return null;
	}

	private void deletePortRedirection(String agentId, String command, Map<String, String> queryParameters,
			ServerConnection connection) {
		String host = queryParameters.get("host");
		int port = Integer.parseInt(queryParameters.get("port"));
		int localPort = Integer.parseInt(queryParameters.get("localPort"));

		logger.info("Deleting Port Redirection. LocalPort: {}, Host: {}, Port: {}", localPort, host, port);
		Listener listener = localServers.get(localPort);
		listener.stop();
		localServers.remove(localPort);
	}

	private void initializePortRedirection(String agentId, String command, Map<String, String> queryParameters,
			ServerConnection connection) {
		String host = queryParameters.get("host");
		int port = Integer.parseInt(queryParameters.get("port"));
		int localPort = Integer.parseInt(queryParameters.get("localPort"));

		HostPortConfig hostPortConfig = new HostPortConfig(host, port, localPort);

		if (localServers.containsKey(hostPortConfig.localPort)) {
			logger.info("Skipping Port Redirection. Redirection is already active. LocalPort: {}, Host: {}, Port: {}",
					localPort, host, port);
			return;
		}

		logger.info("Initializing Port Redirection. LocalPort: {}, Host: {}, Port: {}", localPort, host, port);
		Listener listener = new Listener(agentId, command, connection, hostPortConfig);
		localServers.put(hostPortConfig.localPort, listener);
		try {
			listener.start();
		} catch (Exception e) {
			logger.warn("Cannot start port redirection listener");
		}
	}

	private void syncSavedCommands() {
		JsonArray commands = new JsonArray();
		activeCommands.forEach(cmd -> {
			commands.addObject(cmd.toJsonObject());
		});
		logger.info("Saving {} active port forwarding commands", commands.size());
		ServerStore.saveStore(pluginStoreIdentifier, commands.encode());
	}

}
