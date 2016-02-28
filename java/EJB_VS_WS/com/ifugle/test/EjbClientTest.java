package com.ifugle.test;

import gt3.esb.ejb.adapter.client.IEsbXmlMessageReceiver;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import javax.naming.InitialContext;

import com.ifugle.test.webservice.base.Gt3AipWebService;

/**
 * @author CaiBo
 * @date 2016年2月26日 下午4:33:17
 * @version $Id$
 * 
 */
public class EjbClientTest {

	public static void main(String[] args) {
		Properties props = new Properties();
		props.setProperty("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
		props.setProperty("java.naming.provider.url", "t3://150.12.56.135:8001");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><service xmlns=\"http://www.chinatax.gov.cn/spec/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">	<head>		<tran_id>SWZJ.GSGL.SB.CXZRRDJXXWBXT</tran_id>		<channel_id>GDDS.ZJNB.GSHD</channel_id>		<tran_seq>eea4ff11269045068f421dd3d654804e</tran_seq>		<tran_date>20120416</tran_date>		<tran_time>083231475</tran_time>		<expand>			<name>identityType</name>			<value>49308fd2f75d462594648203c1ae5e93</value>		</expand>		<expand>			<name>sjry</name>			<value>24406060733</value>		</expand>		<expand>			<name>sjjg</name>			<value>24406820018</value>		</expand>	</head>	<body><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?><taxML xsi:type=\"zrrxxcxrequest\" xmlbh=\"String\" bbh=\"String\" xmlmc=\"String\" xsi:schemaLocation=\"http://www.chinatax.gov.cn/dataspec/ TaxMLBw_GSGL_SB_001_Request_v1.0.xsd\" xmlns=\"http://www.chinatax.gov.cn/dataspec/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">	<djxh>20124400001005357570</djxh>	<zjzlDm></zjzlDm>	<zjhm></zjhm>	<xm></xm>	<nsrsbh></nsrsbh></taxML>]]></body></service>";

		try {
			System.out.println("ok-----------0");
			InitialContext ctx = new InitialContext(props);
			IEsbXmlMessageReceiver lookup = (IEsbXmlMessageReceiver)ctx.lookup("gt3.esb.ejb.AipEJBAdapterXml");
			System.out.println(lookup.getClass().getSuperclass().getName());
			System.out.println(lookup.receiveMessageXML(xml));
			System.exit(0);
			Class<?>[] interfaces = lookup.getClass().getInterfaces();
			for (int i = 0; i < interfaces.length; i++) {
				System.out.println(interfaces[i].getName());
				for (Method method : interfaces[i].getMethods()) {
					System.out.println(method.isAccessible() + ":" + method.getName() + ":" + method.getParameterTypes().length + ":" + method.getReturnType());
				}
				System.out.println("---------------------------------------");
			}
			System.out.println("****************************************");
//			while (lookup.getClass().getSuperclass() != Object.class) {
//				lookup = lookup.getClass().getSuperclass();
//				System.out.println(lookup.getClass().getName());
//			}
			Class<? extends Object> class1 = lookup.getClass();
			System.out.println(class1.getName());
			Method[] methods = class1.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				System.out.println(method.isAccessible() + ":" + method.getName() + ":" + method.getParameterTypes().length);
			}

			// Method method = lookup.getClass().getMethod("receiveMessageXML");
			// Object invoke = method.invoke(lookup, xml);
			// System.out.println(invoke);
			System.out.println("ok-----------1");
			// Gt3AipWebService ser = (Gt3AipWebService)lookup;
			// ser.sendBaseXMLEsbWebService(xml);

			// Remote r = (Remote)lookup;
			// r.receiveMessageXML();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
