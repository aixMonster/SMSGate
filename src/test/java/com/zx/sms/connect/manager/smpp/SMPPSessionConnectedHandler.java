package com.zx.sms.connect.manager.smpp;

import java.util.concurrent.atomic.AtomicInteger;

import com.chinamobile.cmos.sms.SmsAlphabet;
import com.zx.sms.BaseMessage;
import com.zx.sms.codec.smpp.Address;
import com.zx.sms.codec.smpp.msg.DeliverSm;
import com.zx.sms.codec.smpp.msg.DeliverSmReceipt;
import com.zx.sms.codec.smpp.msg.DeliverSmResp;
import com.zx.sms.codec.smpp.msg.SubmitSm;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.ServerEndpoint;
import com.zx.sms.handler.api.gate.SessionConnectedHandler;

import io.netty.channel.ChannelHandlerContext;

public class SMPPSessionConnectedHandler extends SessionConnectedHandler {
	public SMPPSessionConnectedHandler(int t) {
		totleCnt = new AtomicInteger(t);
	}

	@Override
	protected BaseMessage createTestReq(String str) {
		final EndpointEntity finalentity = getEndpointEntity();
//		String content = "£$¥èéùì@";
		if (finalentity instanceof ServerEndpoint) {
			DeliverSm pdu = new DeliverSm();
	        pdu.setSourceAddress(new Address((byte)0,(byte)0,"13800138000"));
	        pdu.setDestAddress(new Address((byte)0,(byte)0,"10086"));
//	        pdu.setSmsMsg(new SmsTextMessage(content,SmsDcs.getGeneralDataCodingDcs(SmsAlphabet.GSM,SmsMsgClass.CLASS_UNKNOWN)));
			pdu.setSmsMsg(str);
	        return pdu;
		} else {
			SubmitSm pdu = new SubmitSm();
			pdu.setRegisteredDelivery((byte)0);
	        pdu.setSourceAddress(new Address((byte)0,(byte)0,"10086"));
	        pdu.setDestAddress(new Address((byte)0,(byte)0,"13800138000"));
//	        pdu.setSmsMsg(new SmsTextMessage(content,SmsDcs.getGeneralDataCodingDcs(SmsAlphabet.GSM,SmsMsgClass.CLASS_UNKNOWN)));
	        pdu.setSmsMsg(str+"这个链接的访问记录，跟普通链ssage(content,SmsDcs.getGeneralDataCodingDcs(SmsAlphabet.GSM,SmsMsgClass.CLASS_U接的访问记录都一样，都发到大数据平台了吧？",SmsAlphabet.ASCII); //设置默认字符为ACSII
	        return pdu;
		}
	}
	
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
		super.channelRead(ctx, msg);
		if (msg instanceof DeliverSmReceipt) {
			DeliverSmReceipt e = (DeliverSmReceipt) msg;
			
			DeliverSmResp res =e.createResponse();
			res.setMessageId(String.valueOf(System.currentTimeMillis()));
			 ctx.writeAndFlush(res);

		} else {
			ctx.fireChannelRead(msg);
		}
	}


}
