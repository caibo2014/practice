/**
 * Copyright(C) 2010 Fugle Technology Co. Ltd. All rights reserved.
 * 
 */
package com.ifugle.test.tool;

/**
 * @author WuJianqiang
 * @date 2010-9-27 上午12:08:30
 * @version $Id: UtilException.java 5415 2015-01-03 15:11:15Z WuJianqiang $
 * 
 */
public final class UtilException extends RuntimeException {
	private static final long serialVersionUID = 6931924709992425096L;

	/**
	 * Construct a <code>UtilException</code> with the specified detail message.
	 * 
	 * @param msg
	 *            the detail message
	 */
	public UtilException(String msg) {
		super(msg);
	}

	/**
	 * Construct a <code>UtilException</code> with the specified detail message and nested
	 * exception.
	 * 
	 * @param msg
	 *            the detail message
	 * @param cause
	 *            the nested exception
	 */
	public UtilException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * @param cause
	 */
	public UtilException(Throwable cause) {
		super(cause);
	}

}
