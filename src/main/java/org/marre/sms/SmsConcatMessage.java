/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is "SMS Library for the Java platform".
 *
 * The Initial Developer of the Original Code is Markus Eriksson.
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.marre.sms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zx.sms.config.PropertiesUtils;

/**
 * Baseclass for messages that needs to be concatenated.
 * <p>
 * - Only usable for messages that uses the same UDH fields for all message
 * parts. <br>
 * - This class could be better written. There are several parts that are copy-
 * pasted. <br>
 * - The septet coding could be a bit optimized. <br>
 * 
 * @author Markus Eriksson
 * @version $Id$
 */
public abstract class SmsConcatMessage implements SmsMessage {
	private static final Logger logger = LoggerFactory.getLogger(SmsConcatMessage.class);
	private static final AtomicInteger rnd_ = new AtomicInteger((new Random()).nextInt(0xffff));
	private static final Boolean Use8bit = Boolean.valueOf(PropertiesUtils.getproperties("smsUse8bit", "true"));
	// 长短信拆分，要使用 rnd_ 生成统一的长短信ID,但在针对同一个号码，下发多条长短信，并且高并发情况下，随机生成的ID,有机率带来同一个号码
	// ID重复，冲突，造成短信无法展示
	private String seqNoKey;
	private static final Integer refNoCacheTimeOut = Integer
			.valueOf(PropertiesUtils.getproperties("refNoCacheTimeOut", "60"));
	private static final LoadingCache<String, AtomicInteger> refNoCache = CacheBuilder.newBuilder()
			.expireAfterAccess(refNoCacheTimeOut, TimeUnit.SECONDS).build(new CacheLoader<String, AtomicInteger>() {
				@Override
				public AtomicInteger load(String telephone) throws Exception {
					return new AtomicInteger(rnd_.incrementAndGet());
				}
			});

	/**
	 * Creates an empty SmsConcatMessage.
	 */
	protected SmsConcatMessage() {
		// Empty
	}

	/**
	 * Returns the whole UD
	 * 
	 * @return the UD
	 */
	public abstract SmsUserData getUserData();

	/**
	 * Returns the udh elements
	 * <p>
	 * The returned UDH is the same as specified when the message was created. No
	 * concat headers are added.
	 * 
	 * @return the UDH as SmsUdhElements
	 */
	public abstract SmsUdhElement[] getUdhElements();

	// 考虑在短时间内，针对相同一个号码生成不同的ID
	private int nextRandom() {
		if (StringUtils.isEmpty(seqNoKey))
			return rnd_.incrementAndGet() & 0xffff;
		else {
			AtomicInteger tt = refNoCache.getUnchecked(seqNoKey);
			return tt.incrementAndGet() & 0xffff;
		}
	}

	private SmsPdu[] createOctalPdus(SmsUdhElement[] udhElements, SmsUserData ud, int maxBytes) {
		int nMaxChars;
		int nMaxConcatChars;
		SmsPdu[] smsPdus = null;

		// 8-bit concat header is 6 bytes...
		nMaxConcatChars = maxBytes - 6;
		nMaxChars = maxBytes;

		if (ud.getLength() <= nMaxChars) {
			smsPdus = new SmsPdu[] { new SmsPdu(udhElements, ud) };
		} else {
			int refno = nextRandom();

			// Calculate number of SMS needed
			int nSms = ud.getLength() / nMaxConcatChars;
			if ((ud.getLength() % nMaxConcatChars) > 0) {
				nSms += 1;
			}
			if (nSms > 255) {

				logger.error("error SmsConcatMessage pkTotal Number {} .should be less than 256", nSms);
			}
			smsPdus = new SmsPdu[nSms];

			// Calculate number of UDHI
			SmsUdhElement[] pduUdhElements = null;
			if (udhElements == null) {
				pduUdhElements = new SmsUdhElement[1];
			} else {
				pduUdhElements = new SmsUdhElement[udhElements.length + 1];

				// Copy the UDH headers
				System.arraycopy(udhElements, 0, pduUdhElements, 1, udhElements.length);
			}

			// Create pdus
			for (int i = 0; i < nSms; i++) {
				byte[] pduUd;
				int udBytes;
				int udLength;
				int udOffset;

				// Create concat header
				pduUdhElements[0] = SmsUdhUtil.get8BitConcatUdh(refno, nSms, i + 1);

				// Create
				// Must concatenate messages
				// Calc pdu length
				udOffset = nMaxConcatChars * i;
				udBytes = ud.getLength() - udOffset;
				if (udBytes > nMaxConcatChars) {
					udBytes = nMaxConcatChars;
				}
				udLength = udBytes;

				pduUd = new byte[udBytes];
				System.arraycopy(ud.getData(), udOffset, pduUd, 0, udBytes);
				smsPdus[i] = new SmsPdu(pduUdhElements, pduUd, udLength, ud.getDcs());
			}
		}
		return smsPdus;
	}

	private SmsPdu[] createUnicodePdus(SmsUdhElement[] udhElements, SmsUserData ud, int maxBytes) {
		int nMaxConcatChars;
		SmsPdu[] smsPdus = null;

		// 8-bit concat header is 6 bytes...
		nMaxConcatChars = (maxBytes - 6) / 2;

		if (ud.getLength() <= maxBytes) {
			smsPdus = new SmsPdu[] { new SmsPdu(udhElements, ud) };
		} else {
			int refno = nextRandom();

			// Calculate number of SMS needed
			int nSms = (ud.getLength() / 2) / nMaxConcatChars;
			if (((ud.getLength() / 2) % nMaxConcatChars) > 0) {
				nSms += 1;
			}
			if (nSms > 255) {
				logger.error("error SmsConcatMessage pkTotal Number {} .should be less than 256", nSms);
			}

			smsPdus = new SmsPdu[nSms];

			// Calculate number of UDHI
			SmsUdhElement[] pduUdhElements = null;
			if (udhElements == null) {
				pduUdhElements = new SmsUdhElement[1];
			} else {
				pduUdhElements = new SmsUdhElement[udhElements.length + 1];

				// Copy the UDH headers
				System.arraycopy(udhElements, 0, pduUdhElements, 1, udhElements.length);
			}

			// Create pdus
			for (int i = 0; i < nSms; i++) {
				byte[] pduUd;
				int udBytes;
				int udLength;
				int udOffset;

				// Create concat header
				pduUdhElements[0] = SmsUdhUtil.get8BitConcatUdh(refno, nSms, i + 1);

				// Create
				// Must concatenate messages
				// Calc pdu length
				udOffset = nMaxConcatChars * i;
				udLength = (ud.getLength() / 2) - udOffset;
				if (udLength > nMaxConcatChars) {
					udLength = nMaxConcatChars;
				}
				udBytes = udLength * 2;

				pduUd = new byte[udBytes];
				System.arraycopy(ud.getData(), udOffset * 2, pduUd, 0, udBytes);
				smsPdus[i] = new SmsPdu(pduUdhElements, pduUd, udBytes, ud.getDcs());
			}
		}
		return smsPdus;
	}

	private SmsPdu[] createPdus(SmsUdhElement[] udhElements, SmsUserData ud, int maxBytes) {
		SmsPdu[] smsPdus = null;
		if (ud.getLength() <= maxBytes) {
			smsPdus = new SmsPdu[] { new SmsPdu(udhElements, ud) };
		} else {
			// 使用8bit拆分兼容性好 ，16bit可能很多厂商不支持
			List<byte[]> slice = sliceUd(ud, maxBytes, Use8bit);

			int maxSlicLength = slice.size();
			if (maxSlicLength > 255) {
				logger.error("error SmsConcatMessage pkTotal Number {} .should be less than 255", maxSlicLength);
				maxSlicLength = 255;
			}
			smsPdus = new SmsPdu[maxSlicLength];
			// Calculate number of UDHI
			SmsUdhElement[] pduUdhElements = null;
			if (udhElements == null) {
				pduUdhElements = new SmsUdhElement[1];
			} else {
				pduUdhElements = new SmsUdhElement[udhElements.length + 1];
				// Copy the UDH headers
				System.arraycopy(udhElements, 0, pduUdhElements, 1, udhElements.length);
			}

			int refno = nextRandom();
			for (int i = 0; i < smsPdus.length; i++) {
				byte[] t = slice.get(i);
				// Create concat header
				pduUdhElements[0] = Use8bit ?SmsUdhUtil.get8BitConcatUdh(refno, smsPdus.length, i + 1): SmsUdhUtil.get16BitConcatUdh(refno, smsPdus.length, i + 1);
				smsPdus[i] = new SmsPdu(pduUdhElements, t, t.length, ud.getDcs());
			}
		}
		return smsPdus;
	}

	private List<byte[]> sliceUd(SmsUserData ud, int maxBytes, boolean use8bit) {
		byte[] udbyte = ud.getData();
		int udLength = udbyte.length;
		int nMaxUdLength = use8bit ? maxBytes - 6 : maxBytes - 7;
		int udOffset = 0;
		List<byte[]> slice = new ArrayList<byte[]>();
		while (udOffset < udLength) {
			int oneCopyLength = nMaxUdLength;
			if (udOffset + oneCopyLength >= udLength) {
				oneCopyLength = udLength - udOffset; // 这里是最后一个分片了，长度可能小于nMaxUdLength最大长度
			} else {
				// 检查本次分片的最后一个字节是否为双字节字符，避免一个汉字被拆在两半
				if (oneCopyLength % 2 > 0 && SmsAlphabet.UCS2 == ud.getDcs().getAlphabet()) {
					// 如果是UCS2 ，并且maxBytes是奇数
					oneCopyLength--;
				} else if (SmsAlphabet.RESERVED == ud.getDcs().getAlphabet()) {
					// GBK编码要统计英文字符个数，英文是一个字节，汉字两个字节
					int oneByteCharCnt = 0; // 英文字符个数
					for (int k = udOffset; k < udOffset + oneCopyLength; k++) {
						// 最高位是1的是双字节字符
						if ((udbyte[k] & (byte) 0x80) == (byte) 0x80) {
							// 汉字占两个字符
							k++;
						} else {
							// 如果是英文字符算1个byte
							oneByteCharCnt++;
							;
						}
					}
					if ((nMaxUdLength + oneByteCharCnt) % 2 > 0) {
						oneCopyLength--;
					}
				}
			}

			byte[] tmp = new byte[oneCopyLength];
			System.arraycopy(ud.getData(), udOffset, tmp, 0, oneCopyLength);
			udOffset += oneCopyLength;
			slice.add(tmp);
		}
		return slice;
	}

	private SmsPdu[] createSeptetPdus(SmsUdhElement[] udhElements, SmsUserData ud, int maxBytes) {
		int nMaxChars;
		int nMaxConcatChars;
		SmsPdu[] smsPdus = null;

		// 8-bit concat header is 6 bytes...
		nMaxConcatChars = ((maxBytes - 6) * 8) / 7;
		nMaxChars = (maxBytes * 8) / 7;

		if (ud.getLength() <= nMaxChars) {
			smsPdus = new SmsPdu[] { new SmsPdu(udhElements, ud) };
		} else {
			int refno = nextRandom();
			// Convert septets into a string...
			String msg = SmsPduUtil.readSeptets(ud.getData(), ud.getLength());

			if (msg.length() <= ud.getLength()) {
				// 原字符串长度小于udLength ，说明存在GSM的转义字符
				// 计算转义字符个数,拆分后长度要减去转义字符
				int cnt = SmsPduUtil.countGSMescapechar(msg);
				nMaxConcatChars -= 2 * cnt;
			}
			// Calculate number of SMS needed
			int nSms = msg.length() / nMaxConcatChars;
			if ((msg.length() % nMaxConcatChars) > 0) {
				nSms += 1;
			}
			if (nSms > 255) {
				logger.error("error SmsConcatMessage pkTotal Number {} .should be less than 256", nSms);
			}
			smsPdus = new SmsPdu[nSms];

			// Calculate number of UDHI
			SmsUdhElement[] pduUdhElements = null;
			if (udhElements == null) {
				pduUdhElements = new SmsUdhElement[1];
			} else {
				pduUdhElements = new SmsUdhElement[udhElements.length + 1];

				// Copy the UDH headers
				System.arraycopy(udhElements, 0, pduUdhElements, 1, udhElements.length);
			}

			// Create pdus
			for (int i = 0; i < nSms; i++) {
				byte[] pduUd;
				int udOffset;
				int udLength;

				// Create concat header
				pduUdhElements[0] = SmsUdhUtil.get8BitConcatUdh(refno, nSms, i + 1);

				// Create
				// Must concatenate messages
				// Calc pdu length
				udOffset = nMaxConcatChars * i;
				udLength = ud.getLength() - udOffset;
				if (udLength > nMaxConcatChars) {
					udLength = nMaxConcatChars;
				}

				if (udOffset + udLength > msg.length()) {
					pduUd = SmsPduUtil.getSeptets(msg.substring(udOffset));
				} else {
					pduUd = SmsPduUtil.getSeptets(msg.substring(udOffset, udOffset + udLength));
				}

				smsPdus[i] = new SmsPdu(pduUdhElements, pduUd, udLength, ud.getDcs());
			}
		}
		return smsPdus;
	}

	/**
	 * Converts this message into SmsPdu:s
	 * <p>
	 * If the message is too long to fit in one SmsPdu the message is divided into
	 * many SmsPdu:s with a 8-bit concat pdu UDH element.
	 * 
	 * @return Returns the message as SmsPdu:s
	 */
	public SmsPdu[] getPdus() {
		SmsPdu[] smsPdus;
		SmsUserData ud = getUserData();
		AbstractSmsDcs dcs = ud.getDcs();
		SmsUdhElement[] udhElements = getUdhElements();
		int udhLength = SmsUdhUtil.getTotalSize(udhElements);

		int nBytesLeft = dcs.getMaxMsglength() - udhLength;

//        switch (dcs.getAlphabet())
//        {
//        case GSM:
//            smsPdus = createOctalPdus(udhElements, ud, nBytesLeft);
//            break;
//        case UCS2:
//            smsPdus = createUnicodePdus(udhElements, ud, nBytesLeft);
//            break;
//        case ASCII:
//        	smsPdus = createOctalPdus(udhElements, ud, nBytesLeft); 
//            break;
//        case RESERVED:
//            smsPdus = createUnicodePdus(udhElements, ud, nBytesLeft);  //在CMPP协议里表示字符以GBK编码，因此要避免一个分片结尾一个汉字被分为两半的问题
//            break;
//        case LATIN1:
//        default:
//            smsPdus = createOctalPdus(udhElements, ud, nBytesLeft);
//            break;
//        }
		smsPdus = createPdus(udhElements, ud, nBytesLeft);
		return smsPdus;
	}

	public void setSeqNoKey(String seqNoKey) {
		this.seqNoKey = seqNoKey;
	}
}
