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
      "port" : 8086
}
```
where ListenerAddress and port define the address and port the server should listen on.

##### bm-agent-client
The client will search the configuration file using following path:
/etc/bm/agent/client-config.json:
```javascript
{
      "agentId" : "agent-idX",
      "host" : "<server>",
      "port" : 8086
}
```
where agentId is a unique client identifier and host and port define the server host and port.

# Starting the application

Be sure to start the server before starting up the clients.
Both server and client can be started by executing the init scripts  
``` /etc/init.d/bm-agent-server start (stop, restart)```  
``` /etc/init.d/bm-agent-client start (stop, restart)```

# Logfiles

You will find the generated logfiles under  
```
/var/log/bm-agent-client/
```
```
/var/log/bm-agent-server/
```  

# Example - Port Redirecting

bm-agent comes packaged with a ready-to-use port redirecting plugin. The plugin allows you to expose a port in the internal network (client side)
to a port on the server side.

Assuming you like to access an application on the client side on host 192.168.1.1 and port 2222 via the port 2223 on the server side (example server runs on port 8086) using an client with agentId agent1, you can initiate the port redirection via the REST command.

HTTP method: GET
Path: /agent1/port-redirect?port=2222&host=192.168.1.1&localPort=2223 

wget Example:
```
wget "http://<server>:8086/agent1/port-redirect?port=2222&host=192.168.1.1&localPort=2223"
```

This will open port 2223 on the server side. all data written to this socket will be transfered to client agentId and send to port 2222 on host 192.168.1.1 on the client side.

You can disable a port redirection by calling the same URL using the HTTP method DELETE.

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

```
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

```
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
and restart the application
  
  

