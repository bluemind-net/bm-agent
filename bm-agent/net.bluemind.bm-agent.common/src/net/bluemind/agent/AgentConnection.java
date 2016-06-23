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
package net.bluemind.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.WebSocketBase;

public class AgentConnection<T extends WebSocketBase<T>> implements Connection {

	private final String command;
	private final String id;
	private final T websocket;
	private MessageParser parser;

	private static final Logger logger = LoggerFactory.getLogger(AgentConnection.class);

	public AgentConnection(String command, String id, T websocket) {
		this.command = command;
		this.id = id;
		this.websocket = websocket;
		parser = new MessageParser();
	}

	@Override
	public void send(byte[] data) throws Exception {
		Message message = new Message();
		message.setId(id);
		message.setCommand(command);
		message.setData(data);
		logger.info("sending {}", message);
		Buffer buffer = new Buffer(parser.write(message));
		websocket.write(buffer);
	}

}
