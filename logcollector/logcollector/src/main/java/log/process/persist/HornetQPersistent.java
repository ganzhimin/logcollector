/*
 * FILE     :  HornetQPersistent.java
 *
 * CLASS    :  HornetQPersistent
 *
 * COPYRIGHT:
 *
 *   The computer systems, procedures, data bases and programs
 *   created and maintained by Qware Technology Group Co Ltd, are proprietary
 *   in nature and as such are confidential.  Any unauthorized
 *   use or disclosure of such information may result in civil
 *   liabilities.
 *
 *   Copyright Jan 27, 2015 by Qware Technology Group Co Ltd.
 *   All Rights Reserved.
*/
package log.process.persist;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;

public class HornetQPersistent{
	static ExecutorService es = Executors.newFixedThreadPool(1000);

	public static void produceMessage(final int i,final String log) throws HornetQException{
		es.execute(new Thread(){
			@Override
			public void run(){
				try {
					ClientSession session = ClientSessionPool.getSessionInstanceIfFree();
					ClientProducer producer = session.createProducer("jms.queue.sourceQueue");
					ClientMessage message = session.createMessage(true);
					message.putIntProperty("number", i);
					
					
					message.putStringProperty("log", log);
					producer.send(message);
					session.commit();
					producer.close();
					ClientSessionPool.freeSessionInstance(session);
				} catch (HornetQException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		});
	}
	
	public static ExecutorService getThreads(){
		return es;
	}

}
