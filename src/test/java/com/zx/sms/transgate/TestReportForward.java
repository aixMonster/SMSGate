package com.zx.sms.transgate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.BaseMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverResponseMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.codec.cmpp.wap.UniqueLongMsgId;
import com.zx.sms.common.util.MsgId;
import com.zx.sms.connect.manager.EndpointEntity.SupportLongMessage;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.connect.manager.TestConstants;
import com.zx.sms.connect.manager.cmpp.CMPPClientEndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPResponseSenderHandler;
import com.zx.sms.connect.manager.cmpp.CMPPServerChildEndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPServerEndpointEntity;
import com.zx.sms.handler.api.AbstractBusinessHandler;
import com.zx.sms.handler.api.BusinessHandlerInterface;
import com.zx.sms.handler.api.gate.SessionConnectedHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;

public class TestReportForward {
	private static final Logger logger = LoggerFactory.getLogger(TestReportForward.class);

	/**
	 * 测试短信网接收接收，回复，转发状态报告
	 * S1 模拟运营商网关 
	 * Ts 模拟转发网关分配置的Sp服务账号 
	 * Tc 模拟转发上游通道端账号 
	 * C1 模拟一个Sp用户
	 * 消息路径 
	 * Submit C1 --> Ts ===> Tc --> S1 
	 * Report S1 --> Tc ===> Ts --> C1
	 * 
	 * @throws InterruptedException
	 */

	private Map<String, ImmutablePair<AtomicInteger, ImmutablePair<UniqueLongMsgId, Map<Integer, MsgId>>>> uidMap = new ConcurrentHashMap<String, ImmutablePair<AtomicInteger, ImmutablePair<UniqueLongMsgId, Map<Integer, MsgId>>>>();
	private Map<String, UniqueLongMsgId> msgIdMap = new ConcurrentHashMap<String, UniqueLongMsgId>();

	@Test
	public void testReportForward() throws InterruptedException {
		int count =TestConstants.Count; //发送消息总数
		
		
		int port = 26890;
		String s1Id = createS1(port); // 创建运营商
		Thread.sleep(1000);
		String tcId = createTc(port); // 转发器连到运营商
		Thread.sleep(1000);
		int tsport = 26891;
		String tsId = createTS(tcId, tsport); // 转发器的服务端收到的消息都转到tcId
		Thread.sleep(1000);

		String cid = "C1";
		CMPPClientEndpointEntity client = new CMPPClientEndpointEntity();
		client.setId(cid + "client");
//		client.setLocalhost("127.0.0.1");
		// client.setLocalport(65521);
		client.setHost("127.0.0.1");
		client.setPort(tsport);
		client.setChartset(Charset.forName("utf-8"));
		client.setGroupName("test");
		client.setUserName("test01");
		client.setPassword("1qaz2wsx");

		client.setMaxChannels((short) 1);
		client.setVersion((short) 0x20);
		client.setRetryWaitTimeSec((short) 30);
		client.setMaxRetryCnt((short) 1);
		client.setCloseWhenRetryFailed(false);
		client.setUseSSL(false);
//		 client.setWriteLimit(150);
		client.setWindow(16);
		client.setReSendFailMsg(false);
		client.setSupportLongmsg(SupportLongMessage.BOTH);
		List<BusinessHandlerInterface> clienthandlers = new ArrayList<BusinessHandlerInterface>();
		
		
		
		//用于检查收到的状态和response的msgId是否一样
		final Map<String,String> checkMsgIdMap = new ConcurrentHashMap<String, String>();
		DefaultPromise sendover = new DefaultPromise(GlobalEventExecutor.INSTANCE);
		SessionConnectedHandler sender = new SessionConnectedHandler(new AtomicInteger(count),sendover) {

			@Override
			protected BaseMessage createTestReq(String content) {
				CmppSubmitRequestMessage msg = new CmppSubmitRequestMessage();
				msg.setDestterminalId("13800138005");
				msg.setSrcId("100869");
				msg.setLinkID("0000");
				msg.setMsgContent(content
						+ " 16:28:40.453 [busiWo中国rk-6] IN0.453 [busiWork-6] INFO  c.z.s.h.a.s.MessageReceiveHandler - channels : 1,ToFO  c.z.s.h.a.s.MessageReceiveHandler - channels : 1,Totle Receive Msg Num:5001,   speed : 0/s");
				msg.setRegisteredDelivery((short) 1);
				msg.setServiceId("10086");
				return msg;

			}
			
			public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
				
				super.channelRead(ctx, msg);
				
				if (msg instanceof CmppDeliverRequestMessage) { 
					
					//收状态
					CmppDeliverRequestMessage e = (CmppDeliverRequestMessage) msg;
					
					CmppDeliverResponseMessage responseMessage = new CmppDeliverResponseMessage(e.getHeader().getSequenceId());
					responseMessage.setResult(0);
					responseMessage.setMsgId(e.getMsgId());
					ctx.channel().writeAndFlush(responseMessage);
					
					if(e.isReport()) {
						checkMsgIdMap.remove(e.getReportRequestMessage().getMsgId().toString());
					}

				} else if (msg instanceof CmppSubmitResponseMessage) {
					
					//收Response
					CmppSubmitResponseMessage e = (CmppSubmitResponseMessage) msg;
					checkMsgIdMap.put(e.getMsgId().toString(), "");

				}  else {
					ctx.fireChannelRead(msg);
				}
			}

		};
		clienthandlers.add(sender);
		client.setBusinessHandlerSet(clienthandlers);
		EndpointManager.INS.openEndpoint(client);
		Thread.sleep(1500);
		//等待Sp所有短信发送完
		try {
			logger.info("等待Sp所有短信发送完...." );
			sendover.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		while (uidMap.size() > 0) {
			logger.info("等待所有状态报告回来...." +"size...." + uidMap.size() + ".." );
			Thread.sleep(1000);
		}
		
		EndpointManager.INS.close(EndpointManager.INS.getEndpointEntity(client.getId()));
		EndpointManager.INS.close(EndpointManager.INS.getEndpointEntity(tsId));
		EndpointManager.INS.close(EndpointManager.INS.getEndpointEntity(tcId));
		EndpointManager.INS.close(EndpointManager.INS.getEndpointEntity(s1Id));
		Thread.sleep(1000);
		logger.info("检查状态报告是否完全匹配上...." );
		Assert.assertEquals(0,checkMsgIdMap.size());
	}

	private String createS1(int port) {

		String Sid = "S1";
		CMPPServerEndpointEntity server = new CMPPServerEndpointEntity();
		server.setId(Sid);
		server.setHost("0.0.0.0");
		server.setPort(port);
		server.setValid(true);
		// 使用ssl加密数据流
		server.setUseSSL(false);

		CMPPServerChildEndpointEntity child = new CMPPServerChildEndpointEntity();
		child.setId(Sid + "child");
		child.setChartset(Charset.forName("utf-8"));
		child.setGroupName("test");
		child.setUserName("test01");
		child.setPassword("1qaz2wsx");

		child.setValid(true);
		child.setVersion((short) 0x20);

		child.setMaxChannels((short) 10);
		child.setRetryWaitTimeSec((short) 30);
		child.setMaxRetryCnt((short) 3);

		child.setReSendFailMsg(false);
		List<BusinessHandlerInterface> serverhandlers = new ArrayList<BusinessHandlerInterface>();

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
				return "AddS1ResponseSenderHandler";
			}

		});
		child.setBusinessHandlerSet(serverhandlers);
		server.addchild(child);
		EndpointManager.INS.openEndpoint(server);
		return server.getId();
	}

	private String createTc(int serverPort) throws InterruptedException {
		String cid = "TC";
		CMPPClientEndpointEntity client = new CMPPClientEndpointEntity();
		client.setId(cid + "client");
//		client.setLocalhost("127.0.0.1");
		// client.setLocalport(65521);
		client.setHost("127.0.0.1");
		client.setPort(serverPort);
		client.setChartset(Charset.forName("utf-8"));
		client.setGroupName("test");
		client.setUserName("test01");
		client.setPassword("1qaz2wsx");

		client.setMaxChannels((short) 10);
		client.setVersion((short) 0x20);
		client.setRetryWaitTimeSec((short) 30);
		client.setMaxRetryCnt((short) 1);
		client.setCloseWhenRetryFailed(false);
		client.setUseSSL(false);
//		 client.setWriteLimit(150);
		client.setWindow(16);
		client.setReSendFailMsg(false);
		client.setSupportLongmsg(SupportLongMessage.BOTH);
		List<BusinessHandlerInterface> clienthandlers = new ArrayList<BusinessHandlerInterface>();
		clienthandlers.add(new AbstractBusinessHandler() {

			@Override
			public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
				ForwardResponseHander handler = new ForwardResponseHander(uidMap, msgIdMap);
				handler.setEndpointEntity(getEndpointEntity());
				ctx.pipeline().addAfter("sessionStateManager", handler.name(), handler);
				ctx.pipeline().remove(this);
			}
			@Override
			public String name() {
				return "AddForwardResponseSenderHandler";
			}
		});
		client.setBusinessHandlerSet(clienthandlers);
		EndpointManager.INS.openEndpoint(client);
		EndpointManager.INS.openEndpoint(client);
		EndpointManager.INS.openEndpoint(client);
		EndpointManager.INS.openEndpoint(client);
		EndpointManager.INS.openEndpoint(client);
		//等待连接建立 完成
		Thread.sleep(2000);
		return client.getId();
	}

	private String createTS(String forwardEid, int port) {

		String Sid = "TS";
		CMPPServerEndpointEntity server = new CMPPServerEndpointEntity();
		server.setId(Sid);
		server.setHost("0.0.0.0");
		server.setPort(port);
		server.setValid(true);
		// 使用ssl加密数据流
		server.setUseSSL(false);

		CMPPServerChildEndpointEntity child = new CMPPServerChildEndpointEntity();
		child.setId(Sid + "child");
		child.setChartset(Charset.forName("utf-8"));
		child.setGroupName("test");
		child.setUserName("test01");
		child.setPassword("1qaz2wsx");

		child.setValid(true);
		child.setVersion((short) 0x20);

		child.setMaxChannels((short) 10);
		child.setRetryWaitTimeSec((short) 30);
		child.setMaxRetryCnt((short) 3);

		child.setReSendFailMsg(false);

		child.setWindow(64); //加大窗口，加大状态报告发送速度
		
		List<BusinessHandlerInterface> serverhandlers = new ArrayList<BusinessHandlerInterface>();

		ForwardHander sender = new ForwardHander(forwardEid);

		serverhandlers.add(sender);

		serverhandlers.add(new AbstractBusinessHandler() {

			@Override
			public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
				ForwardResponseHander handler = new ForwardResponseHander(uidMap, msgIdMap);
				handler.setEndpointEntity(getEndpointEntity());
				ctx.pipeline().addAfter("sessionStateManager", handler.name(), handler);
				ctx.pipeline().remove(this);
			}

			@Override
			public String name() {
				return "AddForwardResponseSenderHandler";
			}

		});

		child.setBusinessHandlerSet(serverhandlers);
		server.addchild(child);
		
		
		//再加一个账号
		child = new CMPPServerChildEndpointEntity();
		child.setId(Sid + "child1");
		child.setChartset(Charset.forName("utf-8"));
		child.setGroupName("test");
		child.setUserName("test02");
		child.setPassword("1qaz2wsx");

		child.setValid(true);
		child.setVersion((short) 0x20);

		child.setMaxChannels((short) 10);
		child.setRetryWaitTimeSec((short) 30);
		child.setMaxRetryCnt((short) 3);

		child.setReSendFailMsg(false);

		child.setWindow(64); //加大窗口，加大状态报告发送速度
		
		serverhandlers = new ArrayList<BusinessHandlerInterface>();

		sender = new ForwardHander(forwardEid);

		serverhandlers.add(sender);

		serverhandlers.add(new AbstractBusinessHandler() {

			@Override
			public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
				ForwardResponseHander handler = new ForwardResponseHander(uidMap, msgIdMap);
				handler.setEndpointEntity(getEndpointEntity());
				ctx.pipeline().addAfter("sessionStateManager", handler.name(), handler);
				ctx.pipeline().remove(this);
			}

			@Override
			public String name() {
				return "AddForwardResponseSenderHandler";
			}

		});

		child.setBusinessHandlerSet(serverhandlers);
		server.addchild(child);
		
		EndpointManager.INS.openEndpoint(server);

		return server.getId();
	}
}
