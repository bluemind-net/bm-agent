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
package net.bluemind.agent.client.handler.redirect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.bluemind.agent.Connection;
import net.bluemind.agent.client.handler.redirect.PortRedirectClientHandler.HostPortConfig;

public class Listener {

	protected static Logger logger = LoggerFactory.getLogger(Listener.class);

	public final String id;
	public final String command;
	public final Connection connection;
	public final HostPortConfig hostPortConfig;
	public final Map<String, ServerHandler> serverHandlers;

	public Listener(String id, String command, Connection connection, HostPortConfig hostPortConfig) {
		this.id = id;
		this.command = command;
		this.connection = connection;
		this.hostPortConfig = hostPortConfig;
		serverHandlers = new HashMap<>();
	}

	public void start() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							String clientId = UUID.randomUUID().toString();
							ServerHandler handler = new ServerHandler(Listener.this, clientId);
							serverHandlers.put(clientId, handler);
							ch.pipeline().addLast(handler);
						}
					}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

			ChannelFuture f = b.bind(hostPortConfig.localPort).sync();

			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public void receive(String clientId, byte[] data) {
		serverHandlers.get(clientId).channelWrite(data);
	};

	public static class ServerHandler extends ChannelInboundHandlerAdapter {
		private final Listener con;
		private final String clientId;
		private ChannelHandlerContext ctx;

		public ServerHandler(Listener con, String clientId) {
			this.con = con;
			this.clientId = clientId;
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			this.ctx = ctx;
		}

		public void channelWrite(byte[] data) {
			ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(data.length);
			buffer.setBytes(0, data);
			ctx.write(buffer);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			ByteBuf buffer = (ByteBuf) msg;
			byte[] data = new byte[buffer.readableBytes()];
			buffer.readBytes(data);
			logger.info("Received data from client, redirecting to server: {}", new String(data));
			byte[] messageData = new JsonObject() //
					.putString("server-host", con.hostPortConfig.serverHost) //
					.putNumber("server-dest-port", con.hostPortConfig.remotePort) //
					.putNumber("client-port", con.hostPortConfig.localPort) //
					.putString("client-id", clientId) //
					.putBinary("data", data).asObject().encode().getBytes();
			con.connection.send(con.id, con.command, messageData);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			ctx.close();
			logger.error("An error occured while reading from channel", cause);
		}
	}

}
