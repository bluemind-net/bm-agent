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
package net.bluemind.agent.client.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.bluemind.agent.client.AgentClientHandler;

public class HandlerRegistry {

	private Map<String, AgentHandler> handlers;

	private HandlerRegistry() {
		this.handlers = new HashMap<>();
	}

	public Optional<AgentHandler> get(String command) {
		return Optional.ofNullable(handlers.get(command));
	}

	public void register(String command, AgentClientHandler handler, String info) {
		handlers.put(command, new AgentHandler(handler, info));
	}

	private static class Holder {
		private static final HandlerRegistry INSTANCE = new HandlerRegistry();
	}

	public static HandlerRegistry getInstance() {
		return Holder.INSTANCE;
	}

	public class AgentHandler {
		public final AgentClientHandler handler;
		public final String info;

		public AgentHandler(AgentClientHandler handler, String info) {
			this.handler = handler;
			this.info = info;
		}
	}

}
