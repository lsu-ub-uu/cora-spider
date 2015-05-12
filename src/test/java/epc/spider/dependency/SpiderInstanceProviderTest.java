package epc.spider.dependency;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.spider.record.SpiderRecordHandler;

public class SpiderInstanceProviderTest {
	@Test
	public void testPrivateConstructor() throws Exception {
		Constructor<SpiderInstanceProvider> constructor = SpiderInstanceProvider.class
				.getDeclaredConstructor();
		Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
	}

	@Test(expectedExceptions = InvocationTargetException.class)
	public void testPrivateConstructorInvoke() throws Exception {
		Constructor<SpiderInstanceProvider> constructor = SpiderInstanceProvider.class
				.getDeclaredConstructor();
		Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfRecordHandler() {
		SpiderDependencyProvider spiderDependencyProvider = new SpiderDependencyProviderSpy();
		SpiderInstanceProvider.setSpiderDependencyProvider(spiderDependencyProvider);
		SpiderRecordHandler recordHandler = SpiderInstanceProvider.getSpiderRecordHandler();
		SpiderRecordHandler recordHandler2 = SpiderInstanceProvider.getSpiderRecordHandler();
		assertNotNull(recordHandler);
		assertNotNull(recordHandler2);
		assertNotSame(recordHandler, recordHandler2);
	}
}
