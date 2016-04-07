/*
 * Decompiled with CFR 0_115.
 */
package com.ifugle.test.tool;

import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.getopt.GetOptParser;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.DumperFactoryImpl;

public class Main {

	public static void main(String[] args) {
		GetOptParser getOptParser = new GetOptParser();
		Options options = null;
		try {
			options = getOptParser.parse(args, OptionsImpl.getFactory());
		} catch (Exception e) {
			getOptParser.showHelp(OptionsImpl.getFactory(), e);
			System.exit(1);
		}
		if (options.optionIsSet(OptionsImpl.HELP) || options.getOption(OptionsImpl.FILENAME) == null) {
			getOptParser.showOptionHelp(OptionsImpl.getFactory(), options, OptionsImpl.HELP);
			return;
		}
		ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);
		DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
		String path = options.getOption(OptionsImpl.FILENAME);
		String type = options.getOption(OptionsImpl.ANALYSE_AS);
		if (type == null) {
			type = dcCommonState.detectClsJar(path);
		}
		DumperFactoryImpl dumperFactory = new DumperFactoryImpl();
		if (type.equals("jar")) {
			Decompiler.doJar(dcCommonState, path, dumperFactory);
		} else {
			Decompiler.doClass(dcCommonState, path, dumperFactory);
		}
	}

}
