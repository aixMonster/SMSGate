package com.zx.sms.connect.manager.cmpp;

import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.common.GlobalConstance;
import com.zx.sms.connect.manager.AbstractEndpointConnector;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.handler.cmpp.CMPPDeliverLongMessageHandler;
import com.zx.sms.handler.cmpp.CMPPSubmitLongMessageHandler;
import com.zx.sms.session.AbstractSessionStateManager;
import com.zx.sms.session.cmpp.SessionStateManager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;

public class CMPPServerChildEndpointConnector extends AbstractEndpointConnector {
	private static final Logger logger = LoggerFactory.getLogger(CMPPServerChildEndpointConnector.class);
	
	public CMPPServerChildEndpointConnector(CMPPEndpointEntity endpoint) {
		super(endpoint);
	}

	@Override
	public ChannelFuture open() throws Exception {
		//TODO 子端口打开，说明有客户连接上来
		return null;
	}


	@Override
	protected AbstractSessionStateManager createSessionManager(EndpointEntity entity, ConcurrentMap storeMap,
			boolean preSend) {
		return new SessionStateManager(entity, storeMap, preSend);
	}

	@Override
	protected void doBindHandler(ChannelPipeline pipe, EndpointEntity cmppentity) {
		CMPPEndpointEntity entity = (CMPPEndpointEntity)cmppentity;

		//处理长短信
		pipe.addLast("CMPPDeliverLongMessageHandler", new CMPPDeliverLongMessageHandler(entity));
		pipe.addLast("CMPPSubmitLongMessageHandler",  new CMPPSubmitLongMessageHandler(entity));
		
		pipe.addLast("CmppActiveTestRequestMessageHandler", GlobalConstance.activeTestHandler);
		pipe.addLast("CmppActiveTestResponseMessageHandler", GlobalConstance.activeTestRespHandler);
		pipe.addLast("CmppTerminateRequestMessageHandler", GlobalConstance.terminateHandler);
		pipe.addLast("CmppTerminateResponseMessageHandler", GlobalConstance.terminateRespHandler);
	}

	@Override
	protected SslContext createSslCtx() {
		return null;
	}

	@Override
	protected void initSslCtx(Channel ch, EndpointEntity entity) {
	}

	@Override
	protected void doinitPipeLine(ChannelPipeline pipeline) {
		
	}
}
