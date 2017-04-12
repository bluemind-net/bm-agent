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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Command {
	public final METHOD method;
	public final String command;
	public final String agentId;
	public final String[] pathParameters;
	public final Map<String, String> queryParameters;

	private static final Logger logger = LoggerFactory.getLogger(Command.class);

	public Command(String method, String command, String agentId, String[] pathParameters,
			Map<String, String> queryParameters) {
		this.method = METHOD.valueOf(method);
		this.command = command;
		this.agentId = agentId;
		this.pathParameters = pathParameters;
		this.queryParameters = queryParameters;
	}

	@SuppressWarnings("unchecked")
	public Command(JsonObject body) {
		this.method = METHOD.valueOf(body.getString("method"));
		this.command = body.getString("command");
		this.agentId = body.getString("agentId");
		JsonArray pParameters = body.getArray("pathParameters");
		String[] pathParams = new String[pParameters.size()];
		for (int i = 0; i < pParameters.size(); i++) {
			pathParams[i] = pParameters.get(i);
		}
		this.pathParameters = pathParams;
		Map<String, String> qParameters = null;
		try {
			qParameters = new ObjectMapper().readValue(body.getString("queryParameters"), HashMap.class);
		} catch (Exception e) {
			logger.warn("Cannot read queryParameters from command json object", e);
		}
		this.queryParameters = qParameters;
	}

	public JsonObject toJsonObject() {
		JsonArray pathParameters = new JsonArray();
		for (String param : this.pathParameters) {
			pathParameters.add(param);
		}

		String writeValueAsString = null;
		try {
			writeValueAsString = new ObjectMapper().writeValueAsString(this.queryParameters);
		} catch (JsonProcessingException e) {
			logger.warn("Cannot process query parameters", e);
		}
		JsonObject obj = new JsonObject() //
				.putString("agentId", this.agentId) //
				.putString("method", this.method.name()) //
				.putString("command", this.command) //
				.putString("queryParameters", writeValueAsString) //
				.putArray("pathParameters", pathParameters);
		return obj;
	}

	public static enum METHOD {
		GET, PUT, POST, DELETE;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agentId == null) ? 0 : agentId.hashCode());
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + Arrays.hashCode(pathParameters);
		result = prime * result + ((queryParameters == null) ? 0 : queryParameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Command other = (Command) obj;
		if (agentId == null) {
			if (other.agentId != null)
				return false;
		} else if (!agentId.equals(other.agentId))
			return false;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (!Arrays.equals(pathParameters, other.pathParameters))
			return false;
		if (queryParameters == null) {
			if (other.queryParameters != null)
				return false;
		} else if (!queryParameters.equals(other.queryParameters))
			return false;
		return true;
	}

}
