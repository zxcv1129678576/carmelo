package carmelo.examples.server;
import java.io.IOException;

import carmelo.netty.GameServerBootstrap;
import carmelo.netty.WSGameServerBootstrap;

public class ServerMain {
	
	public static void main(String args[]) throws IOException{
//		System.out.println("�����2�յ�1");
		new GameServerBootstrap().run();
//		System.out.println("�����2�յ���2" );
//		new WSGameServerBootstrap().run();
//		System.out.println("�����2�յ���3");
	}

}
