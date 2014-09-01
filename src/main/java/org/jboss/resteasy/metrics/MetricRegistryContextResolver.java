package org.jboss.resteasy.metrics;

import com.codahale.metrics.MetricRegistry;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class MetricRegistryContextResolver implements ContextResolver<MetricRegistry> {

	protected static MetricRegistry METRICS = new MetricRegistry();

	@Override
	public MetricRegistry getContext(Class<?> type) {
		if (MetricRegistry.class == type) {
			return METRICS;
		}
		return null;
	}
}
