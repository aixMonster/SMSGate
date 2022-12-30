package com.zx.sms.concat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.chinamobile.cmos.sms.SmsConcatMessage;
import com.chinamobile.cmos.sms.SmsException;
import com.chinamobile.cmos.sms.SmsMessage;
import com.chinamobile.cmos.sms.SmsTextMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.wap.LongMessageFrame;
import com.zx.sms.codec.cmpp.wap.LongMessageFrameHolder;
public class TestSmsConcatMessage {

	
	@Test
	public void test1() throws SmsException {
		//1分钟内，同一个号码小于255条长短信，生成的长短信seqreqNo不能重复
		String text = "尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085";
		String[] phones = new String[] {"13805138000","13805138001","13805138002"};
		Map<String ,Integer> checkMap = new HashMap();
		boolean checkExists = false;
		List<CmppSubmitRequestMessage> smsS = new ArrayList<CmppSubmitRequestMessage>();
		for(int i = 0;i<256*phones.length;i++) {
			SmsTextMessage sms = new SmsTextMessage(text);
			String phone = phones[i%phones.length];
			CmppSubmitRequestMessage submitMsg = CmppSubmitRequestMessage.create(phone, "10086", text);
			smsS.add(submitMsg);
		}
		
		Collections.shuffle(smsS);
		
		for(CmppSubmitRequestMessage submitMsg:smsS) {
			//如果是长短信类型，记录序列号key
			String phone = submitMsg.getDestterminalId()[0];
			SmsMessage smsMessage = submitMsg.getSmsMessage();
			
			if(smsMessage instanceof SmsConcatMessage) {
				((SmsConcatMessage)smsMessage).setSeqNoKey(submitMsg.getSrcIdAndDestId());
			}
			
			List<LongMessageFrame> frameList = LongMessageFrameHolder.INS.splitmsgcontent(smsMessage);
			Integer pkseq = Integer.valueOf(frameList.get(0).getPkseq());
			Integer oldpkseq = checkMap.putIfAbsent(phone+pkseq,pkseq);
			if(oldpkseq!=null) {
				checkExists = true;
				System.out.println(phone+"=="+pkseq+"==="+oldpkseq);
				break;
			}
		}
		Assert.assertTrue(!checkExists);
	}
}
