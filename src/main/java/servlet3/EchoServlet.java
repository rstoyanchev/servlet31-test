package servlet3;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Arjen Poutsma
 */
public class EchoServlet extends HttpServlet {

	public static final int BUFFER_SIZE = 8 * 1024;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		AsyncContext context = req.startAsync(req, resp);
		context.setTimeout(0);

		Echo echo = new Echo(context);
		req.getInputStream().setReadListener(echo);
		resp.getOutputStream().setWriteListener(echo);
	}

	private class Echo implements ReadListener, WriteListener {

		private byte[] buffer = new byte[BUFFER_SIZE];
		private long totalRead = 0;
		private long totalWrote = 0;
		private long totalBuffered = 0;
		private LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

		private AsyncContext asyncContext;
		private ServletInputStream input;
		private ServletOutputStream output;

		private Echo(AsyncContext asyncContext) throws IOException {
			this.asyncContext = asyncContext;
			this.input = asyncContext.getRequest().getInputStream();
			this.output = asyncContext.getResponse().getOutputStream();
		}

		@Override
		public void onDataAvailable() throws IOException {
			while (input.isReady()) {
				int read = input.read(buffer);
				totalRead += read;

				if (output.isReady()) {
					totalWrote += read;
					output.write(buffer, 0, read);
				} else {
					totalBuffered += read;
					queue.add(Arrays.copyOf(buffer, read));
				}
			}
		}

		@Override
		public void onAllDataRead() throws IOException {
			System.out.println("Read Done! Total Wrote [" + totalWrote + "]  " +
					"Total Read [" + totalRead + "]  " +
					"Total Buffered [" + totalBuffered + "]  " +
					"Buffer size [" + queue.size() + "]");
			onWritePossible();
		}

		@Override
		public void onWritePossible() throws IOException {
			if (input.isFinished()) {
				if (!queue.isEmpty()) {
					while (output.isReady()) {
						output.write(queue.poll());
						if (queue.isEmpty()) {
							asyncContext.complete();
							return;
						}
					}
				} else {
					asyncContext.complete();
				}
			} else {
				onDataAvailable();
			}
		}

		@Override
		public void onError(Throwable failure) {
			System.out.println("echo failure" +  failure);
		}

	}

}