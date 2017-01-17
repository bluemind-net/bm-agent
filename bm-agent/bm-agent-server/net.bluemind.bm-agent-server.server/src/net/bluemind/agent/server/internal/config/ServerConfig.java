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

public class ServerConfig {

	@Override
	public String toString() {
		return String.format("%s: listenerAddress: %d, port: %s", listenerAddress, port);
	}

	public JsonObject toJsonObject() {
		JsonObject config = new JsonObject();
		config.putString("listenerAddress", listenerAddress);
		config.putNumber("port", port);
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

	public String listenerAddress;
	public int port;

	public ServerConfig() {

	}

	public ServerConfig(JsonObject config) {
		this(config.getString("listenerAddress"), config.getInteger("port"));
	}

	public ServerConfig(String listenerAddress, int port) {
		this.listenerAddress = listenerAddress;
		this.port = port;
	}

}
