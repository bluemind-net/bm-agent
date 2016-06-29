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
package net.bluemind.agent.client.handler.redirect.config;

import java.io.File;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigReader {

	private static final int PORT = 8086;
	private static Logger logger = LoggerFactory.getLogger(ConfigReader.class);

	public static HostPortConfig readConfig(String property, String defaultPath) {
		String filepath = System.getProperty(property, defaultPath);

		try {
			String data = new String(Files.readAllBytes(new File(filepath).toPath()));
			ObjectMapper mapper = new ObjectMapper();

			return mapper.readValue(data, HostPortConfig.class);
		} catch (Exception e) {
			logger.warn("Cannot load config from {}, using defaults", filepath);
		}
		return new HostPortConfig("default", "localhost", PORT, PORT);
	}

}
