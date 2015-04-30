/*
 * FILE     :  ClientSessionPool.java
 *
 * CLASS    :  ClientSessionPool
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

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;

public class ClientSessionPool {
		/**
		 * Logger of ClientSessionPool class.
		 */
		private static final Logger logger = Logger.getLogger(ClientSessionPool.class);

		/**
		 * Client Session Queue to store client session.
		 */
		//private static ConcurrentLinkedQueue<ClientSession> clientSessionQueues = new ConcurrentLinkedQueue<ClientSession>();

		private static LinkedBlockingQueue<ClientSession> clientSessionQueue= new LinkedBlockingQueue<ClientSession>(100);
		/**
		 * Constructor.
		 * 
		 * @param maxSessionCount
		 * @param sessionFactory
		 */
		public static void init(final int maxSessionCount, final ClientSessionFactory sessionFactory) {
			try {
				for (int i = 0; i < maxSessionCount; i++) {
					clientSessionQueue.put(sessionFactory.createTransactedSession());
				}
				logger.info("Create session pool[" + maxSessionCount + "] success");
			} catch (Exception e) {
				logger.error("Create session pool failed.", e);
			}
		}

		/**
		 * Get an instance of client session in this pool if available
		 * 
		 * 
		 * @return an instance of the client session if available or null
		 */
		public static ClientSession getSessionInstanceIfFree() {
			try {
				ClientSession result = clientSessionQueue.take();
				return result;
			} catch (Exception e) {
				logger.error("Take session from pool failed.", e);
				return null;
			}
		}

		/**
		 * Free an instance of client session.
		 * 
		 * @param clientSession
		 * @return
		 */
		public static boolean freeSessionInstance(ClientSession clientSession) {
			try {
				if (!clientSessionQueue.contains(clientSession)) {
					clientSessionQueue.put(clientSession);
					return true;
				}
				return false;
			} catch (Exception e) {
				logger.error("Put session into pool failed.", e);
				return false;
			}
		}
}
