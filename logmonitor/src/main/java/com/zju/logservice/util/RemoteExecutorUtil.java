package com.zju.logservice.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.ScpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;

public class RemoteExecutorUtil {
	private static final Logger logger = Logger.getLogger(RemoteExecutorUtil.class);
	
	public static SshClient getSSHClient(String host,String name,String pwd) throws IOException{
		logger.info("ready to get ssh client");
		SshClient ssh = new SshClient();
		PasswordAuthenticationClient authentication = new PasswordAuthenticationClient();
		authentication.setUsername(name);
		authentication.setPassword(pwd);
		ssh.connect(host,new IgnoreHostKeyVerification());
		if(ssh.authenticate(authentication)==AuthenticationProtocolState.COMPLETE){
			logger.info("get ssh client successfully!");
			return ssh;
		}
		return null;
	}
	
	public static boolean deploy(SshClient sshClient,String jarName,String localfile,
			String remoteJarDir,String cmd, String patternDir, String remotePatternDir) throws Exception{
		SessionChannelClient session = null;
		try {
			session = sshClient.openSessionChannel();
			if(session.startShell()){
				OutputStream writer = session.getOutputStream();
				BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(session.getInputStream()));
				BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(session.getStderrInputStream()));
				
				logger.info("check jar ["+sshClient.getConnectionProperties().getHost()+":"+remoteJarDir+jarName+"]");
				writer.write(("ls "+remoteJarDir+jarName+"\n").getBytes());
				writer.flush();
				
				boolean exist =false;
				while(true){
					if(stdErrReader.ready()){
						String res = stdErrReader.readLine();
						if(res.startsWith("ls")){
							exist = false;
							break;
						}
					}
					if(stdoutReader.ready()){
						String res = stdoutReader.readLine();
						if(res.endsWith(jarName)){
							exist = true;
							break;
						}
					}
					continue;	
				}
				ScpClient scpClient = sshClient.openScpClient();
				if(!exist){
					logger.info("create remote jar dir :"+remoteJarDir);
					writer.write(("mkdir -p "+remoteJarDir+"\n").getBytes());
					writer.flush();
					
					logger.info("remove old jar files in: "+remoteJarDir);
					writer.write(("rm "+remoteJarDir+"*.jar\n").getBytes());
					writer.flush();
				
					logger.info("scp put jar from: "+localfile+" to "+remoteJarDir);
					scpClient.put(localfile, remoteJarDir, false);
				}else{
					logger.info("jar exists at ["+sshClient.getConnectionProperties().getHost()+":"+remoteJarDir+"]");
				}
				
			
				
				
				logger.info("create remote patterns dir :"+remotePatternDir);
				writer.write(("mkdir -p "+remotePatternDir+"\n").getBytes());
				writer.flush();
			
			
				logger.info("scp put patterns from: "+patternDir+" to "+remotePatternDir);
				scpClient.put(patternDir, remotePatternDir, true);
				
				
				logger.info("run cmd:["+cmd+"]");
				writer.write((cmd+"\n").getBytes());
				writer.flush();
				
				String temp = null;
				while((temp=stdoutReader.readLine())!=null){
					//System.out.println(temp);
					if(temp.equals("consumer starts successfully!")){
						logger.info(temp);
						session.close();
						return true;
					}
				}
			}	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.info("deploy failed!");
			e.printStackTrace();
		}
		try {
			if(session!=null)
				session.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean release(SshClient sshClient,String killCmd, String checkCmd) throws IOException{
		logger.info("ready to release machine: "+sshClient.getConnectionProperties().getHost()+" with cmd:["+killCmd+"]");
		SessionChannelClient session = null;
		try {
			session = sshClient.openSessionChannel();
			if(session.startShell()){
				OutputStream writer = session.getOutputStream();
				BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(session.getInputStream()));
				
				logger.info("release cmd:["+killCmd+"]");
				writer.write((killCmd+"\n").getBytes());
				
				
				writer.write((checkCmd+"\n").getBytes());	
				while(stdoutReader.ready()){
					stdoutReader.readLine();
					writer.write((checkCmd+"\n").getBytes());	
				}
				logger.info("release successfully!");
				session.close();
				return true;
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.info("release failed!");
			e.printStackTrace();
		} finally{
			try {
				if(session!=null)
					session.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
}
