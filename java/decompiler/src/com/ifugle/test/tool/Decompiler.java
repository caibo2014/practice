/**
 * Copyright(C) 2016 Fugle Technology Co. Ltd. All rights reserved.
 *
 */
package com.ifugle.test.tool;

import java.util.List;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.relationship.MemberNameResolver;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Functional;
import org.benf.cfr.reader.util.ListFactory;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.DumperFactory;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.NopSummaryDumper;
import org.benf.cfr.reader.util.output.SummaryDumper;
import org.benf.cfr.reader.util.output.ToStringDumper;

/**
 * @author CaiBo
 * @date 2016年4月6日 下午7:59:40
 * @version $Id$
 *
 */
public final class Decompiler {

	private Decompiler() {

	}

	static void doClass(DCCommonState dcCommonState, String path, DumperFactory dumperFactory) {
		Options options = dcCommonState.getOptions();
		IllegalIdentifierDump illegalIdentifierDump = IllegalIdentifierDump.Factory.get(options);
		Dumper d = new ToStringDumper();
		try {
			NopSummaryDumper summaryDumper = new NopSummaryDumper();
			ClassFile c = dcCommonState.getClassFileMaybePath(path);
			dcCommonState.configureWith(c);
			try {
				c = dcCommonState.getClassFile(c.getClassType());
			} catch (CannotLoadClassException e) {
				e.printStackTrace();
			}
			if (options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES).booleanValue()) {
				c.loadInnerClasses(dcCommonState);
			}
			if (options.getOption(OptionsImpl.RENAME_DUP_MEMBERS).booleanValue()) {
				MemberNameResolver.resolveNames(dcCommonState, ListFactory.newList(dcCommonState.getClassCache().getLoadedTypes()));
			}
			c.analyseTop(dcCommonState);
			TypeUsageCollector collectingDumper = new TypeUsageCollector(c);
			c.collectTypeUsages(collectingDumper);
			d = dumperFactory.getNewTopLevelDumper(options, c.getClassType(), summaryDumper, collectingDumper.getTypeUsageInformation(), illegalIdentifierDump);
			String methname = options.getOption(OptionsImpl.METHODNAME);
			if (methname == null) {
				c.dump(d);
			} else {
				try {
					for (Method method : c.getMethodByName(methname)) {
						method.dump(d, true);
					}
				} catch (NoSuchMethodException e) {
					throw new IllegalArgumentException("No such method '" + methname + "'.");
				}
			}
			d.print("");
		} catch (ConfusedCFRException e) {
			System.err.println(e.toString());
			for (StackTraceElement x : e.getStackTrace()) {
				System.err.println(x);
			}
		} catch (CannotLoadClassException e) {
			System.out.println("Can't load the class specified:");
			System.out.println(e.toString());
		} catch (RuntimeException e) {
			System.err.println(e.toString());
			for (StackTraceElement x : e.getStackTrace()) {
				System.err.println(x);
			}
		} finally {
			if (d != null) {
				d.close();
			}
		}
	}

	static void doJar(DCCommonState dcCommonState, String path, DumperFactory dumperFactory) {
		Options options = dcCommonState.getOptions();
		IllegalIdentifierDump illegalIdentifierDump = IllegalIdentifierDump.Factory.get(options);
		SummaryDumper summaryDumper = null;
		boolean silent = true;
		try {
			final Predicate<String> matcher = MiscUtils.mkRegexFilter(options.getOption(OptionsImpl.JAR_FILTER), true);
			silent = options.getOption(OptionsImpl.SILENT);
			summaryDumper = dumperFactory.getSummaryDumper(options);
			summaryDumper.notify("Summary for " + path);
			summaryDumper.notify("Decompiled with CFR 0_115");
			if (!silent) {
				System.err.println("Processing " + path + " (use " + OptionsImpl.SILENT.getName() + " to silence)");
			}
			List<JavaTypeInstance> types = dcCommonState.explicitlyLoadJar(path);
			types = Functional.filter(types, new Predicate<JavaTypeInstance>() {

				@Override
				public boolean test(JavaTypeInstance in) {
					return matcher.test(in.getRawName());
				}
			});
			if (options.getOption(OptionsImpl.RENAME_DUP_MEMBERS).booleanValue() || options.getOption(OptionsImpl.RENAME_ENUM_MEMBERS).booleanValue()) {
				MemberNameResolver.resolveNames(dcCommonState, types);
			}
			for (JavaTypeInstance type : types) {
				Dumper d = new ToStringDumper();
				try {
					ClassFile c = dcCommonState.getClassFile(type);
					if (c.isInnerClass()) {
						d = null;
						continue;
					}
					if (!silent) {
						System.err.println("Processing " + type.getRawName());
					}
					if (options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES).booleanValue()) {
						c.loadInnerClasses(dcCommonState);
					}
					c.analyseTop(dcCommonState);
					TypeUsageCollector collectingDumper = new TypeUsageCollector(c);
					c.collectTypeUsages(collectingDumper);
					d = dumperFactory.getNewTopLevelDumper(options, c.getClassType(), summaryDumper, collectingDumper.getTypeUsageInformation(),
							illegalIdentifierDump);
					c.dump(d);
					d.print("\n");
					d.print("\n");
					continue;
				} catch (Dumper.CannotCreate e) {
					throw e;
				} catch (RuntimeException e) {
					d.print(e.toString()).print("\n").print("\n").print("\n");
					continue;
				} finally {
					if (d != null) {
						d.close();
					}
				}
			}
		} catch (RuntimeException e) {
			String err = "Exception analysing jar " + e;
			System.err.println(err);
			if (summaryDumper != null) {
				summaryDumper.notify(err);
			}
		} finally {
			if (summaryDumper != null) {
				summaryDumper.close();
			}
		}
	}

}
