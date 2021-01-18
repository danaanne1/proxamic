package com.theunknowablebits.proxamic;

import java.nio.ByteBuffer;

/** All document classes implement this */
public interface Document {

	public <T extends DocumentView> T as(final Class<T> documentClass);

	public Document newInstance();

	public Document newInstance(ByteBuffer data);

	public default <T extends DocumentView> T newInstance(Class<T> viewClass) {
		return newInstance().as(viewClass);
	}
	
	public default <T extends DocumentView> T newInstance(Class<T> viewClass, ByteBuffer bytes) {
		return newInstance(bytes).as(viewClass);
	}

	ByteBuffer toByteBuffer();
	
	byte [] toBytes();
	
}
