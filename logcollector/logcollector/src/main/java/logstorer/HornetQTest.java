/*
 * FILE     :  HornetQTest.java
 *
 * CLASS    :  HornetQTest
 *
 * COPYRIGHT:
 *
 *   The computer systems, procedures, data bases and programs
 *   created and maintained by Qware Technology Group Co Ltd, are proprietary
 *   in nature and as such are confidential.  Any unauthorized
 *   use or disclosure of such information may result in civil
 *   liabilities.
 *
 *   Copyright Jan 30, 2015 by Qware Technology Group Co Ltd.
 *   All Rights Reserved.
*/
package logstorer;


import java.util.HashMap;
import java.util.Map;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.MessageHandler;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;

public class HornetQTest {

	public static void main(String[] args) throws Exception {
//		ClientSessionFactory sf = createSessionFactory("localhost",5445);
////    	produceMessage(sf);
//		consumerMessage(sf);
//		while(true);
////		sf.close();
		String test="<14>1 2014-10-28T09:44:39+00:00 loggregator ab939119-f99c-41dc-b901-22da186564df [RTR] - - dotnetap.tianjin.paas - [28/10/2014:09:44:31 +0000] \"GET / HTTP/1.1\" 200 503 \"http://172.31.42.54:28080/cfWeb/\" \"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36\" 172.31.42.28:56449 vcap_request_id:b8f77cd3aa169fa7f4da85fa40ec2042 response_time:0.006618517 app_id:ab939119-f99c-41dc-b901-22da186564df";
		System.out.println(test.length());
	}
	
	private static void produceMessage(ClientSessionFactory sf){
		try {
			ClientSession clientSession=sf.createSession();
			ClientProducer producer=clientSession.createProducer("jms.queue.sourceQueue");
			ClientMessage message=clientSession.createMessage(true);
			message.putIntProperty("number", 1);
			message.putStringProperty("log", "test");
			producer.send(message);
			clientSession.commit();
			producer.close();
			//clientSession.close();
			//sf.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void consumerMessage(ClientSessionFactory sf){
		try {
			
			final ClientSession clientSession=sf.createSession(false,true,true);
			clientSession.start();	
			ClientConsumer consumer=clientSession.createConsumer("jms.queue.sourceQueue");
			ClientMessage message=consumer.receive();
			System.out.println(message.getStringProperty("log"));
			message.acknowledge();
			//clientSession.commit();
//			consumer.setMessageHandler(new MessageHandler() {		
//				public void onMessage(ClientMessage message) {
//					// TODO Auto-generated method stub
//					System.out.println(message.getStringProperty("log"));
//					try {
//						message.acknowledge();
//						//clientSession.commit();
//					} catch (HornetQException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			});	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
