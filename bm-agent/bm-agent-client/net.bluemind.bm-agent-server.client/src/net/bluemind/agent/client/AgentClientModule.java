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
package net.bluemind.agent.client;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.agent.client.internal.AgentClient;

public class AgentClientModule implements BundleActivator {

	private static Logger logger = LoggerFactory.getLogger(AgentClientModule.class);

	@Override
	public void start(BundleContext context) throws Exception {
		logger.info("Starting BlueMind Agent Client");

		new AgentClient().start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		logger.info("Stopping BlueMind Agent Client");
	}

	public static void main(String[] args) throws Exception {
		new AgentClientModule().start(null);
	}

}
