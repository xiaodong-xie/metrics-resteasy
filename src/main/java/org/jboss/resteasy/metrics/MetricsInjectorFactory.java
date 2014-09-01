package org.jboss.resteasy.metrics;

import static org.jboss.resteasy.metrics.Utils.chooseName;
import static org.jboss.resteasy.metrics.Utils.getRootCause;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import java.lang.reflect.Method;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.core.MethodInjectorImpl;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.MethodInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceLocator;

public class MetricsInjectorFactory extends CdiInjectorFactory {

	@Override
	public MethodInjector createMethodInjector(ResourceLocator method, ResteasyProviderFactory factory) {
		if (method.getAnnotatedMethod().isAnnotationPresent(ExceptionMetered.class)) {
			MethodInjector mi = super.createMethodInjector(method, factory);
			return new MetricsMethodInjector(mi, method, factory);
		} else {
			return super.createMethodInjector(method, factory);
		}
	}

	private class MetricsMethodInjector extends MethodInjectorImpl {

		private final MethodInjector originalMethodInjector;

		public MetricsMethodInjector(
			MethodInjector originalMethodInjector,
			ResourceLocator resourceMethod,
			ResteasyProviderFactory factory
		) {
			super(resourceMethod, factory);
			this.originalMethodInjector = originalMethodInjector;
		}

		@Override
		public Object invoke(
			HttpRequest request, HttpResponse httpResponse, Object resource
		) throws Failure, ApplicationException {
			MetricRegistry metrics = factory.getContextResolver(MetricRegistry.class, MediaType.WILDCARD_TYPE)
					.getContext(MetricRegistry.class);
			Method resourceMethod = method.getAnnotatedMethod();
			final ExceptionMetered annotation = resourceMethod.getAnnotation(ExceptionMetered.class);
			final String name = chooseName(
				annotation.name(),
				annotation.absolute(),
				method.getResourceClass().getClazz(),
				method.getAnnotatedMethod()
			);
			Class<? extends Throwable> cause = annotation.cause();
			try {
				return super.invoke(request, httpResponse, resource);
			} catch (Throwable t) {
				if (cause.isAssignableFrom(getRootCause(t).getClass())) {
					metrics.meter(name);
				}
				throw t;
			}
		}
	}

}
