package carmelo.netty.websocket;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import carmelo.servlet.Request;
import carmelo.servlet.Response;
import carmelo.servlet.Servlet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * ClassName:MyWebSocketServerHandler Function: TODO ADD FUNCTION.
 *
 * @author hxy
 */
public class MyWebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = Logger.getLogger(WebSocketServerHandshaker.class.getName());
    private WebSocketServerHandshaker handshaker;

    private Servlet servlet;

    private HttpRequest currHttpRequest;

    private String command;

    private String params;

    /**
     * channel ͨ�� action ��Ծ�� ���ͻ����������ӷ���˵����Ӻ����ͨ�����ǻ�Ծ���ˡ�Ҳ���ǿͻ��������˽�����ͨ��ͨ�����ҿ��Դ�������
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
// ���
        Global.group.add(ctx.channel());
        System.out.println("�ͻ������������ӿ�����" + ctx.channel().remoteAddress().toString());
    }
    /**
     * channel ͨ�� Inactive ����Ծ�� ���ͻ��������Ͽ�����˵����Ӻ����ͨ�����ǲ���Ծ�ġ�Ҳ����˵�ͻ��������˹ر���ͨ��ͨ�����Ҳ����Դ�������
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
// �Ƴ�
        Global.group.remove(ctx.channel());
        System.out.println("�ͻ������������ӹرգ�" + ctx.channel().remoteAddress().toString());
    }
    /**
     * ���տͻ��˷��͵���Ϣ channel ͨ�� Read �� �����֮���Ǵ�ͨ���ж�ȡ���ݣ�Ҳ���Ƿ���˽��տͻ��˷��������ݡ�������������ڲ����н���ʱ����ByteBuf���͵�
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof HttpRequest) {
            handleHttpRequest(msg);
        }

        if (msg instanceof HttpContent) {
            handleHttpContent(  ctx,  msg);
        }


         if (msg instanceof WebSocketFrame) {
            System.out.println(handshaker.uri());
                handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
            }

    }
    /**
     * channel ͨ�� Read ��ȡ Complete ��� ��ͨ����ȡ��ɺ�������������֪ͨ����Ӧ������ˢ�²��� ctx.flush()
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * handleHttpContent
     * @param ctx
     * @param msg
     */
    public void handleHttpContent( ChannelHandlerContext ctx, Object msg){
        HttpContent httpContent = (HttpContent) msg;
        if (currHttpRequest.getMethod().equals(HttpMethod.POST)){
            params = httpContent.content().copy().toString();
        }

        Request request = new Request(0, command, params, "0", ctx);
        Response response = servlet.service(request);

        ByteBuf responseBuf = Unpooled.wrappedBuffer(response.getContents());


        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, responseBuf);
        httpResponse.headers().set(CONTENT_TYPE, "text/plain");
        httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());

        boolean keepAlive = isKeepAlive(currHttpRequest);
        if (!keepAlive) {
            ctx.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
        } else {
            httpResponse.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            ctx.write(httpResponse);
        }
    }
    /**
     *  ����httpRequest
     * @param msg
     */
    public void handleHttpRequest( Object msg){
        HttpRequest httpRequest = (HttpRequest) msg;
        this.currHttpRequest = httpRequest;
        if (httpRequest.getMethod().equals(HttpMethod.GET)){
            Pattern p = Pattern.compile(".*/command=(.*)\\?(.*)");
            Matcher m = p.matcher(httpRequest.getUri());
            if (m.matches()){
                command = m.group(1);
                params = m.group(2);
            }
        }
        else if (httpRequest.getMethod().equals(HttpMethod.POST)){
            Pattern p = Pattern.compile("http://(.*):(.*)/command=(.*)");
            Matcher m = p.matcher(httpRequest.getUri());
            if (m.matches()){
                command = m.group(3);
            }
        }
    }
    /**
     *
     * @param ctx
     * @param frame
     */
    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
// �ж��Ƿ�ر���·��ָ��
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
// �ж��Ƿ�ping��Ϣ
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
// �����̽�֧���ı���Ϣ����֧�ֶ�������Ϣ
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("�����̽�֧���ı���Ϣ����֧�ֶ�������Ϣ");
            throw new UnsupportedOperationException(
                    String.format("%s frame types not supported", frame.getClass().getName()));
        }
// ����Ӧ����Ϣ

        String request = ((TextWebSocketFrame) frame).text();
        System.out.println("�����2�յ���" + request);
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + "��" + request);
// Ⱥ��
       // ctx.writeAndFlush("111");
// ���ء�˭���ķ���˭��
 ctx.channel().writeAndFlush(tws);
    }
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
// ����Ӧ����ͻ���
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
// ����Ƿ�Keep-Alive���ر�����
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    /**
     * exception �쳣 Caught ץס ץס�쳣���������쳣��ʱ�򣬿�����һЩ��Ӧ�Ĵ��������ӡ��־���ر�����
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

    }
}