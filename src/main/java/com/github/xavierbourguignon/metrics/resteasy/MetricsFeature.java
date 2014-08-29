package com.github.xavierbourguignon.metrics.resteasy;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class MetricsFeature implements DynamicFeature {

	private static final Logger LOG = LoggerFactory.getLogger(MetricsFeature.class);
	private static final String RESTEASY_METRICS_REQUEST_PROPERTY_KEY = "resteasy-metrics-request-property-key";
	private static final MetricRegistry METRICS = new MetricRegistry();

	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context) {
		Method resourceMethod = resourceInfo.getResourceMethod();
		if (resourceMethod.isAnnotationPresent(Timed.class)) {
			final Timed annotation = resourceMethod.getAnnotation(Timed.class);
			final String name = chooseName(annotation.name(), annotation.absolute(), resourceInfo);
			final Timer timer = METRICS.timer(name);
			context.register(new TimedInterceptor(timer));
		}

		if (resourceMethod.isAnnotationPresent(Metered.class)) {
			final Metered annotation = resourceMethod.getAnnotation(Metered.class);
			final String name = chooseName(annotation.name(), annotation.absolute(), resourceInfo);
			final Meter meter = METRICS.meter(name);
			context.register(new MeterInterceptor(meter));
		}
	}

	private String chooseName(String explicitName, boolean absolute, ResourceInfo resourceInfo) {
		if (explicitName != null && !explicitName.isEmpty()) {
			if (absolute) {
				return explicitName;
			}
			return name(getName(resourceInfo), explicitName);
		}
		return getName(resourceInfo);
	}

	private String getName(ResourceInfo resourceInfo) {
		return getMethod(resourceInfo.getResourceMethod()) + " - " + getPath(resourceInfo);
	}

	private String getPath(ResourceInfo resourceInfo) {
		String rootPath = null;
		String methodPath = null;

		if (resourceInfo.getResourceClass().isAnnotationPresent(Path.class)) {
			rootPath = resourceInfo.getResourceClass().getAnnotation(Path.class).value();
		}

		if (resourceInfo.getResourceMethod().isAnnotationPresent(Path.class)) {
			methodPath = resourceInfo.getResourceMethod().getAnnotation(Path.class).value();
		}

		return Joiner.on("/").skipNulls().join(rootPath, methodPath);
	}

	private String getMethod(Method resourceMethod) {
		if (resourceMethod.isAnnotationPresent(GET.class)) {
			return HttpMethod.GET;
		}
		if (resourceMethod.isAnnotationPresent(POST.class)) {
			return HttpMethod.POST;
		}
		if (resourceMethod.isAnnotationPresent(PUT.class)) {
			return HttpMethod.PUT;
		}
		if (resourceMethod.isAnnotationPresent(DELETE.class)) {
			return HttpMethod.DELETE;
		}
		if (resourceMethod.isAnnotationPresent(HEAD.class)) {
			return HttpMethod.HEAD;
		}
		if (resourceMethod.isAnnotationPresent(OPTIONS.class)) {
			return HttpMethod.OPTIONS;
		}

		throw new IllegalStateException("Resource method without GET, POST, PUT, DELETE, HEAD or OPTIONS annotation");
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