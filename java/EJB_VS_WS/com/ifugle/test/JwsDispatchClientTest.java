package com.ifugle.test;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

/**
 * @author CaiBo
 * @date 2016年2月26日 下午11:15:12
 * @version $Id$
 * 
 */
public class JwsDispatchClientTest {
	public static void main(String[] args) throws MalformedURLException {
		URL wsdlURL = new URL("http://localhost/hello?wsdl");
		Service service = Service.create(wsdlURL, new QName("HelloService"));
		Dispatch<Source> disp = service.createDispatch(new QName("HelloPort"), Source.class, Service.Mode.PAYLOAD);

		Source request = new StreamSource("<hello/>");
		Source response = disp.invoke(request);
	}
}
