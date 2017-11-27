package net.bluemind.bm.agent.client.executable;

import java.io.File;

import org.apache.log4j.PropertyConfigurator;

import net.bluemind.agent.client.AgentClientModule;
import net.bluemind.agent.client.internal.config.ClientConfig;
import net.bluemind.agent.client.internal.config.ConfigReader;

public class Launcher {

	public static void main(String[] args) throws Exception {
		String path = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
		ClientConfig config = ConfigReader.readConfig("bm-agent-client-config", path + "/client-config.json");
		AgentClientModule.run(config, () -> {
			System.err.println("Application launched");
		});

		// block
		System.in.read();
		AgentClientModule.stopAll();
	}

}
