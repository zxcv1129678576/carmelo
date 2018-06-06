package carmelo.netty;

import carmelo.netty.websocket.ChildChannelHandler;
import carmelo.servlet.Servlet;
import carmelo.common.Configuration;
import carmelo.netty.http.HttpServerInitializer;
import carmelo.netty.tcp.TcpServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * game server bootstrap
 * 
 * @author needmorecode
 *
 */
public class GameServerBootstrap {

	public void run() {

		// init servlet
		final Servlet servlet = new Servlet();
		servlet.init();

		// camelo supports both tcp and http
		
		// http channel
		new Thread() {
			public void run() {
				EventLoopGroup bossGroup2 = new NioEventLoopGroup(1);
				EventLoopGroup workerGroup2 = new NioEventLoopGroup();
				try {
					ServerBootstrap b2 = new ServerBootstrap();
					b2.group(bossGroup2, workerGroup2)
							.channel(NioServerSocketChannel.class)
							.childHandler(new HttpServerInitializer(servlet, false));
					int port = Integer.parseInt(Configuration.getProperty(Configuration.HTTP_PORT));
					b2.bind(port).sync().channel().closeFuture().sync();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					bossGroup2.shutdownGracefully();
					workerGroup2.shutdownGracefully();
				}
			}
		}.start();
		
		// http push channel
		new Thread() {
			public void run() {
				EventLoopGroup bossGroup2 = new NioEventLoopGroup(1);
				EventLoopGroup workerGroup2 = new NioEventLoopGroup();
				try {
					ServerBootstrap b2 = new ServerBootstrap();
					b2.group(bossGroup2, workerGroup2)
							.channel(NioServerSocketChannel.class)
							.childHandler(new HttpServerInitializer(servlet, true));
					int port = Integer.parseInt(Configuration.getProperty(Configuration.HTTP_PUSH_PORT));
					b2.bind(port).sync().channel().closeFuture().sync();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					bossGroup2.shutdownGracefully();
					workerGroup2.shutdownGracefully();
				}
			}
		}.start();

		new Thread() {
			public void run() {
				System.out.println("===========================Netty-ws端口启动========");
				EventLoopGroup bossGroup_ws = new NioEventLoopGroup();
				EventLoopGroup workGroup_ws = new NioEventLoopGroup();
				try {
					ServerBootstrap b = new ServerBootstrap();
					b.group(bossGroup_ws, workGroup_ws);
					b.channel(NioServerSocketChannel.class);
					b.childHandler(new ChildChannelHandler());
					System.out.println("服务端开启等待客户端连接 ... ...");
					int port = Integer.parseInt(Configuration.getProperty(Configuration.WS_PORT));
					Channel ch = b.bind(port).sync().channel();
					ch.closeFuture().sync();
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					bossGroup_ws.shutdownGracefully();
					workGroup_ws.shutdownGracefully();
				}
			}
		}.start();



		new Thread() {
			public void run() {
				// tcp channel
				System.out.println("===========================Netty-tcp端口启动========");
				final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
				final EventLoopGroup workerGroup = new NioEventLoopGroup();
				try {
					ServerBootstrap b1 = new ServerBootstrap();
					b1.group(bossGroup, workerGroup)
							.channel(NioServerSocketChannel.class)
							.childHandler(new TcpServerInitializer(servlet));
					int port = Integer.parseInt(Configuration.getProperty(Configuration.TCP_PORT));
					b1.bind(port).sync().channel().closeFuture().sync();

				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					bossGroup.shutdownGracefully();
					workerGroup.shutdownGracefully();
				}
			}
		}.start();


	}

}
