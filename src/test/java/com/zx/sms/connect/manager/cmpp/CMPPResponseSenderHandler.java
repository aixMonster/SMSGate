package com.zx.sms.connect.manager.cmpp;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverResponseMessage;
import com.zx.sms.codec.cmpp.msg.CmppQueryRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppQueryResponseMessage;
import com.zx.sms.codec.cmpp.msg.CmppReportRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.common.util.CachedMillisecondClock;
import com.zx.sms.connect.manager.EndpointConnector;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.handler.api.AbstractBusinessHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;


public class CMPPResponseSenderHandler extends AbstractBusinessHandler {
	
	boolean isDelay = false;
	
	public CMPPResponseSenderHandler(boolean isDelay ) {
		this.isDelay = isDelay;
	}
	
	public CMPPResponseSenderHandler( ) {
	}
	
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
    	
    	//此时未经过长短信合并
    	if (msg instanceof CmppDeliverRequestMessage) {
    		CmppDeliverRequestMessage e = (CmppDeliverRequestMessage) msg;
    		CmppDeliverResponseMessage responseMessage = new CmppDeliverResponseMessage(e.getHeader().getSequenceId());
			responseMessage.setResult(0);
			responseMessage.setMsgId(e.getMsgId());
			ctx.channel().writeAndFlush(responseMessage);
    	}else if (msg instanceof CmppSubmitRequestMessage) {
    		CmppSubmitRequestMessage e = (CmppSubmitRequestMessage) msg;
    		
    		final CmppSubmitResponseMessage resp = new CmppSubmitResponseMessage(e.getHeader().getSequenceId());
			resp.setResult(0);
			
			
			final CmppDeliverRequestMessage deliver = new CmppDeliverRequestMessage();
			if(e.getRegisteredDelivery()==1) {
				deliver.setDestId(e.getSrcId());
				deliver.setSrcterminalId(e.getDestterminalId()[0]);
				CmppReportRequestMessage report = new CmppReportRequestMessage();
				report.setDestterminalId(deliver.getSrcterminalId());
				report.setMsgId(resp.getMsgId());
				String t = DateFormatUtils.format(CachedMillisecondClock.INS.now(), "yyMMddHHmm");
				report.setSubmitTime(t);
				report.setDoneTime(t);
				report.setStat("DELIVRD");
				report.setSmscSequence(0);
				deliver.setReportRequestMessage(report);

				
			}
			//模拟状态报告先于response回来
			//随机延迟，response和状态不一定谁先回
				int delay = isDelay ?RandomUtils.nextInt(0,100):0;
				ctx.executor().schedule(new Runnable() {
					public void run() {
						//response从同一连接回去
						ctx.channel().writeAndFlush(resp);
					}
				},delay,TimeUnit.MILLISECONDS);
				
				 delay = RandomUtils.nextInt(0,100);
				ctx.executor().schedule(new Runnable() {
					public void run() {
						 EndpointConnector conn = EndpointManager.INS.getEndpointConnector(getEndpointEntity());
						//report从任意连接回去
						Channel ch = conn.fetch(); // 获取连接，保证必写成功
						ChannelFuture future = ch.writeAndFlush(deliver);
					}
				},delay,TimeUnit.MILLISECONDS);
			
    	}else if (msg instanceof CmppQueryRequestMessage) {
			CmppQueryRequestMessage e = (CmppQueryRequestMessage) msg;
			CmppQueryResponseMessage res = new CmppQueryResponseMessage(e.getHeader().getSequenceId());
			ctx.channel().writeAndFlush(res);
		}
    	
    	ctx.fireChannelRead(msg);
    }
    
	@Override
	public String name() {
		return "CMPPResponseSenderHandler";
	}

}
