package com.zju.logservice.monitor;

import java.io.IOException;
import java.util.Queue;

import org.hornetq.api.core.client.ClientSessionFactory;

import com.sshtools.j2ssh.SshClient;
import com.zju.logservice.util.RemoteExecutorUtil;

public class ShutdownThread extends Thread {	
	Queue<SshClient> busyClients;
	Queue<SshClient> idleClients;
	SshClient keepAliveClient;
	String killCmd;
	String checkCmd;
	ClientSessionFactory csf;
	
	
	public ShutdownThread(ClientSessionFactory csf, Queue<SshClient> idleClients, Queue<SshClient> busyClients, 
			SshClient keepAliveClient, String killCmd, String checkCmd){
		this.busyClients = busyClients;
		this.idleClients = idleClients;
		this.keepAliveClient = keepAliveClient;
		this.killCmd = killCmd;
		this.checkCmd = checkCmd;
		this.csf = csf;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("shutdown thread invoke");
		
		if(csf!=null){
			System.out.println("close connection to hornetq");
			csf.close();
			System.out.println("Done");
		}
		System.out.println("Kill consumers at: "+keepAliveClient.getConnectionProperties().getHost());
		try {
			RemoteExecutorUtil.release(keepAliveClient, killCmd,checkCmd);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Done");
		for(SshClient sc:busyClients){
			System.out.println("Kill consumers at: "+sc.getConnectionProperties().getHost());
			try {
				RemoteExecutorUtil.release(sc, killCmd,checkCmd);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Done");
		}
		
		for(SshClient sc:busyClients){
			System.out.println("close connection to machine at: "+sc.getConnectionProperties().getHost());
			sc.disconnect();
			System.out.println("Done");
		}
		for(SshClient sc:idleClients){
			System.out.println("close connection to machine at: "+sc.getConnectionProperties().getHost());
			sc.disconnect();
			System.out.println("Done");
		}
		System.out.println("close connection to machine at: "+keepAliveClient.getConnectionProperties().getHost());
		keepAliveClient.disconnect();
		System.out.println("Done");
		
	}
	
}
