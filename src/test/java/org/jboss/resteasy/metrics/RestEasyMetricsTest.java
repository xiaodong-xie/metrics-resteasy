package org.jboss.resteasy.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import org.junit.Test;

public class RestEasyMetricsTest extends ResourceTest {
	private static Throwable getRootCause(Throwable t) {
		Throwable rootCause = t;
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		System.out.println("root cause is : " + rootCause.getClass());
		return rootCause;
	}

	@Override
	protected void setUpResources() throws Exception {
		MyResource resource = new MyResource();
		addResource(resource);
	}

	@Test
	public void testResourceMetered() {

		Client client = getClient();
		String response = client.target(getContextBaseUrl() + "/myresource/test").request().get(String.class);
		assertEquals("hello world!", response);
		assertEquals(1, MetricRegistryContextResolver.METRICS.getMeters().size());
		assertTrue(
			MetricRegistryContextResolver.METRICS.getMeters()
				.containsKey("GET - /myresource/test.resource-meter")
		);
		assertEquals(1, MetricRegistryContextResolver.METRICS.getTimers().size());
		assertTrue(
			MetricRegistryContextResolver.METRICS.getTimers()
				.containsKey("GET - /myresource/test.resource-timer")
		);

		try {
			client.target(getContextBaseUrl() + "/myresource/test")
				.queryParam("command", "throw")
				.request()
				.get(String.class);
			fail("should have thrown MyException. ");
		} catch (WebApplicationException e) {
			assertEquals(2, MetricRegistryContextResolver.METRICS.getMeters().size());
			assertTrue(
				MetricRegistryContextResolver.METRICS.getMeters()
					.containsKey("GET - /myresource/test.resource-exception-meter")
			);
		}
	}

	@Path("/myresource")
	public static class MyResource {

		@Path("test")
		@GET
		@Metered(name = "resource-meter")
		@Timed(name = "resource-timer")
		@ExceptionMetered(name = "resource-exception-meter", cause = MyException.class)
		public String helloWorld(@QueryParam("command") String command) {
			if (!"throw".equals(command)) {
				return "hello world!";
			} else {
				throw new MyException();
			}
		}

	}

	public static class MyException extends RuntimeException {

	}
}
