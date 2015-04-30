package log.server;

import log.server.handler.EightLengthFieldDecoder;
import log.server.handler.NettyServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
public class LogNodeChannelInitialier extends ChannelInitializer<SocketChannel> {
	
	public LogNodeChannelInitialier() {
		
	}
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		// TODO Auto-generated method stub
		ChannelPipeline pipeline = ch.pipeline();
		// pipeline.addLast("ssl",new SslHandler(engine));
//	    pipeline.addLast(new ThreeLengthFieldDecoder(Integer.MAX_VALUE,0,3,1,0));
		pipeline.addLast(new EightLengthFieldDecoder(Integer.MAX_VALUE,0,1));
//	pipeline.addLast(new LengthFieldPrepender(4,1,false));
//		pipeline.addLast(new LineBasedFrameDecoder(Integer.MAX_VALUE));
//		pipeline.addLast(new LogNodeHandler());
		pipeline.addLast("logHandlerGroup",new NettyServerHandler());
	}

}
