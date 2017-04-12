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
package net.bluemind.agent.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStore {

	public static String storePath;

	private static final Logger logger = LoggerFactory.getLogger(ServerStore.class);

	public static String getStore(String pluginId) {
		if (storeActive()) {
			Path file = getStorePath(pluginId);
			logger.debug("Reading plugin store {} from {}", pluginId, file.toFile().getAbsolutePath());
			try {
				return new String(Files.readAllBytes(file));
			} catch (IOException e) {
				logger.warn("Cannot load store for plugin {}:{}", pluginId, e.getMessage());
			}
		}
		return null;
	}

	public static void saveStore(String pluginId, String content) {
		if (storeActive()) {
			Path file = getStorePath(pluginId);
			logger.info("Saving plugin store {} to {}", pluginId, file.toFile().getAbsolutePath());
			try {
				Files.write(file, content.getBytes());
			} catch (IOException e) {
				logger.warn("Cannot load store for plugin {}:{}", pluginId, e.getMessage());
			}
		}
	}

	private static boolean storeActive() {
		return null != storePath && storePath.trim().length() > 0;
	}

	private static Path getStorePath(String pluginId) {
		return new File(storePath, String.format("bm-agent-%s.config", pluginId)).toPath();
	}

}
