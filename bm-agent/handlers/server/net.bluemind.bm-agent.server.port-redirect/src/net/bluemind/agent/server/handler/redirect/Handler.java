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
package net.bluemind.agent.server.handler.redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.bluemind.agent.Connection;

public class Handler {

	protected final String clientId;
	protected final int clientPort;
	protected final int serverDestPort;
	protected final String id;
	protected final String command;
	protected final Connection connection;
	protected final String serverHost;
	ServerHandler serverHandler;

	private static final Logger logger = LoggerFactory.getLogger(Handler.class);

	public Handler(String id, String command, Connection connection, String clientId, String serverHost, int clientPort,
			int serverDestPort) {
		this.id = id;
		this.command = command;
		this.clientId = clientId;
		this.clientPort = clientPort;
		this.serverDestPort = serverDestPort;
		this.connection = connection;
		this.serverHost = serverHost;
	}

	public void connect() throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline p = ch.pipeline();
							serverHandler = new ServerHandler(Handler.this);
							p.addLast(serverHandler);
						}
					});

			ChannelFuture f = b.connect(serverHost, serverDestPort).sync();
		} finally {
			group.shutdownGracefully();
		}
	}

	public void write(byte[] value) {
		serverHandler.channelWrite(value);
	}

	public static class ServerHandler extends ChannelInboundHandlerAdapter {
		private final Handler con;
		private ChannelHandlerContext ctx;

		private Logger logger = LoggerFactory.getLogger(ServerHandler.class);

		public ServerHandler(Handler con) {
			this.con = con;
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			this.ctx = ctx;
		}

		public void channelWrite(byte[] data) {
			logger.info("Writing data to channel: {}", new String(data));
			ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(data.length);
			buffer.setBytes(0, data);
			ctx.writeAndFlush(buffer);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			ByteBuf buffer = (ByteBuf) msg;
			byte[] data = new byte[buffer.readableBytes()];
			buffer.readBytes(data);
			logger.info("Received data from server, redirecting to cloent: {}", new String(data));
			byte[] messageData = new JsonObject() //
					.putNumber("client-port", con.clientPort) //
					.putString("client-id", con.clientId) //
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
