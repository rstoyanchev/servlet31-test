/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package servlet3;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Copied from:
 * https://docs.google.com/a/pivotal.io/presentation/d/10iO75mXuEmxp-FM5pRIp-v9BCDVgQ7YWjPIHkLGbjzc/edit#slide=id.g39da10049_0875
 */
public class JavaOneEchoServlet extends HttpServlet {

	public static final int SIZE = 8 * 1024;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		AsyncContext ctx = req.startAsync();
		Echo echo = new Echo(ctx);
		req.getInputStream().setReadListener(echo);
		resp.getOutputStream().setWriteListener(echo);
	}


	class Echo implements ReadListener, WriteListener {
		private final byte[] buffer = new byte[SIZE];
		private final AsyncContext ctx;
		private final ServletInputStream input;
		private final ServletOutputStream output;
		private final AtomicBoolean complete=
				new AtomicBoolean(false);

		Echo(AsyncContext ctx) throws IOException {
			this.ctx = ctx;
			input = ctx.getRequest().getInputStream();
			output = ctx.getResponse().getOutputStream();
		}

		public void onWritePossible() throws IOException  {
			if (input.isFinished())
			{
				if (complete.compareAndSet(false,true))
					ctx.complete();
				return;
			}
			while (input.isReady())
			{
				int read = input.read(buffer);
				output.write(buffer, 0, read);
				if (!output.isReady())
					break;
			}
		}
		public void onDataAvailable() throws IOException  {
			if (output.isReady())
				onWritePossible();
		}
		public void onAllDataRead() throws IOException  {
			if (output.isReady() && complete.compareAndSet(false,true))
				ctx.complete();
		}
		public void onError(Throwable failure)  {
			failure.printStackTrace();
		}
	}

}
