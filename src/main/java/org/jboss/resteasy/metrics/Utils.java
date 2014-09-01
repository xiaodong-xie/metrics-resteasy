package org.jboss.resteasy.metrics;

import static com.codahale.metrics.MetricRegistry.name;

import com.google.common.base.Joiner;
import java.lang.reflect.Method;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

public final class Utils {
	private Utils() {

	}

	public static String chooseName(
		String explicitName,
		boolean absolute,
		Class<?> resourceClass,
		Method resourceMethod
	) {
		if (explicitName != null && !explicitName.isEmpty()) {
			if (absolute) {
				return explicitName;
			}
			return name(getName(resourceClass, resourceMethod), explicitName);
		}
		return getName(resourceClass, resourceMethod);
	}

	private static String getName(
		Class<?> resourceClass,
		Method resourceMethod
	) {
		return getMethod(resourceMethod) + " - " + getPath(resourceClass, resourceMethod);
	}

	private static String getPath(
		Class<?> resourceClass,
		Method resourceMethod
	) {
		String rootPath = null;
		String methodPath = null;

		if (resourceClass.isAnnotationPresent(Path.class)) {
			rootPath = resourceClass.getAnnotation(Path.class).value();
		}

		if (resourceMethod.isAnnotationPresent(Path.class)) {
			methodPath = resourceMethod.getAnnotation(Path.class).value();
		}

		return Joiner.on("/").skipNulls().join(rootPath, methodPath);
	}

	private static String getMethod(Method resourceMethod) {
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

	public static Throwable getRootCause(Throwable t) {
		Throwable rootCause = t;
		while(rootCause.getCause()!=null) {
			rootCause = rootCause.getCause();
		}
		System.out.println("root cause is : " + rootCause.getClass());
		return rootCause;
	}
}
