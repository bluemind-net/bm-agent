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
package net.bluemind.agent.server.internal.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.vertx.java.core.http.ServerWebSocket;

public class ConnectionRegistry {

	private Map<String, ServerWebSocket> connections;

	private ConnectionRegistry() {
		this.connections = new HashMap<>();
	}

	public Optional<ServerWebSocket> get(String id) {
		return Optional.ofNullable(connections.get(id));
	}

	public void register(String id, ServerWebSocket connection) {
		connections.put(id, connection);
	}

	private static class Holder {
		private static final ConnectionRegistry INSTANCE = new ConnectionRegistry();
	}

	public static ConnectionRegistry getInstance() {
		return Holder.INSTANCE;
	}

	public String list() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Connections: %d\r\n", connections.size()));
		for (String agent : connections.keySet()) {
			sb.append(String.format("Connection: %s\r\n", agent));
		}
		return sb.toString();
	}

}
