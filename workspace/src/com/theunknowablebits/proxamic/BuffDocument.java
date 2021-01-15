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
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			if (method.isAnnotationPresent(Getter.class)) {
				return convertFromStructValue(method.getGenericReturnType(), root.get(method.getAnnotation(Getter.class).value()));
			}
			if (method.isAnnotationPresent(Setter.class)) {
				return fluently
						(
								proxy,
								method,
								root.put 
								(
										method.getAnnotation(Setter.class).value(), 
										convertToStructValue
										(
												method.getGenericParameterTypes()[0],
												args[0]
										)
								)
							);
			}
			if (method.isDefault()) {
				return handleDefaultMethod(proxy,method,args,documentClass);
			}
			String methodName = method.getName();
			if (methodName.equals("document")) { 
				return BuffDocument.this;
			}
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
				return convertFromStructValue(method.getGenericReturnType(), root.get(methodName.substring(3)));
			}
			if (methodName.startsWith("set")) {
				return fluently
						(
								proxy,
								method,
								root.put
								(
										methodName.substring(3), 
										convertToStructValue
										(
												method.getGenericParameterTypes()[0], 
												args[0]
										)
								)
						);
			}
			if (methodName.startsWith("with")) {
				root.put(methodName.substring(4), convertToStructValue(method.getGenericParameterTypes()[0], args[0]));
				return proxy;
			}
			throw new RuntimeException("No path to invoke for " + method.getName());
		}
		private Object fluently(Object proxy, Method method, Object structValue) {
			if ( (!method.getName().startsWith("with")) &&  method.getReturnType()==method.getParameterTypes()[0]) {
				return convertFromStructValue( method.getGenericReturnType(), structValue );
			} else {
				return proxy;
			}
		}
	
	}


	@SuppressWarnings("unchecked")
	private Object convertFromStructValue(Type methodReturnType, Object structValue) {
		if (structValue == null) 
			return null;
		
		Class<?> returnType = Object.class;
		if (methodReturnType instanceof Class) 
			returnType = (Class<?>)methodReturnType;
		if (methodReturnType instanceof ParameterizedType) 
			returnType = (Class<?>)((ParameterizedType)methodReturnType).getRawType();

		if (returnType.isArray()) 
			return mapToArray(returnType.getComponentType(),(Array)structValue);

		if (List.class.isAssignableFrom(returnType)) {
			if (methodReturnType instanceof ParameterizedType) 
				return mapToList(((ParameterizedType)methodReturnType).getActualTypeArguments()[0], (Array)structValue);
			return mapToList(Object.class,(Array)structValue);
		}
		
		if (Map.class.isAssignableFrom(returnType)) {
			if (methodReturnType instanceof ParameterizedType) 
				return mapToMap(((ParameterizedType)methodReturnType).getActualTypeArguments()[1],(Struct)structValue);
			return mapToMap(Object.class,(Struct)structValue);
		}
		
		if (DocumentView.class.isAssignableFrom(returnType)) 
			return new BuffDocument((Struct)structValue).as((Class<? extends DocumentView>) returnType);

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
	
	private <B> List<B> mapToList(final Type elementClass, final Array array) {
		return new AbstractList<B>() {
			@SuppressWarnings("unchecked")
			@Override
			public B set(int index, B element) {
				return (B)convertFromStructValue(elementClass, array.set(index, convertToStructValue(elementClass, element)));
			}
			@Override
			public void add(int index, B element) {
				array.add(index, convertToStructValue(elementClass, element));
			}
			@SuppressWarnings("unchecked")
			@Override
			public B remove(int index) {
				return (B)convertFromStructValue(elementClass,array.remove(index));
			}
			@SuppressWarnings("unchecked")
			@Override
			public B get(int index) {
				return (B)convertFromStructValue(elementClass, array.get(index));
			}
			@Override
			public int size() {
				return array.size();
			}
		};
	}

	private <B> Map<String,B> mapToMap(final Type valueType, final Struct struct) {
		return new AbstractMap<String, B>() {

			@Override
			public Set<Entry<String, B>> entrySet() {
				return new AbstractSet<Map.Entry<String,B>>() {

					@Override
					public Iterator<Entry<String, B>> iterator() {
						return new Iterator<Entry<String, B>>() {
							final Iterator<String> delegate = struct.keySet().iterator();

							@Override
							public boolean hasNext() {
								return delegate.hasNext();
							}

							@SuppressWarnings("unchecked")
							@Override
							public Entry<String, B> next() {
								String key = delegate.next();
								return new SimpleImmutableEntry<String, B>(key,(B) convertFromStructValue(valueType, struct.get(key)));
							}
							
							@Override
							public void remove() {
								delegate.remove();
							}
							
						};
					}

					@Override
					public int size() {
						return struct.keySet().size();
					}
				};
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public B put(String key, B value) {
				return (B) convertFromStructValue(valueType, struct.put(key, convertToStructValue(valueType, value)));
			}
		
		};
	}
	
	private Object convertToStructValue(Type declaredType, Object value) {
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
	
	@Override
	public int hashCode() {
		return root.hashCode();
	}
}
