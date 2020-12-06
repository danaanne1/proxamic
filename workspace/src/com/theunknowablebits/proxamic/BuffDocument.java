package com.theunknowablebits.proxamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.HashMap;

import com.theunknowablebits.buff.serialization.Struct;

/**
 * A place where buff dynamic objects come to life.
 * 
 * @author Dana
 *
 */
public class BuffDocument {

	public Struct root;

	private BuffDocument(Struct root) {
		this.root = root;
	}

	public BuffDocument(ByteBuffer buffer) {
		this(new Struct(buffer));
	}

	public BuffDocument() {
		this(new Struct());
	}

	public static <T extends Document> T newInstance(Class<T> documentClass) {
		return new BuffDocument(new Struct()).as(documentClass);
	}

	@SuppressWarnings("unchecked")
	public <T extends Document> T as(final Class<T> documentClass) {
		return (T)Proxy.newProxyInstance(
				getClass().getClassLoader(), 
				new Class [] { documentClass }, 
				new BuffHandler(documentClass));
	}

	private class BuffHandler implements InvocationHandler {
		Class<? extends Document> documentClass;

		public BuffHandler(Class<? extends Document> documentClass) {
			this.documentClass = documentClass;
		}
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.isAnnotationPresent(Getter.class))
				return handleGet(proxy,method,args,method.getAnnotation(Getter.class));
			if (method.isAnnotationPresent(Setter.class))
				return handleSet(proxy,method,args,method.getAnnotation(Setter.class));
			if (method.getName().equals("document"))
				return BuffDocument.this;
			if (method.isDefault())
				return handleDefaultMethod(proxy,method,args,documentClass);
			if (method.getName().equals("equals")) {
				if (args.length!=1)
					return false;
				if (!Proxy.isProxyClass(args[1].getClass()))
					return false;
				InvocationHandler handler = Proxy.getInvocationHandler(args[1]);
				if (!BuffHandler.class.isAssignableFrom(handler.getClass()))
					return false;
				return BuffDocument.this.equals(((BuffHandler)handler).getDocument());
			}
			if (method.getName().equals("hashCode")) {
				return BuffDocument.this.hashCode();
			}
			
			return null;
		}
		public BuffDocument getDocument() {
			return BuffDocument.this;
		}
	}

	private Object handleGet(Object proxy, Method method, Object [] args, Getter getter) {
		return null;
	}

	private Object handleSet(Object proxy, Method method, Object [] args, Setter setter) {
		return null;
	}

	private static final Object handleDefaultMethod(Object proxy, Method method, Object [] args, Class<?> documentClass) throws Throwable {
		// this can get very ugly between Java8 and Java 9+
		// for more detail see this
		// https://blog.jooq.org/2018/03/28/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/
		Lookup lookup;
		try {
			Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
			constructor.setAccessible(true);
			lookup = constructor.newInstance(documentClass);
		} catch (Exception e) {
			lookup = MethodHandles.lookup();
		}
		return 
			lookup
				.findSpecial(documentClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), documentClass)
				.bindTo(proxy)
				.invokeWithArguments(args);
	}
	
}
