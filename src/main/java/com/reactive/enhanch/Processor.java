package com.reactive.enhanch;

import java.nio.channels.SelectionKey;

public interface Processor {
	void read();
	void write();
	void connect(SelectionKey selectionKey);

	String getRequestId();
}
