package com.zju.logservice.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientRequestor;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.api.core.management.ManagementHelper;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;

import com.sshtools.j2ssh.SshClient;
import com.zju.logservice.machine.Machine;
import com.zju.logservice.util.RemoteExecutorUtil;






public class LogMonitor {
	private static final Logger logger = Logger.getLogger(LogMonitor.class);
	private String serverHost;//hornetq host,
	private String serverPort;//hornetq port
	private String adminName;//only admin has authority to monitor queue
	private String adminPwd;
	
	private String sourceQueue;//where the data stored
	private String managementQueue;//which queue is used to manage

	private int consumerNumOnOneNode;
	private int monitorInterval;//the interval of monitor to sample the storeQueue	
	private float capacity;//assume the consumer can consume 300 messages per second
	private int deadline;//the time limit for current consumers to consume remain messages,millisecond
	
	private ClientSession session;
	private ClientRequestor requestor;
	private ClientSessionFactory csf;
	
	private Queue<SshClient> idleClients;
	private Queue<SshClient> busyClients;
	private SshClient keepAliveClient;
	
	private String jarName;
	private String remoteJarDir;
	private String patternDir;
	private String remotePatternDir;
	
	private String consumerJarPath;
	private String killConsumerCmd;
	private String runConsumerCmd;
	private String checkConsumerCmd;
	
	//private DefaultCategoryDataset data;
	
	public LogMonitor(){
		
		try {
			initParams();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("init monitor params failed");
			System.exit(-1);
		}
		
		try {
			initClients();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("init machines failed");
			System.exit(-1);
		}
		
		try {
			initRequestor();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("init hornetq config failed");
			System.exit(-1);
		}
		
		try {
			keepAliveClient = this.idleClients.poll();
			if(keepAliveClient==null)
				throw new Exception();
			RemoteExecutorUtil.deploy(keepAliveClient,jarName,consumerJarPath,
					remoteJarDir,runConsumerCmd,patternDir,remotePatternDir);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("start keep-alive machine failed");
			System.exit(-1);
		}
		
		Runtime.getRuntime().addShutdownHook(
				new ShutdownThread(csf,idleClients,busyClients,
						keepAliveClient,killConsumerCmd,checkConsumerCmd));
		
		System.out.println("monitor init successfully");
	}

	private void initParams() throws Exception {
		// TODO Auto-generated method stub
		ClassLoader cl = this.getClass().getClassLoader();
		InputStream is = cl.getResourceAsStream("logmonitor.properties");
		Properties p = new Properties();
		p.load(is);
		is.close();
		serverHost = p.getProperty("serverHost");
		serverPort = p.getProperty("serverPort");
		adminName = p.getProperty("adminName");
		adminPwd = p.getProperty("adminPwd");
		sourceQueue = p.getProperty("sourceQueue");
		managementQueue = p.getProperty("managementQueue");
		consumerNumOnOneNode = Integer.parseInt(p.getProperty("consumerNumOnOneNode"));
		monitorInterval = Integer.parseInt(p.getProperty("monitorInterval"));
		capacity = Float.parseFloat(p.getProperty("capacity"));
		deadline = Integer.parseInt(p.getProperty("deadline"));
		
		remoteJarDir = p.getProperty("remoteJarDir");
		patternDir = p.getProperty("patternDir");
		remotePatternDir = p.getProperty("remotePatternDir");
		
		killConsumerCmd = p.getProperty("killConsumerCmd");
		checkConsumerCmd = p.getProperty("checkConsumerCmd");
		
		String jarDir = p.getProperty("jarDir");
		
		File f = new File(jarDir);
		if(!f.exists() || f.isFile()){
			System.out.println("consumer jar dir is invalid");
			System.exit(-1);
		}else{
			File[] fs = f.listFiles();
			if(fs.length!=1){
				System.out.println("multiple files exist in "+jarDir);
				System.exit(-1);
			}
			
			String temp = fs[0].getName();
			
			if(!temp.contains("logconsumer")){
				System.out.println("Invalid consumer jar name: "+temp);
				System.exit(-1);
			}
			consumerJarPath = fs[0].getAbsolutePath();
			jarName = temp;
		}
		
		if(serverHost==null || serverPort==null || adminName==null || adminPwd==null
				|| sourceQueue==null || managementQueue==null || remoteJarDir==null 
				|| patternDir==null || remotePatternDir==null || killConsumerCmd==null
				|| checkConsumerCmd == null){
			throw new Exception();
		}
		runConsumerCmd = "java -jar "+remoteJarDir+jarName+" "+serverHost+" "+serverPort+" "+consumerNumOnOneNode;
	}
	
	private void initClients() throws IOException{
		idleClients = new LinkedList<SshClient>();
		busyClients = new LinkedList<SshClient>();
		JSONArray array = JSONArray.fromObject(getMachinesFromJson());
		int size = array.size();
		for(int i=0;i<size;i++){
			JSONObject obj = array.getJSONObject(i);
			Machine m = (Machine) JSONObject.toBean(obj, Machine.class);
			SshClient sc = RemoteExecutorUtil.getSSHClient(m.getHost(), m.getUsername(), m.getPassword());
			if(sc==null){
				System.out.println("Machine at: "+m.getHost()+" is unavailable! ignore it");
				continue;
			}
			idleClients.add(sc);
			System.out.println("Add machine at: "+m.getHost()+" to machine list");	
		}
	}
	
	private String getMachinesFromJson() throws IOException{
		BufferedReader br =null;
		StringBuilder res = new StringBuilder();
		try {
			
			br = new BufferedReader(new InputStreamReader(
					this.getClass().getClassLoader().getResourceAsStream("machines")
					,"UTF-8"));
			String temp = null;
			while((temp = br.readLine())!=null){
				res.append(temp);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if(br!=null){
				br.close();
			}
		}		
		return res.toString();
	}
	
	private void initRequestor() throws Exception{
		Map<String, Object> connParams = new HashMap<String, Object>();
		connParams.put(TransportConstants.HOST_PROP_NAME, serverHost);
		connParams.put(TransportConstants.PORT_PROP_NAME, serverPort);
		TransportConfiguration config = new TransportConfiguration(NettyConnectorFactory.class.getName(), connParams);

		ServerLocator serverLocator = HornetQClient.createServerLocatorWithoutHA(config);
		csf = serverLocator.createSessionFactory();
		session = csf.createSession(adminName, adminPwd, false, true, true, true, 1024);
		requestor = new ClientRequestor(session,managementQueue);
	}
	
	private void addConsumer() throws Exception{
		SshClient target = this.idleClients.poll();
		if(target==null){
			System.out.println("No machine can be deployed!");
			return;
		}
		if(!RemoteExecutorUtil.deploy(target,jarName,consumerJarPath,remoteJarDir,runConsumerCmd,patternDir,remotePatternDir)){
			idleClients.add(target);
		}else{
			busyClients.add(target);
		}
	}
	
	private void removeConsumer() throws IOException{
		SshClient target = this.busyClients.poll();
		if(target==null){
			System.out.println("Unexpected consumers occur!!");
			return;
		}
		if(!RemoteExecutorUtil.release(target, killConsumerCmd,checkConsumerCmd)){
			busyClients.add(target);
		}else{
			idleClients.add(target);
		}
	}
	
	private void adjust(int messageNum, int consumerNum) throws Exception {			
		int expectNum = (int)(consumerNum * capacity * 0.001 * deadline);
		if(messageNum>expectNum){
			System.out.println("ADD");
			this.addConsumer();
		}else if(messageNum==expectNum){
			System.out.println("OK");
		}else{
			if(consumerNum>this.consumerNumOnOneNode && (messageNum<=(int)((consumerNum-this.consumerNumOnOneNode)* capacity * 0.001 * deadline))){
				System.out.println("REMOVE");
				this.removeConsumer();
			}else{
				System.out.println("OK");
			}
		}
	}
	
	private int getMessageCount() throws Exception{
		ClientMessage m = session.createMessage(false);
		ManagementHelper.putAttribute(m, sourceQueue, "messageCount");
		ClientMessage reply = requestor.request(m);
		return (Integer)ManagementHelper.getResult(reply);
	}
	
	private int getConsumerCount() throws Exception{
		ClientMessage m = session.createMessage(false);
		ManagementHelper.putAttribute(m, sourceQueue, "consumerCount");
		ClientMessage reply = requestor.request(m);
		return (Integer)ManagementHelper.getResult(reply);
	}
	
	public void monitor(){
		
		try {
			 session.start();
			 logger.info("monitor begin");
		} catch (HornetQException e) {
			// TODO Auto-generated catch block
			System.out.println("Exception occurs: "+e);
			e.printStackTrace();
			System.exit(-1);
		}
		
		int i=0;
		while(true){
			try {
				 int messageCount = this.getMessageCount();
				 int consumerCount = this.getConsumerCount(); 
				 
				 System.out.println((i++)+":"+this.sourceQueue + " contains " + messageCount + " messages and "+ consumerCount + " consumers");
				 
				 adjust(messageCount,consumerCount);
				 
				 Thread.sleep(monitorInterval*1000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
		 }
		
	}

	public static void main(String[] args){
		new LogMonitor().monitor();
	}
}
