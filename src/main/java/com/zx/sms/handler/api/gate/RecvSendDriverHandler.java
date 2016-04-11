package com.zx.sms.handler.api.gate;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppReportRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppTerminateRequestMessage;
import com.zx.sms.codec.cmpp.msg.Message;
import com.zx.sms.common.util.ChannelUtil;
import com.zx.sms.common.util.MsgId;
import com.zx.sms.connect.manager.EventLoopGroupFactory;
import com.zx.sms.connect.manager.ExitUnlimitCirclePolicy;
import com.zx.sms.connect.manager.ServerEndpoint;
import com.zx.sms.connect.manager.cmpp.CMPPEndpointEntity;
import com.zx.sms.handler.api.AbstractBusinessHandler;
import com.zx.sms.session.cmpp.SessionState;

/**
 * 
 * @author kangyufeng(18852780@qq.com)
 *
 */
public class RecvSendDriverHandler extends AbstractBusinessHandler {
	private static final Logger logger = LoggerFactory.getLogger(RecvSendDriverHandler.class);
	//发送多少条
	private int totleCnt = 20;
	
	
	public int getTotleCnt() {
		return totleCnt;
	}
	public void setTotleCnt(int totleCnt) {
		this.totleCnt = totleCnt;
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

		if (evt == SessionState.Connect) {
			
			final CMPPEndpointEntity finalentity = (CMPPEndpointEntity) getEndpointEntity();
			final Channel ch = ctx.channel();
			EventLoopGroupFactory.INS.submitUnlimitCircleTask(new Callable<Boolean>() {
				private Message createTestReq() {
					
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("chanid", "Z005");
					map.put("b", "adf");
					
					if (finalentity instanceof ServerEndpoint) {
						CmppDeliverRequestMessage msg = new CmppDeliverRequestMessage();
						msg.setDestId("10085101");
						msg.setLinkid("0000");
						msg.setMsgContent("smrz");

						msg.setMsgId(new MsgId());
						msg.setRegisteredDelivery((short) 0);
						if (msg.getRegisteredDelivery() == 1) {
							msg.setReportRequestMessage(new CmppReportRequestMessage());
						}
						//msg.setServiceid("10085101");
						msg.setSrcterminalId("13800138000");
						msg.setSrcterminalType((short) 1);
						msg.setAttachment((Serializable)map);
						return msg;
					} else {
						CmppSubmitRequestMessage msg = new CmppSubmitRequestMessage();
						msg.setDestterminalId(String.valueOf(System.nanoTime()));
						msg.setLinkID("0000");
						msg.setMsgContent("下行测试");
						msg.setMsgid(new MsgId());
						msg.setServiceId("10086");
						msg.setSrcId("10086");
						msg.setAttachment((Serializable)map);
						return msg;
					}
				}

				@Override
				public Boolean call() throws Exception{			
					
				//	logger.info("last msg cnt : {}" ,totleCnt<0?0:totleCnt);
					while(totleCnt-->0) {
						ChannelFuture future =ChannelUtil.asyncWriteToEntity(getEndpointEntity(), createTestReq());
						future.sync();
					}
					return true;
				}
			}, new ExitUnlimitCirclePolicy() {
				@Override
				public boolean notOver(Future future) {
					boolean ret = ch.isActive() && totleCnt > 0;
					if(!ret){
						ChannelFuture promise =	ch.writeAndFlush(new CmppTerminateRequestMessage());
						promise.addListener(new GenericFutureListener<ChannelFuture>(){
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								ch.close();
							}
						});
					}
					return ret;
				}
			},0);
		}
		ctx.fireUserEventTriggered(evt);

	}
	@Override
	public String name() {
		return "SessionConnectedHandler-Gate";
	}

}
