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
package net.bluemind.agent.config;

import org.vertx.java.core.json.JsonObject;

public class SSLConfig {

	private boolean ssl;
	private String keyStore;
	private String keyStorePassword;
	private String trustStore;
	private String trustStorePassword;
	private boolean authRequired;

	public SSLConfig() {

	}

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public String getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(String trustStore) {
		this.trustStore = trustStore;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public boolean isAuthRequired() {
		return authRequired;
	}

	public void setAuthRequired(boolean authRequired) {
		this.authRequired = authRequired;
	}

	public static SSLConfig fromJson(JsonObject config) {
		SSLConfig sslConfigs = new SSLConfig();
		if (config.containsField("sslConfig")) {
			JsonObject sslConfig = config.getObject("sslConfig");
			sslConfigs.ssl = sslConfig.getBoolean("ssl", false);
			sslConfigs.authRequired = sslConfig.getBoolean("authRequired", false);
			sslConfigs.keyStore = sslConfig.getString("keyStore", "");
			sslConfigs.keyStorePassword = sslConfig.getString("keyStorePassword", "");
			sslConfigs.trustStore = sslConfig.getString("trustStore", "");
			sslConfigs.trustStorePassword = sslConfig.getString("trustStorePassword", "");
		}
		return sslConfigs;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.putBoolean("ssl", ssl);
		json.putBoolean("authRequired", authRequired);
		json.putString("keyStore", keyStore);
		json.putString("keyStorePassword", keyStorePassword);
		json.putString("trustStore", trustStore);
		json.putString("trustStorePassword", trustStorePassword);
		return json;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Enabled: %s,", new Boolean(ssl).toString()));
		if (ssl) {
			sb.append(String.format("KeyStore: %s,", keyStore));
			sb.append(String.format("KeyStore password set: %s,", isSet(keyStorePassword)).toString());
			sb.append(String.format("TrustStore: %s,", trustStore));
			sb.append(String.format("TrustStore password set: %s,", new Boolean(trustStorePassword.length() > 0))
					.toString());
		}
		if (authRequired) {
			sb.append(String.format("Auth required: %s,", "true"));
		}

		return sb.toString();
	}

	private String isSet(String value) {
		if (null == value) {
			return "false";
		}
		return new Boolean(value.length() > 0).toString();
	}

}
