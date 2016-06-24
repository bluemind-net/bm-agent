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
package net.bluemind.agent.client.internal.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.agent.client.AgentClientHandler;

public class PluginLoader {

	private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

	private static final String pluginId = "client";
	private static final String namespace = "net.bluemind.agent";
	private static final String pointName = "clientHandler";
	private static final String handlerElement = "handler";
	private static final String handlerAttribute = "impl";
	private static final String command = "command";
	private static final String name = "name";

	public static List<ClientHandler> load() {

		List<ClientHandler> plugins = new ArrayList<ClientHandler>();
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		if (null == extensionRegistry) {
			return plugins;
		}
		IExtensionPoint point = extensionRegistry.getExtensionPoint(namespace, pluginId);
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (handlerElement.equals(e.getName())) {
					try {
						AgentClientHandler handlerImpl = (AgentClientHandler) e
								.createExecutableExtension(handlerAttribute);
						String commandValue = e.getAttribute(command);
						String nameValue = e.getAttribute(name);
						ClientHandler ext = new ClientHandler(nameValue, commandValue, handlerImpl);
						plugins.add(ext);
						logger.info("Loaded Agent plugin {}", ext);
					} catch (CoreException ce) {
						;
						logger.error(ie.getNamespaceIdentifier() + ": " + ce.getMessage(), ce);
					}

				}
			}
		}
		logger.info("Loaded " + plugins.size() + " implementors of " + pluginId + "." + pointName);
		return plugins;
	}

	public static class ClientHandler {
		public final String name;
		public final String command;
		public final AgentClientHandler handler;

		public ClientHandler(String name, String command, AgentClientHandler handler) {
			this.name = name;
			this.command = command;
			this.handler = handler;
		}

		@Override
		public String toString() {
			return String.format("Agent Plugin: %s, Command: %s", this.name, this.command);
		}

	}

}
