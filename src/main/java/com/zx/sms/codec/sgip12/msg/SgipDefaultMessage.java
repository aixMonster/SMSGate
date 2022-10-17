package com.zx.sms.codec.sgip12.msg;

import org.marre.sms.SgipSmsDcs;
import org.marre.sms.SmsAlphabet;
import org.marre.sms.SmsMessage;
import org.marre.sms.SmsMsgClass;
import org.marre.sms.SmsTextMessage;

import com.zx.sms.codec.cmpp.msg.DefaultMessage;
import com.zx.sms.codec.cmpp.msg.Header;
import com.zx.sms.codec.cmpp.packet.PacketType;
import com.zx.sms.common.util.SequenceNumber;

public abstract class SgipDefaultMessage extends DefaultMessage {

	public SgipDefaultMessage(PacketType packetType, Header header) {
		super(packetType,header);
	}
	public SgipDefaultMessage(PacketType packetType) {
		super(packetType);
	}
	public SequenceNumber getSequenceNumber() {
		return new SequenceNumber(getTimestamp(),getHeader().getNodeId(),getSequenceNo()) ;
	}
	
	protected SmsMessage buildSmsMessage(String text) {
		if (SmsTextMessage.haswidthChar(text))
			return  new SmsTextMessage(text, SgipSmsDcs.getGeneralDataCodingDcs(SmsAlphabet.UCS2, SmsMsgClass.CLASS_UNKNOWN));
		else 
			return  new SmsTextMessage(text, SgipSmsDcs.getGeneralDataCodingDcs(SmsAlphabet.ASCII, SmsMsgClass.CLASS_UNKNOWN));
	}
}
