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
package net.bluemind.agent.server.internal.handler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.agent.VertxHolder;
import net.bluemind.agent.server.internal.AgentServer;

public class CommandReplyHandler implements Handler<Message<JsonObject>> {

	private final AtomicReference<String> ref;
	private final long commandId;
	private final CountDownLatch latch;

	public CommandReplyHandler(AtomicReference<String> ref, long commandId, CountDownLatch latch) {
		this.ref = ref;
		this.commandId = commandId;
		this.latch = latch;
	}

	@Override
	public void handle(Message<JsonObject> event) {
		long id = event.body().getLong("id");
		if (id == commandId) {
			ref.set(event.body().getString("response"));
			latch.countDown();
			VertxHolder.vertices.get(VertxHolder.DEFAULT).eventBus()
					.unregisterHandler(AgentServer.address_command_reply, this);
		}

	}

}
