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

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.agent.Connection;
import net.bluemind.agent.client.AgentClientHandler;
import net.bluemind.agent.client.handler.redirect.config.HostPortConfig;

public class PortRedirectClientHandler implements AgentClientHandler {

	Logger logger = LoggerFactory.getLogger(PortRedirectClientHandler.class);
	public static final Map<Integer, Listener> localServers = new ConcurrentHashMap<>();

	@Override
	public void onMessage(byte[] data) {

		JsonObject obj = new JsonObject(new String(data));
		String clientId = obj.getString("client-id");
		int clientPort = obj.getInteger("client-port");
		byte[] value = obj.getBinary("data");

		logger.info("Received data for from server for client port {}, id: {}", clientPort, clientId);
		localServers.get(clientPort).receive(clientId, value);
	}

	@Override
	public void onInitialize(String id, String command, Connection connection) {

		logger.info("Initializing Port Redirections");

		List<HostPortConfig> config = readConfig();

		logger.info("Found {} port redirection configurations", config.size());

		for (HostPortConfig hostPortConfig : config) {
			logger.info("Starting up connection {}", hostPortConfig);
			Listener listener = new Listener(id, command, connection, hostPortConfig);
			localServers.put(hostPortConfig.localPort, listener);
			Runnable t = () -> {
				try {
					listener.start();
				} catch (Exception e) {
					logger.warn("Cannot start port redirection listener");
				}
			};
			new Thread(t).start();
		}
	}

	private List<HostPortConfig> readConfig() {
		String filepath = System.getProperty("bm-agent-port-config", "/etc/bm/agent/port-config");

		try {
			String data = new String(Files.readAllBytes(new File(filepath).toPath()));
			ObjectMapper mapper = new ObjectMapper();

			return mapper.readValue(data,
					mapper.getTypeFactory().constructCollectionType(List.class, HostPortConfig.class));
		} catch (Exception e) {
			logger.warn("Cannot load port-redirection config from {}", filepath, e);
		}
		return Collections.emptyList();
	}

}
