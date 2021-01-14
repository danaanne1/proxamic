package com.theunknowablebits.proxamic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
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
				return handleGet(proxy,method,args,method.getAnnotation(Getter.class).value());
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
				if (!Proxy.isProxyClass(args[1].getClass()))
					return false;
				InvocationHandler handler = Proxy.getInvocationHandler(args[1]);
				if (!BuffHandler.class.isAssignableFrom(handler.getClass()))
					return false;
				return BuffDocument.this.equals(((BuffHandler)handler).getDocument());
			}
			if (methodName.equals("hashCode")) {
				return BuffDocument.this.hashCode();
			}
			if (methodName.startsWith("get")) {
				return handleGet(proxy, method, args, methodName.substring(3));
			}
			if (methodName.startsWith("set")) {
				return handleSet(proxy, method, args, methodName.substring(3));
			}
			return null;
		}
		public Document getDocument() {
			return BuffDocument.this;
		}
	}

	@SuppressWarnings("unchecked")
	private Object handleGet(Object proxy, Method method, Object [] args, String fieldName) {
		// determine the result type of the method
		Class<?> resultType = method.getReturnType();
		
		if (resultType.isArray()) {
			return mapToArray(resultType.getComponentType(),(Array)root.get(fieldName));
		}

		ParameterizedType genericResultType = (ParameterizedType)method.getGenericReturnType();

		if (List.class.isAssignableFrom(resultType)) {
			return mapToList((Class<?>)genericResultType.getActualTypeArguments()[0],(Array)root.get(fieldName));
		}
		
		if (Map.class.isAssignableFrom(resultType)) {

			return mapToMap(String.class,(Class <?>)genericResultType.getActualTypeArguments()[1], (Struct)root.get(fieldName));
		}
		
		// -----------------------------
		// direct conversions below this line 
		
		if (DocumentView.class.isAssignableFrom(resultType)) {
			return mapToObject((Class<? extends DocumentView>)resultType,(Struct)root.get(fieldName));
		}

		return mapToPrimitive(resultType,root.get(fieldName));

	}

	private Object mapToArray(Class<?> componentType, Array array) {
		return null;
	}
	
	private <B> List<B> mapToList(Class<B> valueType, Array array) {
		return null;
	}

	private <A,B> Map<A,B> mapToMap(Class<A> keyType, Class<B> valueType, Struct struct) {
		return null;
	}

	private DocumentView mapToObject(Class<? extends DocumentView> type, Struct struct) {
		return new BuffDocument(struct).as(type);
	}
	
	private Object mapToPrimitive(Class<?> type, Object value) {
		return type.cast(value);
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
	
}
