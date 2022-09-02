package com.zx.sms.codec.cmpp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.marre.sms.SmsMessage;
import org.marre.wap.push.SmsMmsNotificationMessage;
import org.marre.wap.push.SmsWapPushMessage;
import org.marre.wap.push.WapSIPush;
import org.marre.wap.push.WapSLPush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.BaseMessage;
import com.zx.sms.LongSMSMessage;
import com.zx.sms.codec.AbstractTestMessageCodec;
import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppReportRequestMessage;
import com.zx.sms.codec.cmpp.msg.DefaultHeader;
import com.zx.sms.codec.cmpp.msg.Header;
import com.zx.sms.codec.cmpp.wap.LongMessageFrame;
import com.zx.sms.codec.cmpp.wap.LongMessageFrameHolder;
import com.zx.sms.codec.cmpp.wap.SmsMessageHolder;
import com.zx.sms.common.util.MsgId;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TestCmppDeliverRequestMessageCodec extends AbstractTestMessageCodec<CmppDeliverRequestMessage>{
	private static final Logger logger = LoggerFactory.getLogger(TestCmppDeliverRequestMessageCodec.class);

	@Test
	public void testperformance() {
		int i = 0;
		int l = 100000;
		CmppDeliverRequestMessage msg = createTestReq("a尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085d3 中");
		//先预热
		for(;i< 1000;i++){
			decode(encode(msg));
		}
		//开始计时
		
		long start = System.currentTimeMillis();
		
		for(i =0 ;i< l;i++){
			decode(encode(msg));
		}
		long end = System.currentTimeMillis();
		System.out.print((end - start)*1000000/l);
		System.out.println("ns");
//		assert(msg.getMsgContent().equals(result.getMsgContent()));
	}

	@Test
	public void testCodec() {

		CmppDeliverRequestMessage msg = createTestReq("ad3 中");

		test0(msg);
	}

	@Test
	public void testReportCodec() {
		CmppDeliverRequestMessage msg = createTestReq("k k k ");
		msg.setMsgContent((SmsMessage)null);
		CmppReportRequestMessage reportRequestMessage = new CmppReportRequestMessage();
		reportRequestMessage.setSmscSequence(0x1234L);
		reportRequestMessage.setMsgId(new MsgId());
		reportRequestMessage.setDestterminalId("13800138000");
		reportRequestMessage.setStat("9876");
		msg.setReportRequestMessage(reportRequestMessage);
		test0(msg);
	}

	private void test0(CmppDeliverRequestMessage msg) {

		
		ByteBuf buf = encode(msg);
		ByteBuf newbuf = buf.copy();

		int length = buf.readableBytes();
		
		Assert.assertEquals(length, buf.readInt());
		Assert.assertEquals(msg.getPacketType().getCommandId(), buf.readInt());
		Assert.assertEquals(msg.getHeader().getSequenceId(), buf.readInt());

		buf.release();
		CmppDeliverRequestMessage result = decode(newbuf);

		Assert.assertEquals(msg.getHeader().getSequenceId(), result.getHeader().getSequenceId());
		if (msg.isReport()) {
			Assert.assertEquals(msg.getReportRequestMessage().getSmscSequence(), result.getReportRequestMessage().getSmscSequence());
		} else {
			Assert.assertEquals(msg.getMsgContent(), result.getMsgContent());
		}
		Assert.assertEquals(msg.getSrcterminalId(), result.getSrcterminalId());

	}

	private CmppDeliverRequestMessage createTestReq(String content) {

		Header header = new DefaultHeader();
		// 取时间，用来查看编码解码时间

		CmppDeliverRequestMessage msg = new CmppDeliverRequestMessage(header);
		msg.setDestId("13800138000");
		msg.setLinkid("0000");
		// 70个汉字
		msg.setMsgContent(content);
		msg.setMsgId(new MsgId());
		msg.setServiceid("10086");
		msg.setSrcterminalId("13800138000");
		msg.setSrcterminalType((short) 1);
		header.setSequenceId((int)System.nanoTime());
		return msg;
	}
	
	@Test
	public void testchinesecode()
	{
		CmppDeliverRequestMessage msg = createTestReq("1234567890123456789中01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" );
		testlongCodec(msg);
	}

	@Test
	public void testASCIIcode()
	{
		CmppDeliverRequestMessage msg = createTestReq("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
		testlongCodec(msg);
	}
	
	@Test
	public void testSLPUSH()
	{
		CmppDeliverRequestMessage msg = createTestReq("");
		WapSLPush sl = new WapSLPush("http://www.baidu.com");
		SmsMessage wap = new SmsWapPushMessage(sl);
		msg.setMsgContent(wap);
		CmppDeliverRequestMessage result = testWapCodec(msg);
		SmsWapPushMessage smsmsg = (SmsWapPushMessage)result.getSmsMessage();
		WapSLPush actsl = (WapSLPush)smsmsg.getWbxml();
		Assert.assertEquals(sl.getUri(), actsl.getUri());
	}
	
	@Test
	public void testSIPUSH()
	{
		CmppDeliverRequestMessage msg = createTestReq("");
		WapSIPush si = new WapSIPush("http://www.baidu.com","baidu");
		SmsMessage wap = new SmsWapPushMessage(si);
		msg.setMsgContent(wap);
		CmppDeliverRequestMessage result = testWapCodec(msg);
		SmsWapPushMessage smsmsg = (SmsWapPushMessage)result.getSmsMessage();
		WapSIPush actsi = (WapSIPush)smsmsg.getWbxml();
		Assert.assertEquals(si.getUri(), actsi.getUri());
		Assert.assertEquals(si.getMessage(), actsi.getMessage());
	}
	
	@Test
	public void testMMSPUSH()
	{
		CmppDeliverRequestMessage msg = createTestReq("");
		SmsMmsNotificationMessage mms = new SmsMmsNotificationMessage("https://www.baidu.com/s?wd=SMPPv3.4%20%E9%95%BF%E7%9F%AD%E4%BF%A1&rsv_spt=1&rsv_iqid=0xdd4666100001e74c&issp=1&f=8&rsv_bp=1&rsv_idx=2&ie=utf-8&rqlang=cn&tn=baiduhome_pg&rsv_enter=0&oq=SMPPv%2526lt%253B.4%2520ton%2520npi&rsv_t=50fdNrphqry%2FYfHh29wvp8KzJ9ogqigiPr33FT%2FpcGQu6X34vByQNu4O%2FLNZgIiXdd16&inputT=3203&rsv_pq=d576ead9000016eb&rsv_sug3=60&rsv_sug1=15&rsv_sug7=000&rsv_sug2=0&rsv_sug4=3937&rsv_sug=1",50*1024);
		msg.setMsgContent(mms);
		mms.setTransactionId("ABC");
		CmppDeliverRequestMessage result =testWapCodec(msg);
		SmsMmsNotificationMessage smsmsg = (SmsMmsNotificationMessage)result.getSmsMessage();
		Assert.assertEquals(smsmsg.getContentLocation_(), smsmsg.getContentLocation_());
	}
	
	public CmppDeliverRequestMessage testWapCodec(CmppDeliverRequestMessage msg)
	{
		channel().writeOutbound(msg);
		ByteBuf buf =(ByteBuf)channel().readOutbound();
		ByteBuf copybuf = Unpooled.buffer();
	    while(buf!=null){
			
			
	    	copybuf.writeBytes(buf.copy());
			int length = buf.readableBytes();
			
			Assert.assertEquals(length, buf.readInt());
			Assert.assertEquals(msg.getPacketType().getCommandId(), buf.readInt());
			

			buf =(ByteBuf)channel().readOutbound();
	    }
	    
	    CmppDeliverRequestMessage result = decode(copybuf);
		System.out.println(result);
		Assert.assertTrue(result.getSmsMessage() instanceof SmsMessage);
		return result;
	}
	
	public void testlongCodec(CmppDeliverRequestMessage msg)
	{
		
		channel().writeOutbound(msg);
		ByteBuf buf =(ByteBuf)channel().readOutbound();
		ByteBuf copybuf = Unpooled.buffer();
	    while(buf!=null){
			
			
	    	copybuf.writeBytes(buf.copy());
			int length = buf.readableBytes();
			
			Assert.assertEquals(length, buf.readInt());
			Assert.assertEquals(msg.getPacketType().getCommandId(), buf.readInt());
			

			buf =(ByteBuf)channel().readOutbound();
	    }
	    
	    CmppDeliverRequestMessage result = decode(copybuf);
		
		System.out.println(result.getMsgContent());
		Assert.assertEquals(msg.getMsgContent(), result.getMsgContent());
	}
	
	//多线程长短信合并
	@Test
	public void testConcurrentLongMessageMerge() throws Exception {
		//生成一个超长的短信
	     String tmp = "123456尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜123456尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动100857890123456789012345678901234尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E7尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动1008566D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085动100855678901234567890123456789012345123456尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动100857890123456789012345678901234尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E7尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动1008566D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085动1008556789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789067890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动100857890123456789012345678901234尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E7尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动1008566D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085动100855678901234567890123456789012345123456尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动100857890123456789012345678901234尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E7尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动1008566D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动10085动1008556789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789067890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
//		String tmp = "123456尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜123456尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动10085销售专线订购的【一加手机高清防刮保护膜】，请点击支付http://www.10085.cn/web85/page/zyzxpay/wap_order.html?orderId=76DEF9AE1808F506FD4E6CB782E3B8E7EE875E766D3D335C 完成下单。请在60分钟内完成支付，如有疑问，请致电10085咨询，谢谢！中国移动100857890123456789012345678901234尊敬的客户,您好！您于2016-03-23 14:51:36通过中国移动中不";
		final String longlongMsg = tmp + tmp;
	    //测试10次
		long start = System.nanoTime();
		ExecutorService executor = Executors.newFixedThreadPool(100);
	    for(int i = 0;i<1000;i++) {
		    CmppDeliverRequestMessage lmsg = createTestReq(longlongMsg);
			// 长短信拆分
			SmsMessage msgcontent = lmsg.getSmsMessage();
			List<LongMessageFrame> frameList = LongMessageFrameHolder.INS.splitmsgcontent(msgcontent);
			
			//保证同一条长短信，通过同一个tcp连接发送
			List<BaseMessage> msgs = new ArrayList<BaseMessage>();
			for (LongMessageFrame frame : frameList) {
				BaseMessage basemsg = (BaseMessage) lmsg.generateMessage(frame);
				msgs.add(basemsg);
			}
			//启用多线程进行短信合并
			final String key = lmsg.getSrcterminalId()+lmsg.getDestId()+RandomUtils.nextLong();
		
			int totalSplitMsg = msgs.size();
			
			//随机打乱
			Collections.shuffle(msgs);
			
			final CountDownLatch startSignal = new CountDownLatch(totalSplitMsg);
			final CountDownLatch stop = new CountDownLatch(totalSplitMsg);
			final AtomicInteger count = new AtomicInteger();
			for(final BaseMessage split : msgs) {
				executor.submit(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						//等待开始信号
						startSignal.await();
						
						SmsMessageHolder hoder = LongMessageFrameHolder.INS.putAndget(key, ((LongSMSMessage)split),false);
						if(hoder != null) {
							//只有一个线程能到达这里
							CmppDeliverRequestMessage result = (CmppDeliverRequestMessage)hoder.getMsg();
							result.setMsgContent(hoder.getSmsMessage());
							
							//完成计数器加1
							count.incrementAndGet();
							stop.countDown();
							
							
							Assert.assertEquals(longlongMsg,result.getMsgContent());
							return true;
						}
						stop.countDown();
						return false;
					}
				});
				startSignal.countDown();
			}
			stop.await(); //等待结束
			Assert.assertEquals("合并成功的线程数：",1,count.get());
		}
		long end = System.nanoTime();
		System.out.println("合并耗时 : " + (end - start)/1000000);
	}
}
