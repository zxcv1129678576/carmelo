package carmelo.netty.websocket;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
/**
 * ClassName:ChildChannelHandler
 * Function: TODO ADD FUNCTION.
 * @author hxy
 */
public class ChildChannelHandler extends ChannelInitializer<SocketChannel>{
    @Override
    protected void initChannel(SocketChannel e) throws Exception {
// ����30��û�ж������ݣ��򴥷�һ��READER_IDLE�¼���
    // pipeline.addLast(new IdleStateHandler(30, 0, 0));
// HttpServerCodec���������Ӧ����Ϣ����ΪHTTP��Ϣ


        e.pipeline().addLast("http-codec",new HttpServerCodec());
// HttpObjectAggregator����HTTP��Ϣ�Ķ�����ֺϳ�һ��������HTTP��Ϣ
        e.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
// ChunkedWriteHandler����ͻ��˷���HTML5�ļ�
        e.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
// �ڹܵ�����������Լ��Ľ�������ʵ�ַ���
        e.pipeline().addLast("handler",new MyWebSocketServerHandler());
    }
}

