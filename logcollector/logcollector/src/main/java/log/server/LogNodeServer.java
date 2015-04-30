package log.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import log.process.persist.ClientSessionPool;
import log.process.persist.HornetQPersistent;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;

public class LogNodeServer {
	public static void main(String[] args) throws Exception {
		InputStream in = LogNodeServer.class.getClassLoader().getResourceAsStream("logstore.properties");
		Properties p = new Properties();
		p.load(in);
		in.close();
		
		String hqHost = p.getProperty("hqHost");
		int hqPort = Integer.parseInt(p.getProperty("hqPort"));
		
		int logstorePort = Integer.parseInt(p.getProperty("logstorePort"));
		
		int poolSize = Integer.parseInt(p.getProperty("poolSize"));
		
		
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ClientSessionFactory sf = null;
		try {
			sf = createSessionFactory(hqHost,hqPort);
			ClientSessionPool.init(poolSize, sf);
			Runtime.getRuntime().addShutdownHook(new ShutdownThread(sf,HornetQPersistent.getThreads()));
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 100)
					.option(ChannelOption.SO_KEEPALIVE, true)		
					// .handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(
					new LogNodeChannelInitialier());
			
			// Start the server.
			ChannelFuture f = b.bind(logstorePort).sync();

			// Wait until the server socket is closed.
			f.channel().closeFuture().sync();
		} finally {
			// Shut down all event loops to terminate all threads.
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
	private static ClientSessionFactory createSessionFactory(String host, int port) throws Exception {
		try {
			Map<String, Object> connParams = new HashMap<String, Object>();
			connParams.put(TransportConstants.HOST_PROP_NAME, host);
			connParams.put(TransportConstants.PORT_PROP_NAME, port);
			TransportConfiguration config = new TransportConfiguration(NettyConnectorFactory.class.getName(), connParams);

			ServerLocator serverLocator = HornetQClient.createServerLocatorWithoutHA(config);
			serverLocator.setConsumerWindowSize(-1);
			serverLocator.setReconnectAttempts(-1);
			serverLocator.setBlockOnDurableSend(false);
			return serverLocator.createSessionFactory();
		} catch (Exception e) {
			throw e;
		}
	}
}
