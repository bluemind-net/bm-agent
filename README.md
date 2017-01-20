[logo]: https://avatars0.githubusercontent.com/u/13470640?v=3&s=200
![Bluemind][logo]
# bm-agent
BlueMind remote agent

# Description
bm-agent is an extensible client-server application.
It provides the connection management between the server and multiple clients via a Websocket connection.
bm-agent uses this Websocket connection to multiplex all connections between client and server plugins.

Plugins on both client and server side can be registered via Eclipse Extension Points.

The application is intended to securely access services behind a firewall where connection establishment is not possible from outside the network.
The client initiates the connection from the internal network automatically at startup. 
Afterwards, the bidirectional connection allows the server to control and access services on the client side.

Additionally, The server side can be accessed via a REST API to send messages to registered plugins.

![Overview](https://github.com/bluemind-net/bm-agent/blob/master/doc-img/bm-agent.png "Overview")

# Installation

##### Prerequisites

To build the installation packages you need following tools installed on your system:

Java 8 JDK  
[Apache Maven 3](https://maven.apache.org/)  
[Docker](https://www.docker.com/)

##### Build the application.  From the root folder
```
maven clean install
```

##### Build the package
From the folder p2:  
```
maven clean install
```

##### Build the installers:
From the folder packaging:  
```
maven clean install
```

You will find the packages for Debian and RedHat in the folders  
packaging/bm-agent-client/target/out  
packaging/bm-agent-server/target/out

# Configuration

##### bm-agent-server
The server will search the configuration file using following path:
/etc/bm/agent/server-config.json:
```javascript
{
      "listenerAddress" : "0.0.0.0",
      "port" : 8086,
      "sslConfig" : {
            "ssl" : true || false,
	        "keyStore" : <path to keystore>,
	        "keyStorePassword" : <keystore password>,
	        "authRequired" : true || false,
	        "trustStore" : <path to truststore>,
	        "trustStorePassword" : <truststore password>
      }
}
```
where ListenerAddress and port define the address and port the server should listen on.
If you do not intend to use SSL, you can omit the sslConfig object or set "ssl" to false. If the server should run in SSL mode, but no client authentication is required, the value "authRequired" should be set to false and "trustStore" and "trustStorePassword" can be omited.

##### bm-agent-client
The client will search the configuration file using following path:
/etc/bm/agent/client-config.json:
```javascript
{
      "agentId" : "agent-idX",
      "host" : "<server>",
      "port" : 8086,
      "sslConfig" : {
            "ssl" : true || false,
            "trustAll" : true || false,
	        "trustStore" : <path to truststore>,
	        "trustStorePassword" : <truststore password>,
	        "authRequired" : true || false,
	        "keyStore" : <path to keystore>,
	        "keyStorePassword" : <keystore password>,
}
```
where agentId is a unique client identifier and host and port define the server host and port.
If you do not intend to use SSL, you can omit the sslConfig object or set "ssl" to false. 
If the client should run in SSL mode, but you want to trust all server certificates, the value "trustAll" should be set to true and "trustStore" and "trustStorePassword" can be omited.
If the client should run in SSL mode, but no client authentication is required, the value "authRequired" should be set to false and "keyStore" and "keyStorePassword" can be omited.

# Starting the application

Both server and client can be started by executing the init scripts  
``` /etc/init.d/bm-agent-server start (stop, restart)```  
``` /etc/init.d/bm-agent-client start (stop, restart)```

It is not necessary to start the applications in a specific order.

# Logfiles

You will find the generated logfiles under  
```
/var/log/bm-agent-client/
```
```
/var/log/bm-agent-server/
```  

# Using bm-agent-server behind a proxy 

bm-agent-server uses following paths:
```
/bm-agent-ws
```
The websocket connection
```
/bm-agent/
```
The Command REST API

To forward requests via nginx, you can use this config (assuming your bm-agent-server runs on port 8086):
```Javascript
location /bm-agent-ws {
   proxy_pass $scheme://127.0.0.1:8086/bm-agent-ws;
   proxy_pass_request_headers      on;
   proxy_http_version 1.1;
   proxy_set_header Upgrade $http_upgrade;
   proxy_set_header Connection "upgrade";
  }

  location ~ ^/bm-agent/(.*)$ {
   proxy_pass $scheme://127.0.0.1:8086/bm-agent/$1/$is_args$args;
   proxy_pass_request_headers      on;
  }
```

# Example - Port Redirecting

bm-agent comes packaged with a ready-to-use port-forwarding plugin. The plugin allows you to expose a port in the internal network (client side)
to a port on the server side.

Assuming you like to access an application on the client side on host 192.168.1.1 and port 2222 via the port 2223 on the server side (example server runs on port 8086) using an client with agentId agent1, you can initiate the port redirection via the REST command.

HTTP method: GET
Path: /bm-agent/agent1/port-redirect?port=2222&host=192.168.1.1&localPort=2223 

wget Example:
```
wget "http://<server>:8086/bm-agent/agent1/port-redirect?port=2222&host=192.168.1.1&localPort=2223"
```

This will open port 2223 on the server side. all data written to this socket will be transfered to client agentId and send to port 2222 on host 192.168.1.1 on the client side.

You can disable a port redirection by calling the same URL using the HTTP method DELETE.

# Embedding bm-agent-client or bm-agent-server

Both, client and server, can be embedded in your Java application and used as a library by adding following artifacts to your dependencies:
```XML
<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>net.bluemind.bm-agent.common</artifactId>
	<version>3.1.0-SNAPSHOT</version>
</dependency>

<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>org.eclipse.equinox.nonosgi</artifactId>
	<version>3.1.0-SNAPSHOT</version>
</dependency>

<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>net.bluemind.bm-agent.runtime.deps</artifactId>
	<version>3.1.0-SNAPSHOT</version>
	<type>pom</type>
</dependency>
```
To embed the server, you will also need the artifact
```XML
<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>net.bluemind.bm-agent-server.server</artifactId>
	<version>3.1.0-SNAPSHOT</version>
</dependency>
```
To embed the client, you will need the artifact
```XML
<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>net.bluemind.bm-agent-server.client</artifactId>
	<version>3.1.0-SNAPSHOT</version>
</dependency>
```
The plugins ping and port forwarding are likewise available as maven artifacts:
```XML
<!-- ping handler implementations -->
<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>net.bluemind.bm-agent.client.ping</artifactId>
	<version>3.1.0-SNAPSHOT</version>
</dependency>

<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>net.bluemind.bm-agent.server.ping</artifactId>
	<version>3.1.0-SNAPSHOT</version>
</dependency>

<!-- port-redirect handler implementations -->
<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>net.bluemind.bm-agent.client.port-redirect</artifactId>
	<version>3.1.0-SNAPSHOT</version>
</dependency>

<dependency>
	<groupId>net.bluemind</groupId>
	<artifactId>net.bluemind.bm-agent.server.port-redirect</artifactId>
	<version>3.1.0-SNAPSHOT</version>
</dependency>
```

##### Server

To manage the server programmatically, the class "AgentServerModule" exposes 3 static methods
```
public static void run(ServerConfig config, Runnable doneHandler)
```
Starts the server using the submitted configuration. since the start is an asynchronous operation, you can submit a doneHandler which will be called once the server has been completely started and deployed all plugins.
```
public static void stop()
```
Stops the server
```
public static void command(Command command)
```
Executes a command. This can be used as an alternative to the REST command API to manage all interaction with the server programmatically. The REST API remains still available in this mode.  
Example:
To replace the REST API call 
```
"http://<server>/bm-agent/agent1/port-redirect?port=2222&host=192.168.1.1&localPort=2223"
```
you can use the command method as follows:
```Java
String[] pathParameters = new String[0];
Map<String, String> queryParameters = new HashMap<>();
queryParameters.put("port", String.valueOf(2222));
queryParameters.put("host", "192.168.1.1");
queryParameters.put("localPort", String.valueOf(2223));
Command command = new Command("GET", "port-redirect", "agent1", pathParameters, queryParameters);
AgentServerModule.command(command);
```

##### Client
Like the server, the client exposes its functionality using static methods
```
public static void run(ClientConfig config, Runnable doneHandler)
```
Starts a client using the agentId configured in the config parameter. The doneHandler will be called once the client has been started and deployed all plugins. You can start multiple clients, all communicating with different bm-agent servers.
```
public static void stop(String agentId)
```
Stops the specific client and undeploys all associated plugins
```
public static void stopAll()
```
Stops all clients

# Example
```Java
package net.bluemind.bm.agent.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.agent.client.AgentClientModule;
import net.bluemind.agent.client.internal.config.ClientConfig;
import net.bluemind.agent.config.SSLConfig;
import net.bluemind.agent.server.AgentServerModule;
import net.bluemind.agent.server.Command;
import net.bluemind.agent.server.internal.config.ServerConfig;

public class TestApplication {

	/*
	 * This example embeds bm-agent-server and bm-agent-client. To demonstrate
	 * the port forwarding feature, the method "startPortForwarding" will
	 * redirect localhost:8181 to localhost:22
	 */

	private static final String serverHost = "localhost";
	private static final String serverListenerAddress = "localhost";
	private static final int serverPort = 8080;

	// port forwarding config
	private static final String portForwardingTargetHost = "localhost";
	private static final int portForwardingTargetPort = 22;
	private static final int portForwardingLocalPort = 8181;

	private static final Logger logger = LoggerFactory.getLogger(TestApplication.class);

	public static void main(String[] args) throws Exception {

		TestApplication.startServer() //
				.thenCompose(TestApplication::startClient) //
				.thenRun(TestApplication::startPortForwarding) //
				.exceptionally((e) -> {
					logger.warn("Error while executing Test application", e);
					return null;
				});

		Thread.sleep(2000);
		waitForQuit();
	}

	private static void waitForQuit() {
		logger.info("Application started...");
		logger.info("the port-forwarding example will open port {} on locahost and redirecting all input to {}:{}",
				portForwardingLocalPort, portForwardingTargetHost, portForwardingTargetPort);
		logger.info("Type anything to QUIT");
		try {
			System.in.read();
		} catch (IOException e) {

		}
		AgentClientModule.stop();
		AgentServerModule.stop();
	}

	private static CompletableFuture<Void> startServer() {
		CompletableFuture<Void> future = new CompletableFuture<>();
		ServerConfig serverConfig = new ServerConfig(serverListenerAddress, serverPort, SSLConfig.noSSL());
		AgentServerModule.run(serverConfig, () -> {
			future.complete(null);
		});
		return future;
	}

	private static CompletableFuture<Void> startClient(Void ret) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		ClientConfig clientConfig = new ClientConfig(serverHost, serverPort, "agent-1", SSLConfig.noSSL());
		AgentClientModule.run(clientConfig, () -> {
			future.complete(null);
		});
		return future;
	}

	private static void startPortForwarding() {
		Command command = portForwardingExample();
		AgentServerModule.command(command);
	}

	private static Command portForwardingExample() {
		String[] pathParameters = new String[0];
		Map<String, String> queryParameters = new HashMap<>();
		queryParameters.put("port", String.valueOf(portForwardingTargetPort));
		queryParameters.put("host", portForwardingTargetHost);
		queryParameters.put("localPort", String.valueOf(portForwardingLocalPort));
		Command command = new Command("GET", "port-redirect", "agent-1", pathParameters, queryParameters);
		return command;
	}

}
```
See the project net.bluemind.bm-agent.application.test for the complete example code.

##### Classloader in embedded mode

When started via the published .deb packages, the client/server will run as an OSGI application using Eclipse Equinox as its target runtime. When using the embedded mode, the features provided by equinox will still work (for example the plugin lookup via Eclipse Extension Points), however there is a slightly different behaviour regarding the classloader, since all classes will be loaded by your applications classloader. In contrast to OSGI, dependencies used by bm-agent will be visible by your application classloader. 

# Developing a Plugin

the easiest way to develop a plugin is to copy the very simple existing ping plugin and use it as a template.
You will find the client and server parts in the folders:  
``` bm-agent/handlers/client/net.bluemind.bm-agent.client.ping/ ```  
``` bm-agent/handlers/server/net.bluemind.bm-agent.server.ping/ ```  

##### Creating the project

To develop a bm-agent plugin you will create 2 maven projects (client and server).
The easiest way to setup all required dependencies and repositories is to reference the file ` global/pom.xml ` which includes all needed dependencies.
Since the project uses Equinox as its target runtime, your pom.xml needs following packaging declaration:  
```
<packaging>eclipse-plugin</packaging>
```  

Typically, your project will contain following folders and files:    

+ src/
+ META-INF/MANIFEST.MF 
+ build.properties
+ pom.xml
+ plugin.xml

___

```
src/
```   
The source files  
___
```
META-INF/MANIFEST.MF
```  
The OSGI manifest file
```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: <your plugin name>
Bundle-SymbolicName: <your plugin name>;singleton:=true
Bundle-Version: 3.1.0.qualifier
Bundle-Vendor: <you>
Bundle-RequiredExecutionEnvironment: JavaSE-1.8
Require-Bundle: org.eclipse.osgi,
 net.bluemind.slf4j,
 net.bluemind.bm-agent.common,
 org.eclipse.core.runtime;bundle-version="3.11.1"
```
___
```
build.properties
```  
Build properties configuration
```
source.. = src/
bin.includes = META-INF/,\
               .,\
               plugin.xml
```
___
```
pom.xml
```  
Maven pom
___
```
plugin.xml
```  
Plugin configuration [see Registering the client](#registering-the-client)    

##### Creating the client implementation

To communicate with the client engine, you need to create a class which implements 
```
net.bluemind.agent.client.AgentClientHandler
```  
```Java
public interface AgentClientHandler {

   public void onInitialize(String command, String agentId, ClientConnection connection);

   public void onMessage(byte[] data);

}
```

The method ``` onInitialize ``` will be called when your plugin is loaded. It provides you with a connection instance.
You will use this connection object to send messages to the server part of your plugin.  
The method ``` onMessage ``` will be called when message from the server part of your plugin arrive.

##### Registering the client

bm-agent uses Eclipse Extension Points to lookup plugins during startup.
You can attach your plugin extension in the file plugin.xml:

```XML
<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>
   <extension
         point="net.bluemind.agent.client">
      <handler
            command="<command>"
            impl="<your class implementing AgentClientHandler>"
            name="A description">
      </handler>
   </extension>
</plugin>

 ```

```command``` defines a unique identifier used to connect your client and server plugins.  
```impl``` is the full path (package+class) to your implementation.

##### Creating the server implementation

To communicate with the server engine, you need to create a class which implements 
```
net.bluemind.agent.server.AgentServerHandler
```  
```Java
public interface AgentServerHandler {

   public void onMessage(String agentId, String command, byte[] data, ServerConnection connection);

   public void onCommand(Command command, ServerConnection connection);

}
```

The method ``` onMessage ``` will be called when message from the client part of your plugin arrive.  
The method ``` onCommand ``` will be called when REST messages for your plugin have been executed.

##### Registering the server

You can attach your plugin extension in the file plugin.xml:

```XML
<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>
   <extension
         point="net.bluemind.agent.server">
      <handler
            command="<command>"
            impl="<your class implementing AgentServerHandler>"
            name="A description">
      </handler>
   </extension>
</plugin>


 ```

``` command ``` defines a unique identifier used to connect your client and server plugins.
It must be the same as used by the client plugin.   
``` impl ``` is the full path (package+class) to your implementation.

##### Sending REST messages to your server plugin

After your server plugin has been loaded, you can send REST messages via the URL:

``` http://<server>:<port>/<agentId>/<command> ```

The server engine will call your plugin's onCommand method once it receives a command.
You can extend the path as needed. Additional path parameters will be submitted within the List parameter ``` pathParams ```.
URL Parameters will be submitted via the ``` queryParameters ``` Map object.

##### Deploying your plugin

To deploy your plugin you simply need to place it in the folder   
``` /usr/share/bm-agent-client/extensions/ ```  
respectively   
``` /usr/share/bm-agent-server/extensions/ ```  
and restart the application.
If the application is running as a library included into your application, the plugin must be included into the application's classpath.
  
  

