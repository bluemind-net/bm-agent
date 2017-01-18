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
package net.bluemind.bm.agent.test;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.agent.client.AgentClientModule;
import net.bluemind.agent.client.internal.config.ClientConfig;
import net.bluemind.agent.config.SSLConfig;
import net.bluemind.agent.server.AgentServerModule;
import net.bluemind.agent.server.Command;
import net.bluemind.agent.server.internal.config.ServerConfig;

public class TestApplication {

	private static final Logger logger = LoggerFactory.getLogger(TestApplication.class);

	public static void main(String[] args) throws Exception {

		ServerConfig serverConfig = new ServerConfig("localhost", 8080, SSLConfig.noSSL());
		AgentServerModule.run(serverConfig, () -> {
			System.out.println("server running");
			ClientConfig clientConfig = new ClientConfig("localhost", 8080, "agent-1", SSLConfig.noSSL());
			AgentClientModule.run(clientConfig, () -> {
				System.out.println("client  running");
				Command command = portForwardingExample("localhost");
				AgentServerModule.command(command);
			});
		});

		logger.info("Application is running... Type anything to stop");
		System.in.read();

		AgentClientModule.stop();
		AgentServerModule.stop();
	}

	private static Command portForwardingExample(String mySSHServer) {
		// this example will open a local port 8081 and redirecting all input to
		// port 22 of mySSHServer
		String[] pathParameters = new String[0];
		Map<String, String> queryParameters = new HashMap<>();
		queryParameters.put("port", "22");
		queryParameters.put("host", mySSHServer);
		queryParameters.put("localPort", "8081");
		Command command = new Command("GET", "port-redirect", "agent-1", pathParameters, queryParameters);
		return command;
	}

}
