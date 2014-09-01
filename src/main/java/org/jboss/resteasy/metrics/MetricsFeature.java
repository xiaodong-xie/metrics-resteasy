package org.jboss.resteasy.metrics;

import static org.jboss.resteasy.metrics.Utils.chooseName;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class MetricsFeature implements DynamicFeature {

	private static final Logger LOG = LoggerFactory.getLogger(MetricsFeature.class);
	private static final String RESTEASY_METRICS_REQUEST_PROPERTY_KEY = "resteasy-metrics-request-property-key";

	@Context
	private Providers providers;

	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context) {

		MetricRegistry metrics =
			providers.getContextResolver(MetricRegistry.class, MediaType.WILDCARD_TYPE).getContext(MetricRegistry.class);

		Method resourceMethod = resourceInfo.getResourceMethod();
		if (resourceMethod.isAnnotationPresent(Timed.class)) {
			final Timed annotation = resourceMethod.getAnnotation(Timed.class);
			final String name = chooseName(
				annotation.name(),
				annotation.absolute(),
				resourceInfo.getResourceClass(),
				resourceInfo.getResourceMethod()
			);
			final Timer timer = metrics.timer(name);
			context.register(new TimedInterceptor(timer));
		}

		if (resourceMethod.isAnnotationPresent(Metered.class)) {
			final Metered annotation = resourceMethod.getAnnotation(Metered.class);
			final String name = chooseName(
				annotation.name(),
				annotation.absolute(),
				resourceInfo.getResourceClass(),
				resourceInfo.getResourceMethod()
			);
			final Meter meter = metrics.meter(name);
			context.register(new MeterInterceptor(meter));
		}
	}

	@Priority(500)
	static class TimedInterceptor implements ContainerRequestFilter, ContainerResponseFilter {

		private final Timer timer;

		public TimedInterceptor(Timer timer) {
			this.timer = timer;
		}

		@Override
		public void filter(ContainerRequestContext requestContext) throws IOException {
			try {
				Timer.Context context = timer.time();
				requestContext.setProperty(RESTEASY_METRICS_REQUEST_PROPERTY_KEY, context);
			} catch (Exception e) {
				LOG.info(
					"fail to time the request: {}, with error {}", requestContext.getUriInfo().getRequestUri(),
					e
				);
			}
		}

		@Override
		public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
			try {
				Timer.Context context = Timer.Context.class.cast(
					requestContext.getProperty(RESTEASY_METRICS_REQUEST_PROPERTY_KEY)
				);
				if (context != null) {
					context.stop();
				}
			} catch (Exception e) {
				LOG.info(
					"fail to time the request: {}, with error {}", requestContext.getUriInfo().getRequestUri(),
					e
				);
			}
		}
	}

	static class MeterInterceptor implements ContainerRequestFilter {

		private final Meter meter;

		public MeterInterceptor(Meter meter) {
			this.meter = meter;
		}

		@Override
		public void filter(ContainerRequestContext requestContext) throws IOException {
			try {
				meter.mark();
			} catch (Exception e) {
				LOG.info(
					"fail to mark the request: {}, with error {}", requestContext.getUriInfo().getRequestUri(),
					e
				);
			}
		}
	}
}