package com.theunknowablebits.proxamic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import com.theunknowablebits.buff.serialization.Array;
import com.theunknowablebits.buff.serialization.Struct;

/**
 * A place where Dynamic Documents based on byte buffers come to life.
 * 
 * @author Dana
 *
 */
public class BuffDocument implements Document {

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

	@Override
	public <T extends DocumentView> T newInstance(Class<T> documentClass) {
		return new BuffDocument(new Struct()).as(documentClass);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends DocumentView> T as(final Class<T> documentClass) {
		return (T)Proxy.newProxyInstance(
				getClass().getClassLoader(), 
				new Class [] { documentClass }, 
				new BuffHandler(documentClass));
	}

	private class BuffHandler implements InvocationHandler {
		Class<? extends DocumentView> documentClass;

		public BuffHandler(Class<? extends DocumentView> documentClass) {
			this.documentClass = documentClass;
		}
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.isAnnotationPresent(Getter.class))
				return convertFromStructValue(method.getReturnType(), root.get(method.getAnnotation(Getter.class).value()));
			if (method.isAnnotationPresent(Setter.class))
				return handleSet(proxy,method,args,method.getAnnotation(Setter.class).value());
			if (method.isDefault())
				return handleDefaultMethod(proxy,method,args,documentClass);
			String methodName = method.getName();
			if (methodName.equals("document"))
				return BuffDocument.this;
			if (methodName.equals("equals")) {
				if (args.length!=1)
					return false;
				if (args[0] instanceof DocumentView)
					return BuffDocument.this.equals(((DocumentView)args[0]).document());
				return false;
			}
			if (methodName.equals("hashCode")) {
				return BuffDocument.this.hashCode();
			}
			if (methodName.startsWith("get")) {
				return convertFromStructValue(method.getReturnType(), root.get(methodName.substring(3)));
			}
			if (methodName.startsWith("set")) {
				return handleSet(proxy, method, args, methodName.substring(3));
			}
			return null;
		}
	}


	@SuppressWarnings("unchecked")
	private Object convertFromStructValue(Class<?> returnType, Object structValue) {
		
		if (returnType.isArray()) {
			return mapToArray(returnType.getComponentType(),(Array)structValue);
		}

		if (List.class.isAssignableFrom(returnType)) {
			return mapToList((Array)structValue);
		}
		
		if (Map.class.isAssignableFrom(returnType)) {
			return mapToMap((Struct)structValue);
		}
		
		if (DocumentView.class.isAssignableFrom(returnType)) {
			return new BuffDocument((Struct)structValue).as((Class<? extends DocumentView>) returnType);
		}

		return returnType.cast(structValue);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T[] mapToArray(Class<T> arrayType, Array array) {
		T [] result = (T[])java.lang.reflect.Array.newInstance(arrayType, array.size());
		for (int i = 0; i < array.size(); i++) {
			result[i] = arrayType.cast(convertFromStructValue(arrayType, array.get(i)));
		}
		return result;
	}
	
	private <B> List<B> mapToList(Array array) {
		return null;
	}

	private <A,B> Map<A,B> mapToMap(Struct struct) {
		return null;
	}
	
	private Object handleSet(Object proxy, Method method, Object [] args, String fieldName) {
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
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj) ||
				( BuffDocument.class.isAssignableFrom(obj.getClass()) && 
						root.equals(((BuffDocument)obj).root));
	}
}
