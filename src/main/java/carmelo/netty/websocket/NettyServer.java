package carmelo.netty.websocket;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
/**
 * ClassName:NettyServer ע��ʽ��spring����
 * Function: TODO ADD FUNCTION.
 * @author hxy
 */
@Service
public class NettyServer {
    public static void main(String[] args) {
        new NettyServer().run();
    }
    @PostConstruct
    public void initNetty(){
        new Thread(){
            public void run() {
                new NettyServer().run();
            }
        }.start();
    }
    public void run(){
        System.out.println("===========================Netty�˿�����========");
// Boss�̣߳�������̳߳��ṩ���߳���boss����ģ����ڴ��������ӡ���socket�� ���е���������Ȼ�����Щsocket����worker�̳߳ء�
// �ڷ�������ÿ��������socket����һ��boss�߳��������ڿͻ��ˣ�ֻ��һ��boss�߳����������е�socket��
        EventLoopGroup bossGroup = new NioEventLoopGroup();
// Worker�̣߳�Worker�߳�ִ�����е��첽I/O�����������
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
// ServerBootstrap ����NIO����ĸ���������,�����ʼ��netty�����������ҿ�ʼ�����˿ڵ�socket����
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup);
// ���÷�����,������������accept������,���ڹ���serversocketchannel�Ĺ�����
            b.channel(NioServerSocketChannel.class);
// ChildChannelHandler �Գ�������ݽ��е�ҵ�����,��̳�ChannelInitializer
            b.childHandler(new ChildChannelHandler());
            System.out.println("����˿����ȴ��ͻ������� ... ...");
            Channel ch = b.bind(7397).sync().channel();
            ch.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}