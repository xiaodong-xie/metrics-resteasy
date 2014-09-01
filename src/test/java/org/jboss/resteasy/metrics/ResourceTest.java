package org.jboss.resteasy.metrics;

import io.undertow.servlet.api.DeploymentInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.test.TestPortProvider;
import org.jboss.weld.environment.se.Weld;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class ResourceTest {

	private static UndertowJaxrsServer server;

	private App app = new App();

	private Client client = ClientBuilder.newClient();

	private Weld weld;

	@BeforeClass
	public static void init() throws Exception {
		server = new UndertowJaxrsServer().start();
	}

	@AfterClass
	public static void stop() throws Exception {
		server.stop();
	}

	@Before
	public void before() throws Exception {
		setUpResources();
		ResteasyDeployment deployment = new ResteasyDeployment();
		deployment.getDefaultContextObjects().putAll(app.contextObjbects);
		deployment.setApplication(app);
		deployment.setInjectorFactoryClass("org.jboss.resteasy.metrics.MetricsInjectorFactory");
		List<Class> providerClasses = Arrays.asList((Class)MetricsFeature.class, MetricRegistryContextResolver.class);
		deployment.setActualProviderClasses(providerClasses);

		weld = new Weld();
		weld.initialize();

		DeploymentInfo di = server.undertowDeployment(deployment);
		di.setClassLoader(app.getClass().getClassLoader());
		di.setContextPath("/resources");
		di.setDeploymentName("Resteasy/resources");
		server.deploy(di);

	}

	@After
	public void after() {
		client.close();
		weld.shutdown();
	}

	protected void addResource(Object resource) {
		app.singletons.add(resource);
	}

	protected void addProvider(Object provider) {
		app.singletons.add(provider);
	}

	protected void addContextInstance(
		Class<?> contextInstanceClass,
		Object contextInstance
	) {
		app.contextObjbects.put(contextInstanceClass, contextInstance);
	}

	protected Client getClient() {
		return client;
	}

	protected String getContextBaseUrl() {
		return TestPortProvider.generateURL("/resources");
	}

	protected abstract void setUpResources() throws Exception;

	@ApplicationPath("/resources")
	public static class App extends Application {

		private final Set<Object> singletons = new HashSet<>();

		private final Map<Class<?>, Object> contextObjbects = new HashMap<>();

		@Override
		public Set<Object> getSingletons() {
			return singletons;
		}
	}
}
