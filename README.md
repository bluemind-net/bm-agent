# bm-agent
BlueMind remote agent

# Description
bm-agent is an extensible client-server application.
It provides the connection management between the server and multiple clients via a Websocket connection.

Plugins on both client and server side can be registered via Eclipse Extension Points.

The application is intended to securely access services behind a firewall where connection establishment is not possible from outside the network.
The client initiates the connection from the internal network automatically at startup. 
Afterwards, the bidirectional connection allows the server to control and access services on the client side.

Additionally, The server side can be accessed via a REST API to send messages to registered plugins.

# Installation

##### Prerequisites:

To build the installation packages you need following tools installed on your system:

Java 8 JDK  
[Apache Maven 3](https://maven.apache.org/)  
[Docker](https://www.docker.com/)

##### Build the application.  From the root folder:  
```maven clean install```

##### Build the package:
From the folder p2:  
```maven clean install```

##### Build the installers:
From the folder packaging:  
```maven clean install```

You will find the packages for Debian and RedHat in the folders  
packaging/bm-agent-client/target/out  
packaging/bm-agent-server/target/out

# Configuration:

##### bm-agent-server:
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

# Starting the applications:

Be sure to start the server before starting up the clients.
Both server and client can be started by executing the init scripts  
/etc/init.d/bm-agent-server  
/etc/init.d/bm-agent-client

# Example - Port Redirecting:

bm-agent comes packaged with a ready-to-use port redirecting plugin. The plugin allows you to expose a port in the internal network (client side)
to a port on the server side.

Assuming you like to access an application on the client side on host 192.168.1.1 and port 2222 via the port 2223 on the server side (example server runs on port 8086) using an client with agentId agent1, you can initiate the port redirection via the REST command.

HTTP method: GET
Path: /agent1/port-redirect?port=2222&host=192.168.1.1&localPort=2223 

wget Example:
```wget "http://<server>:8086/agent1/port-redirect?port=2222&host=192.168.1.1&localPort=2223"```

This will open port 2223 on the server side. all data written to this socket will be transfered to client agentId and send to port 2222 on host 192.168.1.1 on the client side.

You can disable a port redirection by calling the same URL using the HTTP method DELETE.

