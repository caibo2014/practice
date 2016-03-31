/**
 * Copyright(C) 2016 Fugle Technology Co. Ltd. All rights reserved.
 *
 */
package com.ifugle.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * @author CaiBo
 * @date 2016年3月31日 上午9:48:54
 * @version $Id$
 *
 */
public class ReflectionUtil {

	private static final boolean includeObject = false;

	public static Set<Field> getAllFields(final Class<?> type, Predicate<? super Field>... predicates) {
		Set<Field> result = Sets.newHashSet();
		Set<Class<?>> allSuperTypes = getAllSuperTypes(type);
		for (Class<?> t : allSuperTypes) {
			result.addAll(getFields(t, predicates));
		}
		return result;
	}

	public static Set<Class<?>> getAllSuperTypes(final Class<?> type, Predicate<? super Class<?>>... predicates) {
		return filter(getAllSuperTypes(type), predicates);
	}

	private static Set<Class<?>> getAllSuperTypes(final Class<?> type) {
		Set<Class<?>> result = Sets.newLinkedHashSet();
		if (type != null && (includeObject || !type.equals(Object.class))) {
			result.add(type);
			result.addAll(getAllSuperTypes(type.getSuperclass()));
			for (Class<?> ifc : type.getInterfaces()) {
				result.addAll(getAllSuperTypes(ifc));
			}
		}
		return result;
	}

	private static Set<Field> getFields(Class<?> type, Predicate<? super Field>... predicates) {
		return filter(type.getDeclaredFields(), predicates);
	}

	private static <T> Set<T> filter(final T[] elements, Predicate<? super T>... predicates) {
		return filter(Arrays.asList(elements), predicates);
	}

	private static <T> Set<T> filter(final Iterable<T> elements, Predicate<? super T>... predicates) {
		if (isEmpty(predicates)) {
			return Sets.newHashSet(elements);
		}
		HashSet<T> result = Sets.newHashSet();
		Iterator<T> it = elements.iterator();
		le: while (it.hasNext()) {
			T e = it.next();
			for (Predicate<? super T> c : predicates) {
				assertNotNull(c, "predicate");
				if (!c.apply(e)) {
					continue le;
				}
			}
			result.add(e);
		}
		return result;
	}

	private static void assertNotNull(final Object argument, final String name) {
		if (argument == null) {
			throw new IllegalArgumentException(name + "may not be null");
		}
	}

	private static boolean isEmpty(Object[] objects) {
		return objects == null || objects.length == 0;
	}

}
