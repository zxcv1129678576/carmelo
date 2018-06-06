package carmelo.examples.server;
import java.io.IOException;

import carmelo.netty.GameServerBootstrap;
import carmelo.netty.WSGameServerBootstrap;

public class ServerMain {
	
	public static void main(String args[]) throws IOException{
//		System.out.println("服务端2收到1");
		new GameServerBootstrap().run();
//		System.out.println("服务端2收到：2" );
//		new WSGameServerBootstrap().run();
//		System.out.println("服务端2收到：3");
	}

}
