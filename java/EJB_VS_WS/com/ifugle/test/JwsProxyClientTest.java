package com.ifugle.test;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 * @author CaiBo
 * @date 2016年2月26日 下午11:07:07
 * @version $Id$
 * 
 */
public class JwsProxyClientTest {
	public static void main(String[] args) throws MalformedURLException {
		URL wsdlURL = new URL("http://localhost/hello?wsdl");
		QName SERVICE_NAME = new QName("http://apache.org/hello_world_soap_http", "SOAPService");
		Service service = Service.create(wsdlURL, SERVICE_NAME);
		Greeter client = service.getPort(Greeter.class);
		String result = client.greetMe("test");
	}
}

class Greeter {
	public String greetMe(String str) {
		return null;
	}
}
