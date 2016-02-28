package com.ifugle.test;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author CaiBo
 * @date 2016年2月27日 上午10:31:57
 * @version $Id$
 * 
 */
public class EjbJboos7ClientTest {

	public static void main(String[] args) throws NamingException {
		final Hashtable jndiProperties = new Hashtable();
		jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");// 让JNDI
																					// API知道是由谁来管理我们用来查找JNDI
																					// 名字的命名空间的。
		final Context context = new InitialContext(jndiProperties);
		// appName 和 moduleName分别就打包的格式而定
		// 如果是.ear就是appName,其它的是moduleName(.jar,.war)
		final String appName = "";
		final String moduleName = "EJBDBTest";
		final String distinctName = "";
		// 实现类名
//		final String beanName = DB.class.getSimpleName();
//		System.out.println(beanName);
//		// 接口类名
//		final String viewClassName = DBRemote.class.getName();
//		System.out.println(viewClassName);
//		String jndi = "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + viewClassName;
//		System.out.println(jndi);
//		DBRemote db = (DBRemote) context.lookup(jndi);
		final String jndi = "ejb:";
		context.lookup(jndi);
		System.out.println("---------------------end");
	}

}
