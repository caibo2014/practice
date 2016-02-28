package com.ifugle.test.ejb;

import gt3.esb.ejb.adapter.client.IEsbXmlMessageReceiver;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author CaiBo
 * @date 2016年2月26日 下午4:33:17
 * @version $Id$
 * 
 */
public class EJBMultiClientMultiTheadTest {

	public static void main(String[] args) throws NamingException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><service xmlns=\"http://www.chinatax.gov.cn/spec/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">	<head>		<tran_id>SWZJ.GSGL.SB.CXZRRDJXXWBXT</tran_id>		<channel_id>GDDS.ZJNB.GSHD</channel_id>		<tran_seq>eea4ff11269045068f421dd3d654804e</tran_seq>		<tran_date>20120416</tran_date>		<tran_time>083231475</tran_time>		<expand>			<name>identityType</name>			<value>49308fd2f75d462594648203c1ae5e93</value>		</expand>		<expand>			<name>sjry</name>			<value>24406060733</value>		</expand>		<expand>			<name>sjjg</name>			<value>24406820018</value>		</expand>	</head>	<body><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?><taxML xsi:type=\"zrrxxcxrequest\" xmlbh=\"String\" bbh=\"String\" xmlmc=\"String\" xsi:schemaLocation=\"http://www.chinatax.gov.cn/dataspec/ TaxMLBw_GSGL_SB_001_Request_v1.0.xsd\" xmlns=\"http://www.chinatax.gov.cn/dataspec/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">	<djxh>20124400001005357570</djxh>	<zjzlDm></zjzlDm>	<zjhm></zjhm>	<xm></xm>	<nsrsbh></nsrsbh></taxML>]]></body></service>";


		IEsbXmlMessageReceiver beanArray[] = new IEsbXmlMessageReceiver[20];
		for (int i = 0; i < 20; i++) {
			Properties props = new Properties();
			props.setProperty("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
			props.setProperty("java.naming.provider.url", "t3://150.12.56.135:8001");
			InitialContext ctx = new InitialContext(props);
			IEsbXmlMessageReceiver r = (IEsbXmlMessageReceiver) ctx.lookup("gt3.esb.ejb.AipEJBAdapterXml");
			beanArray[i] = r;
		}

		int countArray[] = { 1, 10, 100, 1000, 10000, 100000 };
		for (int i = 0; i < 20; i++) {
			new Thread(new EJBRequest1(xml, beanArray[i], countArray)).start();
		}
	}
}
class EJBRequest1 implements Runnable {

	private final String xml;
	private final IEsbXmlMessageReceiver lookup;
	private final int countArray[];

	public EJBRequest1(String xml, IEsbXmlMessageReceiver lookup, int countArray[]) {
		this.xml = xml;
		this.lookup = lookup;
		this.countArray = countArray;
	}

	@Override
	public void run() {
		for (int i = 0; i < countArray.length; i++) {
			testMultiTimes(xml, lookup, countArray[i]);
			System.out.println("-------------------------------------");
		}
	}

	private static void testMultiTimes(String xml, IEsbXmlMessageReceiver lookup, int count) {
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			testOnce(xml, lookup);
		}
		long t2 = System.currentTimeMillis();
		String name = Thread.currentThread().getName();
		System.out.println("name:" + name + " times:" + count + " time:" + (t2 - t1));
	}

	private static void testOnce(String xml, IEsbXmlMessageReceiver lookup) {
		lookup.receiveMessageXML(xml);
	}

}

