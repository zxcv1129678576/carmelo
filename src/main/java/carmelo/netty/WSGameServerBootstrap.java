package carmelo.netty;

import carmelo.common.Configuration;
import carmelo.netty.websocket.ChildChannelHandler;
import carmelo.servlet.Servlet;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class WSGameServerBootstrap {

    public void run() {

        // init servlet
        final Servlet servlet = new Servlet();
        servlet.init();
        // ws channel
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
}
