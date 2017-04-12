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
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStore {

	public static String storePath;

	private static final Logger logger = LoggerFactory.getLogger(ServerStore.class);

	public static String getStore(String pluginId) {
		if (storeActive()) {
			File file = getStoreFile(pluginId);
			logger.debug("Reading plugin store {} from {}", pluginId, file.getAbsolutePath());
			try {
				return new String(Files.readAllBytes(file.toPath()));
			} catch (IOException e) {
				logger.warn("Cannot load store for plugin %s:%s", pluginId, e.getMessage());
			}
		}
		return null;
	}

	public static void saveStore(String pluginId, String content) {
		if (storeActive()) {
			File file = getStoreFile(pluginId);
			logger.info("Saving plugin store {} to {}", pluginId, file.getAbsolutePath());
			try {
				Files.write(file.toPath(), content.getBytes(), StandardOpenOption.WRITE);
			} catch (IOException e) {
				logger.warn("Cannot load store for plugin %s:%s", pluginId, e.getMessage());
			}
		}
	}

	private static boolean storeActive() {
		return null != storePath && storePath.trim().length() > 0;
	}

	private static File getStoreFile(String pluginId) {
		File file = new File(storePath, String.format("bm-agent-%s.config", pluginId));
		return file;
	}

}
