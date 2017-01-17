package net.bluemind.agent;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.platform.PlatformManager;

public class VertxHolder {

	public static final String DEFAULT = "default";

	public static Map<String, Vertx> vertices;
	public static Map<String, PlatformManager> pms;

	static {
		reset();
	}

	public static Vertx getVertx(String id) {
		return vertices.get(id);
	}

	public static PlatformManager getPm(String id) {
		return pms.get(id);
	}

	public static void reset() {
		vertices = new HashMap<>();
		pms = new HashMap<>();
	}

}
