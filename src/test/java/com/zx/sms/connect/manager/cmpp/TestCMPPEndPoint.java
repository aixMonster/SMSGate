package com.zx.sms.connect.manager.cmpp;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.connect.manager.EndpointEntity.SupportLongMessage;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.handler.api.AbstractBusinessHandler;
import com.zx.sms.handler.api.BusinessHandlerInterface;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;


/**
 * 经测试，35个连接，每个连接每200/s条消息 lenovoX250能承担7000/s消息编码解析无压力。
 * 10000/s的消息服务不稳定，开个网页，或者打开其它程序导致系统抖动，会有大量消息延迟 (超过500ms)
 *
 * 低负载时消息编码解码可控制在10ms以内。
 *
 */

public class TestCMPPEndPoint {
	private static final Logger logger = LoggerFactory.getLogger(TestCMPPEndPoint.class);

	@Test
	public void testCMPPEndpoint() throws Exception {
		ResourceLeakDetector.setLevel(Level.ADVANCED);
		final EndpointManager manager = EndpointManager.INS;
		int port = 16890;
		CMPPServerEndpointEntity server = new CMPPServerEndpointEntity();
		server.setId("server");
		server.setHost("0.0.0.0");
		server.setPort(port);
		server.setValid(true);
		// 使用ssl加密数据流
		server.setUseSSL(false);

		CMPPServerChildEndpointEntity child = new CMPPServerChildEndpointEntity();
		child.setId("cmppchild");
		child.setChartset(Charset.forName("utf-8"));
		child.setGroupName("test");
		child.setUserName("test01");
		child.setPassword("1qaz2wsx");

		child.setValid(true);
		child.setVersion((short) 0x20);

		child.setMaxChannels((short) 1);
		child.setRetryWaitTimeSec((short) 30);
		child.setMaxRetryCnt((short) 3);
		
		//不开启IP白名单
//		List<String> iplist = new ArrayList<String>();
//		iplist.add("192.168.98.48/18");
//		child.setAllowedAddr(iplist);
		
		child.setReSendFailMsg(false);
		// child.setWriteLimit(200);
		// child.setReadLimit(200);
		List<BusinessHandlerInterface> serverhandlers = new ArrayList<BusinessHandlerInterface>();

		CMPPMessageReceiveHandler receiver = new CMPPMessageReceiveHandler();
		serverhandlers.add(new AbstractBusinessHandler() {

		    @Override
		    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		    	CMPPResponseSenderHandler handler = new CMPPResponseSenderHandler();
		    	handler.setEndpointEntity(getEndpointEntity());
		    	ctx.pipeline().addAfter("sessionStateManager", handler.name(), handler);
		    	ctx.pipeline().remove(this);
		    }
			
			@Override
			public String name() {
				return "AddCMPPResponseSenderHandler";
			}
			
		});
		serverhandlers.add(receiver);
		child.setBusinessHandlerSet(serverhandlers);
		server.addchild(child);

//		manager.addEndpointEntity(server);

		CMPPClientEndpointEntity client = new CMPPClientEndpointEntity();
		client.setId("client");
//		client.setLocalhost("127.0.0.1");
		// client.setLocalport(65521);
		client.setHost("127.0.0.1");
		client.setPort(port);
		client.setChartset(Charset.forName("utf-8"));
		client.setGroupName("test");
		client.setUserName("test01");
		client.setPassword("1qaz2wsx");

		client.setMaxChannels((short) 1);
		client.setVersion((short) 0x20);
		client.setRetryWaitTimeSec((short) 30);
		client.setMaxRetryCnt((short)1);
		client.setCloseWhenRetryFailed(false);
		client.setUseSSL(false);
//		 client.setWriteLimit(150);
		client.setWindow(16);
		client.setReSendFailMsg(false);
		client.setSupportLongmsg(SupportLongMessage.BOTH);
		List<BusinessHandlerInterface> clienthandlers = new ArrayList<BusinessHandlerInterface>();
		int count = 5000;
		CMPPSessionConnectedHandler sender = new CMPPSessionConnectedHandler(count);
		clienthandlers.add(sender);
		client.setBusinessHandlerSet(clienthandlers);

		manager.addEndpointEntity(client);

		manager.openEndpoint(server);
		manager.openEndpoint(client);
		Thread.sleep(1000);
//		manager.startConnectionCheckTask();
		System.out.println("start.....");
		boolean connection = EndpointManager.INS.getEndpointConnector(client).getConnectionNum() > 0;
		while (EndpointManager.INS.getEndpointConnector(client).getConnectionNum()>0 && receiver.getCnt().get() < count) {
			Thread.sleep(1000);
			connection = true;
		}
		Assert.assertEquals(true, receiver.getCnt().get() == count || connection);
		EndpointManager.INS.close();
		EndpointManager.INS.removeAll();
		Assert.assertEquals(count, receiver.getCnt().get());
		System.out.println("end.....");
	}
}
