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
package net.bluemind.agent.server.internal;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

public class RequestParser {

	private static final Logger logger = LoggerFactory.getLogger(RequestParser.class);

	public static Command parse(HttpServerRequest request) {
		String method = request.method();
		String path = request.path();
		logger.info("Handling request to path {}", path);
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		String[] parts = path.split("/");

		String agentId = parts[0];
		String command = parts[1];
		String[] additionalParameters;
		if (parts.length == 2) {
			additionalParameters = new String[0];
		} else {
			additionalParameters = new String[parts.length - 2];
			System.arraycopy(parts, 2, additionalParameters, 0, additionalParameters.length);
		}

		Map<String, String> queryMap = new HashMap<>();
		MultiMap params = request.params();
		for (Map.Entry<String, String> entry : params.entries()) {
			String key = entry.getKey();
			String value = entry.getValue();
			queryMap.put(key, value);
		}
		return new Command(method, command, agentId, additionalParameters, queryMap);

	}

	public static class Command {
		public final String method;
		public final String command;
		public final String agentId;
		public final String[] pathParameters;
		public final Map<String, String> queryParameters;

		public Command(String method, String command, String agentId, String[] pathParameters,
				Map<String, String> queryParameters) {
			this.method = method;
			this.command = command;
			this.agentId = agentId;
			this.pathParameters = pathParameters;
			this.queryParameters = queryParameters;
		}

	}

}
