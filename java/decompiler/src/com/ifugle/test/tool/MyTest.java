/**
 * Copyright(C) 2016 Fugle Technology Co. Ltd. All rights reserved.
 *
 */
package com.ifugle.test.tool;

/**
 * @author CaiBo
 * @date 2016年4月7日 上午10:05:19
 * @version $Id$
 *
 */
public class MyTest {

	public static void main(String[] args) {
		DecompilerUtil.decompiler("resources/jars", "resources/result");
	}
}
