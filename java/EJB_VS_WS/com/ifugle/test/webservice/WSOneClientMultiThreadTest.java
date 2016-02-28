package com.ifugle.test.webservice;

import com.ifugle.test.webservice.base.Gt3AipWebService;
import com.ifugle.test.webservice.base.Gt3AipWebService_Service;

/**
 * @author CaiBo
 * @date 2016年2月27日 下午9:03:18
 * @version $Id$
 * 
 */
public class WSOneClientMultiThreadTest {

	public static void main(String[] args) throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><service xmlns=\"http://www.chinatax.gov.cn/spec/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">	<head>		<tran_id>SWZJ.GSGL.SB.CXZRRDJXXWBXT</tran_id>		<channel_id>GDDS.ZJNB.GSHD</channel_id>		<tran_seq>eea4ff11269045068f421dd3d654804e</tran_seq>		<tran_date>20120416</tran_date>		<tran_time>083231475</tran_time>		<expand>			<name>identityType</name>			<value>49308fd2f75d462594648203c1ae5e93</value>		</expand>		<expand>			<name>sjry</name>			<value>24406060733</value>		</expand>		<expand>			<name>sjjg</name>			<value>24406820018</value>		</expand>	</head>	<body><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?><taxML xsi:type=\"zrrxxcxrequest\" xmlbh=\"String\" bbh=\"String\" xmlmc=\"String\" xsi:schemaLocation=\"http://www.chinatax.gov.cn/dataspec/ TaxMLBw_GSGL_SB_001_Request_v1.0.xsd\" xmlns=\"http://www.chinatax.gov.cn/dataspec/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">	<djxh>20124400001005357570</djxh>	<zjzlDm></zjzlDm>	<zjhm></zjhm>	<xm></xm>	<nsrsbh></nsrsbh></taxML>]]></body></service>";
		Gt3AipWebService_Service service = new Gt3AipWebService_Service();
		Gt3AipWebService port = service.getGt3AipWebServiceSoapPort();
		int countArray[] = { 1, 10, 100, 1000, 10000, 100000 };
		for (int i = 0; i < 20; i++) {
			new Thread(new WsRequest(xml, port, countArray)).start();
		}
	}

}

class WsRequest implements Runnable {

	private final String xml;
	private final Gt3AipWebService port;
	private final int countArray[];

	public WsRequest(String xml, Gt3AipWebService port, int countArray[]) {
		this.countArray = countArray;
		this.xml = xml;
		this.port = port;
	}

	@Override
	public void run() {
		for (int i = 0; i < countArray.length; i++) {
			testMultiTimes(xml, countArray[i], port);
			System.out.println("-------------------------------------");
		}
	}

	private static void testMultiTimes(String xml, int count, Gt3AipWebService port) {
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			testOnce(xml, port);
		}
		long t2 = System.currentTimeMillis();
		System.out.println("times:" + count + " time:" + (t2 - t1));
	}

	private static void testOnce(String xml, Gt3AipWebService port) {
		port.sendBaseXMLEsbWebService(xml);
	}

}
