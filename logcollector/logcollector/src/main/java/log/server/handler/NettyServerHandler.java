package log.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.UnsupportedEncodingException;

import log.process.persist.HornetQPersistent;

import org.apache.log4j.Logger;
import org.hornetq.api.core.HornetQException;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {
	private static final Logger logger = Logger.getLogger(NettyServerHandler.class);
	int count;
	int num1=0;
	int num2=0;
	@Override
	public void exceptionCaught(ChannelHandlerContext arg0, Throwable arg1)
			throws Exception {
		arg0.close();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		super.channelReadComplete(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException, HornetQException {
		count=count+1;
		ByteBuf buffer = (ByteBuf) msg;
		byte[] bytes = new byte[buffer.readableBytes()];
		buffer.readBytes(bytes);
		String log = new String(bytes,"UTF-8");
		
		if(log.contains("[App/")){
			num1++;
			System.out.println("APP:"+num1);
		}
		if(log.contains("[RTR]")){
			num2++;
			System.out.println("RTR:"+num2);
		}
		System.out.println("COUNT:"+count);
		if("".equals(log))
			return;
		logger.info("源：" + log);
		//HornetQPersistent.produceMessage(count, log);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		ctx.close();
		super.channelInactive(ctx);
	}
}
