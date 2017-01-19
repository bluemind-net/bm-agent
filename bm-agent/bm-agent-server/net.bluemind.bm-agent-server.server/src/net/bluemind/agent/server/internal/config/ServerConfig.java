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
package net.bluemind.agent.server.internal.config;

import org.vertx.java.core.json.JsonObject;

import net.bluemind.agent.config.SSLConfig;

public class ServerConfig {

	public String listenerAddress;
	public int port;
	public SSLConfig sslConfig;

	public ServerConfig() {

	}

	public ServerConfig(JsonObject config) {
		this(config.getString("listenerAddress"), config.getInteger("port"), SSLConfig.fromJson(config));
	}

	public ServerConfig(String listenerAddress, int port, SSLConfig sslConfig) {
		this.listenerAddress = listenerAddress;
		this.port = port;
		this.sslConfig = sslConfig;
	}

	@Override
	public String toString() {
		return String.format("%s: listenerAddress: %d, port: %s, SSL: %s", listenerAddress, port, sslConfig.toString());
	}

	public JsonObject toJsonObject() {
		JsonObject config = new JsonObject();
		config.putString("listenerAddress", listenerAddress);
		config.putNumber("port", port);
		if (null != sslConfig) {
			config.putObject("sslConfig", sslConfig.toJson());
		}
		return config;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getListenerAddress() {
		return listenerAddress;
	}

	public void setListenerAddress(String listenerAddress) {
		this.listenerAddress = listenerAddress;
	}

	public SSLConfig getSslConfig() {
		return sslConfig;
	}

	public void setSslConfig(SSLConfig sslConfig) {
		this.sslConfig = sslConfig;
	}

}
