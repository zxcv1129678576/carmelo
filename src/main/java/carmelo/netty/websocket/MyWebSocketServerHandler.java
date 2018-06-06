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
     * channel 通道 action 活跃的 当客户端主动链接服务端的链接后，这个通道就是活跃的了。也就是客户端与服务端建立了通信通道并且可以传输数据
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
// 添加
        Global.group.add(ctx.channel());
        System.out.println("客户端与服务端连接开启：" + ctx.channel().remoteAddress().toString());
    }
    /**
     * channel 通道 Inactive 不活跃的 当客户端主动断开服务端的链接后，这个通道就是不活跃的。也就是说客户端与服务端关闭了通信通道并且不可以传输数据
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
// 移除
        Global.group.remove(ctx.channel());
        System.out.println("客户端与服务端连接关闭：" + ctx.channel().remoteAddress().toString());
    }
    /**
     * 接收客户端发送的消息 channel 通道 Read 读 简而言之就是从通道中读取数据，也就是服务端接收客户端发来的数据。但是这个数据在不进行解码时它是ByteBuf类型的
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
     * channel 通道 Read 读取 Complete 完成 在通道读取完成后会在这个方法里通知，对应可以做刷新操作 ctx.flush()
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
     *  处理httpRequest
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
// 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
// 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
// 本例程仅支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("本例程仅支持文本消息，不支持二进制消息");
            throw new UnsupportedOperationException(
                    String.format("%s frame types not supported", frame.getClass().getName()));
        }
// 返回应答消息

        String request = ((TextWebSocketFrame) frame).text();
        System.out.println("服务端2收到：" + request);
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + "：" + request);
// 群发
       // ctx.writeAndFlush("111");
// 返回【谁发的发给谁】
 ctx.channel().writeAndFlush(tws);
    }
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
// 返回应答给客户端
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
// 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    /**
     * exception 异常 Caught 抓住 抓住异常，当发生异常的时候，可以做一些相应的处理，比如打印日志、关闭链接
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

    }
}