package com.zx.sms.handler.smpp;

import com.chinamobile.cmos.sms.SmsMessage;
import com.zx.sms.LongSMSMessage;
import com.zx.sms.codec.cmpp.wap.AbstractLongMessageHandler;
import com.zx.sms.codec.cmpp.wap.LongMessageFrame;
import com.zx.sms.codec.smpp.msg.BaseSm;
import com.zx.sms.connect.manager.EndpointEntity;

public class SMPPLongMessageHandler extends AbstractLongMessageHandler<BaseSm> {
	
	
	public SMPPLongMessageHandler(EndpointEntity entity) {
		super(entity);
	}

	@Override
	protected void resetMessageContent(BaseSm t, SmsMessage content) {
		t.setSmsMsg(content);
	}
	
	//TODO 暂时没实现payLoad保存消息，
//	requestMessage.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_MSG_REF_NUM,ByteArrayUtil.toByteArray(frame.getPkseq())));
//	requestMessage.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_TOTAL_SEGMENTS,ByteArrayUtil.toByteArray(frame.getPktotal())));
//	requestMessage.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_SEGMENT_SEQNUM,ByteArrayUtil.toByteArray(frame.getPknumber())));
	protected LongSMSMessage generateMessage(BaseSm lmsg ,LongMessageFrame frame ,EndpointEntity entity) throws Exception{
		return (LongSMSMessage)((LongSMSMessage) lmsg).generateMessage(frame);
	}
	
}