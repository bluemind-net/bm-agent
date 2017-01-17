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
package net.bluemind.agent.client.internal.config;

import org.vertx.java.core.json.JsonObject;

public class ClientConfig {

	@Override
	public String toString() {
		return String.format("%s: host: %d, port: %s", host, port);
	}

	public JsonObject toJsonObject() {
		JsonObject config = new JsonObject();
		config.putString("host", host);
		config.putNumber("port", port);
		config.putString("agentId", agentId);
		return config;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String host;
	public int port;
	public String agentId;

	public ClientConfig() {

	}

	public ClientConfig(JsonObject config) {
		this(config.getString("host"), config.getInteger("port"), config.getString("agentId"));
	}

	public ClientConfig(String host, int port, String agentId) {
		this.host = host;
		this.port = port;
		this.agentId = agentId;
	}

}
