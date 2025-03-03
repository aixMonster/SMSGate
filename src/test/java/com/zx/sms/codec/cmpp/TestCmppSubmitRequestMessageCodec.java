package com.zx.sms.codec.cmpp;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.chinamobile.cmos.sms.SmsDcs;
import com.chinamobile.cmos.sms.SmsMessage;
import com.chinamobile.cmos.sms.SmsPort;
import com.chinamobile.cmos.sms.SmsPortAddressedTextMessage;
import com.chinamobile.cmos.sms.SmsTextMessage;
import com.chinamobile.cmos.wap.push.SmsMmsNotificationMessage;
import com.chinamobile.cmos.wap.push.SmsWapPushMessage;
import com.chinamobile.cmos.wap.push.WapSIPush;
import com.chinamobile.cmos.wap.push.WapSLPush;
import com.zx.sms.codec.AbstractTestMessageCodec;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.common.util.MsgId;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TestCmppSubmitRequestMessageCodec  extends AbstractTestMessageCodec<CmppSubmitRequestMessage>{


	@Test
	public void testCodec()
	{
		CmppSubmitRequestMessage msg = new CmppSubmitRequestMessage();
		msg.setDestterminalId(new String[]{"13800138000"});
		msg.setLinkID("0000");
		msg.setMsgContent("123");
		msg.setMsgid(new MsgId());
		msg.setServiceId("10086");
		msg.setSrcId("10086");
		ByteBuf buf = encode(msg);
		ByteBuf copybuf = buf.copy();
		
		int length = buf.readableBytes();
		
		Assert.assertEquals(length, buf.readInt());
		Assert.assertEquals(msg.getPacketType().getCommandId(), buf.readInt());
		Assert.assertEquals(msg.getHeader().getSequenceId(), buf.readInt());
		
	
		
		CmppSubmitRequestMessage result = decode(copybuf);
		
		Assert.assertEquals(msg.getHeader().getSequenceId(), result.getHeader().getSequenceId());

		Assert.assertEquals(msg.getMsgContent(), result.getMsgContent());
		Assert.assertEquals(msg.getServiceId(), result.getServiceId());
	}

	@Test
	public void testchinesecode()
	{
		
		testlongCodec(createTestReq("尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085"));
	}

	@Test
	public void testASCIIcode()
	{
		testlongCodec(createTestReq("12345678901AssBC56789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890abcdefghijklmnopqrstuvwxyzABCE"));
	}
	
	
	
	@Test
	public void testSLPUSH()
	{
		CmppSubmitRequestMessage msg = createTestReq("");
		WapSLPush sl = new WapSLPush("http://www.baidu.com");
		SmsMessage wap = new SmsWapPushMessage(sl);
		msg.setMsgContent(wap);
		CmppSubmitRequestMessage result = testWapCodec(msg);
		SmsWapPushMessage smsmsg = (SmsWapPushMessage)result.getSmsMessage();
		WapSLPush actsl = (WapSLPush)smsmsg.getWbxml();
		Assert.assertEquals(sl.getUri(), actsl.getUri());
	}
	
	@Test
	public void testSIPUSH()
	{
		CmppSubmitRequestMessage msg = createTestReq("");
		WapSIPush si = new WapSIPush("http://www.baidu.com","baidu");
		SmsMessage wap = new SmsWapPushMessage(si);
		msg.setMsgContent(wap);
		CmppSubmitRequestMessage result = testWapCodec(msg);
		SmsWapPushMessage smsmsg = (SmsWapPushMessage)result.getSmsMessage();
		WapSIPush actsi = (WapSIPush)smsmsg.getWbxml();
		Assert.assertEquals(si.getUri(), actsi.getUri());
		Assert.assertEquals(si.getMessage(), actsi.getMessage());
	}
	@Test
	public void testPortTextSMSH()
	{
		Random rnd_ = new Random();
		CmppSubmitRequestMessage msg = createTestReq("");
		SmsPortAddressedTextMessage textMsg =new SmsPortAddressedTextMessage(new SmsPort(rnd_.nextInt() &0xffff,"")  ,new SmsPort(rnd_.nextInt()&0xffff,""),"这是一条端口文本短信");
		msg.setMsgContent(textMsg);
		CmppSubmitRequestMessage result =testWapCodec(msg);
		SmsPortAddressedTextMessage smsmsg = (SmsPortAddressedTextMessage)result.getSmsMessage();
		Assert.assertEquals(textMsg.getDestPort_(), smsmsg.getDestPort_());
		Assert.assertEquals(textMsg.getOrigPort_(), smsmsg.getOrigPort_());
		Assert.assertEquals(textMsg.getText(), smsmsg.getText());
	}
	@Test
	public void testMMSPUSH()
	{
		CmppSubmitRequestMessage msg = createTestReq("");
		SmsMmsNotificationMessage mms = new SmsMmsNotificationMessage("https://www.baidu.com/s?wd=SMPPv3.4%20%E9%95%BF%E7%9F%AD%E4%BF%A1&rsv_spt=1&rsv_iqid=0xdd4666100001e74c&issp=1&f=8&rsv_bp=1&rsv_idx=2&ie=utf-8&rqlang=cn&tn=baiduhome_pg&rsv_enter=0&oq=SMPPv%2526lt%253B.4%2520ton%2520npi&rsv_t=50fdNrphqry%2FYfHh29wvp8KzJ9ogqigiPr33FT%2FpcGQu6X34vByQNu4O%2FLNZgIiXdd16&inputT=3203&rsv_pq=d576ead9000016eb&rsv_sug3=60&rsv_sug1=15&rsv_sug7=000&rsv_sug2=0&rsv_sug4=3937&rsv_sug=1",50*1024);
		mms.setFrom("10085");
		mms.setSubject("这是一条测试彩信，彩信消息ID是：121241");
		msg.setMsgContent(mms);
		CmppSubmitRequestMessage result =testWapCodec(msg);
		SmsMmsNotificationMessage smsmsg = (SmsMmsNotificationMessage)result.getSmsMessage();
		Assert.assertEquals(mms.getSubject_(), smsmsg.getSubject_());
		Assert.assertEquals(mms.getContentLocation_(), smsmsg.getContentLocation_());
		Assert.assertEquals(mms.getFrom_(), smsmsg.getFrom_());
	}
	
	@Test
	public void testseptedMsg(){

		String origin = "112aaaasssss2334455@£$¥èéùìòçØøÅåΔ_ΦΓΛΩΠΨΣΘΞ^{}\\[~]|€ÆæßÉ!\"#¤%&'()*+,-./0123456789:;<=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà";
		CmppSubmitRequestMessage msg = createTestReq(origin);
		msg.setMsgContent(new SmsTextMessage(origin));
		
		CmppSubmitRequestMessage ret =  testWapCodec(msg);
		Assert.assertEquals(msg.getMsgContent(), ret.getMsgContent());
	}
	
	
	@Test
	public void testGBKMsg(){

		CmppSubmitRequestMessage msg = createTestReq("");
		msg.setMsgContent(new SmsTextMessage("有没有发现，使用模型的表达要清晰易懂很多，而且也不需要做关于组合品的判断了，因为我们在系统中引入了更加贴近现实的对象模型（CombineBackO123456",new SmsDcs((byte)0x0f)));
		
		CmppSubmitRequestMessage ret =  testWapCodec(msg);
		Assert.assertEquals(msg.getMsgContent(), ret.getMsgContent());
	}
	
	private CmppSubmitRequestMessage createTestReq(String content) {

		// 取时间，用来查看编码解码时间
		CmppSubmitRequestMessage msg = new CmppSubmitRequestMessage();
		msg.setDestterminalId(new String[]{"13800138000"});
		msg.setLinkID("0000");
		msg.setMsgContent(content);
		msg.setMsgid(new MsgId());
		msg.setServiceId("10086");
		msg.setSrcId("10086");
		return msg;
	}
	public CmppSubmitRequestMessage  testWapCodec(CmppSubmitRequestMessage msg)
	{

		channel().writeOutbound(msg);
		ByteBuf buf =(ByteBuf)channel().readOutbound();
		ByteBuf copybuf = Unpooled.buffer();
	    while(buf!=null){
			
	    	ByteBuf copy = buf.copy();
	    	copybuf.writeBytes(copy);
	    	copy.release();
			int length = buf.readableBytes();
			
			Assert.assertEquals(length, buf.readInt());
			Assert.assertEquals(msg.getPacketType().getCommandId(), buf.readInt());
			

			buf =(ByteBuf)channel().readOutbound();
	    }
	    
		CmppSubmitRequestMessage result = decode(copybuf);
		System.out.println(result);
		Assert.assertEquals(msg.getServiceId(), result.getServiceId());
		Assert.assertTrue(result.getSmsMessage() instanceof SmsMessage);
		return result;
	}
	
	public void testlongCodec(CmppSubmitRequestMessage msg)
	{

		channel().writeOutbound(msg);
		ByteBuf buf =(ByteBuf)channel().readOutbound();
		ByteBuf copybuf = Unpooled.buffer();
	    while(buf!=null){
			
			
	    	ByteBuf copy = buf.copy();
	    	copybuf.writeBytes(copy);
	    	copy.release();
			int length = buf.readableBytes();
			
			Assert.assertEquals(length, buf.readInt());
			Assert.assertEquals(msg.getPacketType().getCommandId(), buf.readInt());
			

			buf =(ByteBuf)channel().readOutbound();
	    }
	    
		CmppSubmitRequestMessage result = decode(copybuf);
		
		System.out.println(result.getMsgContent());
		Assert.assertNotNull(result.getUniqueLongMsgId().getId());
		System.out.println(result.getUniqueLongMsgId());
		
		if(result.getFragments()!=null) {
			for(CmppSubmitRequestMessage m : result.getFragments())
				System.out.println(m.getUniqueLongMsgId());
		}
		
		Assert.assertEquals(msg.getServiceId(), result.getServiceId());
		Assert.assertEquals(msg.getMsgContent(), result.getMsgContent());
	}
}
