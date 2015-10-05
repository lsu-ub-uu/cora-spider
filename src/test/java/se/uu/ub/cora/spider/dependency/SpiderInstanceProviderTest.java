package se.uu.ub.cora.spider.dependency;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.testng.Assert;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.SpiderRecordCreator;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.SpiderRecordReader;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;

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
	public void initMakeSureWeGetMultipleInstancesOfRecordReader() {
		SpiderDependencyProvider spiderDependencyProvider = new SpiderDependencyProviderSpy();
		SpiderInstanceProvider.setSpiderDependencyProvider(spiderDependencyProvider);
		SpiderRecordReader recordReader = SpiderInstanceProvider.getSpiderRecordReader();
		SpiderRecordReader recordReader2 = SpiderInstanceProvider.getSpiderRecordReader();
		assertNotNull(recordReader);
		assertNotNull(recordReader2);
		assertNotSame(recordReader, recordReader2);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfRecordCreator() {
		SpiderDependencyProvider spiderDependencyProvider = new SpiderDependencyProviderSpy();
		SpiderInstanceProvider.setSpiderDependencyProvider(spiderDependencyProvider);
		SpiderRecordCreator recordCreator = SpiderInstanceProvider.getSpiderRecordCreator();
		SpiderRecordCreator recordCreator2 = SpiderInstanceProvider.getSpiderRecordCreator();
		assertNotNull(recordCreator);
		assertNotNull(recordCreator2);
		assertNotSame(recordCreator, recordCreator2);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfRecordUpdater() {
		SpiderDependencyProvider spiderDependencyProvider = new SpiderDependencyProviderSpy();
		SpiderInstanceProvider.setSpiderDependencyProvider(spiderDependencyProvider);
		SpiderRecordUpdater recordUpdater = SpiderInstanceProvider.getSpiderRecordUpdater();
		SpiderRecordUpdater recordUpdater2 = SpiderInstanceProvider.getSpiderRecordUpdater();
		assertNotNull(recordUpdater);
		assertNotNull(recordUpdater2);
		assertNotSame(recordUpdater, recordUpdater2);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfRecordDeleter() {
		SpiderDependencyProvider spiderDependencyProvider = new SpiderDependencyProviderSpy();
		SpiderInstanceProvider.setSpiderDependencyProvider(spiderDependencyProvider);
		SpiderRecordDeleter recordDeleter = SpiderInstanceProvider.getSpiderRecordDeleter();
		SpiderRecordDeleter recordDeleter2 = SpiderInstanceProvider.getSpiderRecordDeleter();
		assertNotNull(recordDeleter);
		assertNotNull(recordDeleter2);
		assertNotSame(recordDeleter, recordDeleter2);
	}

}
