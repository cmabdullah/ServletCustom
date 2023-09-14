package com.loommigration;

import java.io.IOException;
import java.io.InputStream;

public class MyHttpServletRequest implements AutoCloseable {

	byte[] buffer = new byte[1024];

	StringBuilder stringBuilder = new StringBuilder();

	int len = 0;
	int position = 0;

	String uri = "";

	private InputStream inputStream;

	public MyHttpServletRequest(InputStream inputStream) {
		this.inputStream = inputStream;

		try {
			len = inputStream.read(buffer);

			if (len > 0) {
				stringBuilder.append(new String(buffer, 0, len));
			}
			position = stringBuilder.indexOf("\r\n\r\n");

			if (position > 0) {
				String[] request = stringBuilder.substring(0, position).split("\r\n");
				String firstLine = request[0];
				String[] firstLineArray = firstLine.split(" ");
				uri = firstLineArray[1];
			}

		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}

	}

	public int getPosition() {
		return position;
	}

	public String getUri() {
		return uri;
	}

	@Override
	public void close() throws Exception {
		throw new Exception("Close failed");
	}
}
