package com.theunknowablebits.proxamic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
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
public class BuffDocument implements Document, DocumentStoreAware {

	private static final long serialVersionUID = 1L;

	private static final DocumentStore defaultDocStore = new InMemoryDocumentStore();	

	public transient Struct root;
	
	private transient DocumentStore docStore = defaultDocStore;
	
	
	/**
	 * If the document store is serializable (see network aware doc stores) then this will serialize the doc store
	 * followed by the document id. It is the responsibility of the caller to save documents before serialization.
	 * <p>
	 * If the document store is not serializable, will serialize the document as a byte stream
	 * <p>
	 * @param out
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream out)
			throws IOException 
	{
		out.defaultWriteObject();
		if (docStore instanceof Serializable) {
			out.writeObject(docStore);
			out.writeObject(docStore.getID(this));
			return;
		}
		out.writeObject(null);
		out.writeObject(toBytes());
	}

	/** only used when restoring from a doc store style serialization */
	private transient String resolveKey = null;

	private void readObject(ObjectInputStream in)
		     throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		DocumentStore store = (DocumentStore)in.readObject();
		if (store != null) {
			docStore = store;
			resolveKey = (String)in.readObject();
			return;
		}
		docStore = defaultDocStore;
		root = new Struct(ByteBuffer.wrap((byte [])in.readObject()));
	}

	private Object readResolve() throws ObjectStreamException
	{
		if (resolveKey!=null)
			return docStore.get(resolveKey);
		return this;
	}
	
	private BuffDocument(Struct root) {
		this.root = root;
	}

	protected BuffDocument(ByteBuffer buffer) {
		this(new Struct(buffer));
	}

	protected BuffDocument() {
		this(new Struct());
	}

	public Document newInstance() {
		return docStore.newInstance();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends DocumentView> T as(final Class<T> documentClass) {
		return (T)Proxy.newProxyInstance(
				getClass().getClassLoader(), 
				new Class [] { documentClass }, 
				new BuffHandler(documentClass));
	}

	@Override
	public void setDocumentStore(DocumentStore docStore) {
		this.docStore = docStore;
	}
	
	@Override
	public DocumentStore getDocumentStore() {
		return this.docStore;
	}
	
	private class BuffHandler implements InvocationHandler, Serializable {
		Class<? extends DocumentView> documentClass;
		
		// visible for serialization
		protected BuffHandler() {
			
		}

		public BuffHandler(Class<? extends DocumentView> documentClass) {
			this.documentClass = documentClass;
		}
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			if (method.isAnnotationPresent(Getter.class)) {
				return convertFromStructValue(
								method.getGenericReturnType(), 
								createIfRequired(method.getAnnotation(Getter.class).value(), method.getReturnType())
						);
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
				if (args[0] instanceof DocumentView) 
					return BuffDocument.this.equals(((DocumentView)args[0]).document());
				return false;
			}
			if (methodName.equals("hashCode")) {
				return BuffDocument.this.hashCode();
			}
			if (methodName.startsWith("get")) { 
				return convertFromStructValue
						(
								method.getGenericReturnType(), 
								createIfRequired(methodName.substring(3),method.getReturnType())							
						);
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
	
		private Object createIfRequired(String fieldName, Class<?> returnType) {
			if (!root.has(fieldName)) {
				if (List.class.isAssignableFrom(returnType)) {
					root.put(fieldName, new Array());
				}
				if (Map.class.isAssignableFrom(returnType) || DocumentView.class.isAssignableFrom(returnType)) {
					root.put(fieldName, new Struct());
				}
			}
			return root.get(fieldName);
		}
	}


	@SuppressWarnings("unchecked")
	private Object convertFromStructValue(Type methodReturnType, Object structValue) {

		if (structValue==null)
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

		if (DocumentView.class.isAssignableFrom(returnType)) { 
			return new BuffDocument((Struct)structValue).as((Class<? extends DocumentView>) returnType);
		}
			
		return returnType.cast(structValue);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T[] mapToArray(Class<T> arrayType, Array array) {
		if (array==null) {
			// return (T[])java.lang.reflect.Array.newInstance(arrayType,0);
			return null;
		}
		T [] result = (T[])java.lang.reflect.Array.newInstance(arrayType, array.size());
		for (int i = 0; i < array.size(); i++) {
			result[i] = arrayType.cast(convertFromStructValue(arrayType, array.get(i)));
		}
		return result;
	}

	private <T> Array mapFromArray(Class<?> arrayType, Object [] value) {
		Array result = new Array();
		for (int i = 0; i < value.length; i++) {
			result.add(convertToStructValue(arrayType, value[i]));
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
	
	/**
	 * 
	 * @param declaredType The declared type of value
	 * @param value
	 * @return 
	 */
	private Object convertToStructValue(Type declaredType, Object value) {
		
		if (value == null) 
			return null;
		
		Class<?> returnType = Object.class;
		if (declaredType instanceof Class) 
			returnType = (Class<?>)declaredType;
		if (declaredType instanceof ParameterizedType) 
			returnType = (Class<?>)((ParameterizedType)declaredType).getRawType();

		if (returnType.isArray()) {
			return mapFromArray(returnType.getComponentType(),(Object [])value);
		}

		if (List.class.isAssignableFrom(returnType)) {
			throw new RuntimeException("Cannot replace a synthetic collection. Please operate through the collections members.");
		}
		
		
		if (Map.class.isAssignableFrom(returnType)) {
			throw new RuntimeException("Cannot replace a synthetic collection. Please operate through the collections members.");
		}
		
		if (DocumentView.class.isAssignableFrom(returnType)) {
			return ((BuffDocument)((DocumentView)value).document()).root;
		}

		return returnType.cast(value);
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

	@Override
	public ByteBuffer toByteBuffer() {
		return root.toByteBuffer();
	}

	@Override
	public byte[] toBytes() {
		ByteBuffer buf = toByteBuffer();
		byte [] bytes = new byte[buf.limit()];
		buf.get(bytes,0,buf.limit());
		return bytes;
	}
}
