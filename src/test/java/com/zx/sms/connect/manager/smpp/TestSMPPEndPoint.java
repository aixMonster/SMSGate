package com.zx.sms.connect.manager.smpp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.connect.manager.EndpointEntity.ChannelType;
import com.zx.sms.connect.manager.EndpointEntity.SupportLongMessage;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.handler.api.BusinessHandlerInterface;
import com.zx.sms.handler.api.smsbiz.MessageReceiveHandler;
/**
 *经测试，35个连接，每个连接每200/s条消息
 *lenovoX250能承担7000/s消息编码解析无压力。
 *10000/s的消息服务不稳定，开个网页，或者打开其它程序导致系统抖动，会有大量消息延迟 (超过500ms)
 *
 *低负载时消息编码解码可控制在10ms以内。
 *
 */


public class TestSMPPEndPoint {
	private static final Logger logger = LoggerFactory.getLogger(TestSMPPEndPoint.class);

	@Test
	public void testSMPPEndpoint() throws Exception {
	
		final EndpointManager manager = EndpointManager.INS;
		int port = RandomUtils.nextInt(55000, 56000);
		SMPPServerEndpointEntity server = new SMPPServerEndpointEntity();
		server.setId("smppserver");
		server.setHost("127.0.0.1");
		server.setPort(port);
		server.setValid(true);
		//使用ssl加密数据流
		server.setUseSSL(false);
		
		SMPPServerChildEndpointEntity child = new SMPPServerChildEndpointEntity();
		child.setId("smppchild");
		child.setSystemId("901782");
		child.setPassword("ICP");

		child.setValid(true);
		child.setChannelType(ChannelType.DUPLEX);
		child.setMaxChannels((short)3);
		child.setRetryWaitTimeSec((short)30);
		child.setMaxRetryCnt((short)3);
		child.setReSendFailMsg(false);
		child.setIdleTimeSec((short)15);
//		child.setWriteLimit(200);
//		child.setReadLimit(200);
		List<BusinessHandlerInterface> serverhandlers = new ArrayList<BusinessHandlerInterface>();
		MessageReceiveHandler receiver  = new SMPPMessageReceiveHandler();
		serverhandlers.add(receiver);   
		child.setBusinessHandlerSet(serverhandlers);
		server.addchild(child);
		
		SMPPClientEndpointEntity client = new SMPPClientEndpointEntity();
		client.setId("smppclient");
		client.setHost("127.0.0.1");
		client.setPort(port);
		client.setSystemId("901782");
		client.setPassword("ICP");
		client.setChannelType(ChannelType.DUPLEX);

		client.setMaxChannels((short)1);
		client.setRetryWaitTimeSec((short)100);
		client.setUseSSL(false);
		client.setReSendFailMsg(false);
//		client.setWriteLimit(200);
//		client.setReadLimit(200);
		client.setSupportLongmsg(SupportLongMessage.SEND);  //接收长短信时不自动合并
		List<BusinessHandlerInterface> clienthandlers = new ArrayList<BusinessHandlerInterface>();
		int count = 10000;
		clienthandlers.add( new SMPPSessionConnectedHandler(count)); 
		client.setBusinessHandlerSet(clienthandlers);
		
		manager.addEndpointEntity(server);
		manager.addEndpointEntity(client);
		manager.openAll();
		Thread.sleep(1000);
		System.out.println("start.....");
		boolean connection = EndpointManager.INS.getEndpointConnector(client).getConnectionNum() > 0;
		while (EndpointManager.INS.getEndpointConnector(client).getConnectionNum() > 0 &&   receiver.getCnt().get() < count) {
			connection = true;
			Thread.sleep(1000);
		}
		Assert.assertEquals(true, receiver.getCnt().get() == count || connection);
		EndpointManager.INS.close();
		EndpointManager.INS.removeAll();
		Assert.assertEquals(count, receiver.getCnt().get());
			

	}
}
