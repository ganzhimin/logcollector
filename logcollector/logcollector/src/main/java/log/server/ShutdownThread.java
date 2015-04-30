package log.server;

import java.util.concurrent.ExecutorService;
import org.hornetq.api.core.client.ClientSessionFactory;

public class ShutdownThread extends Thread {
	ClientSessionFactory csf;
	ExecutorService es;
	public ShutdownThread(ClientSessionFactory csf,ExecutorService es){
		this.csf = csf;
		this.es = es;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(csf!=null){
			System.out.println("close connection and sessions to hornetq");
			csf.close();
			System.out.println("Done");
		}
		if(es!=null){
			System.out.println("release executor threads");
			es.shutdown();
			System.out.println("Done");
		}
	}
	
	
}
