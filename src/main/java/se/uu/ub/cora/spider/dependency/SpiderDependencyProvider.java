/*
 * Copyright 2015, 2016, 2019 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.uu.ub.cora.spider.dependency;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollectorImp;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollectorImp;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorImp;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.BasePermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizatorImp;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.RecordSearch;
import se.uu.ub.cora.spider.role.RulesProviderImp;
import se.uu.ub.cora.spider.search.RecordIndexer;
import se.uu.ub.cora.storage.MetadataStorageProvider;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordIdGeneratorProvider;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.StreamStorageProvider;

public abstract class SpiderDependencyProvider {
	protected Map<String, String> initInfo;
	protected RecordStorageProvider recordStorageProvider;
	protected StreamStorageProvider streamStorageProvider;
	protected RecordIdGeneratorProvider recordIdGeneratorProvider;
	protected MetadataStorageProvider metadataStorageProvider;
	private Logger log = LoggerProvider.getLoggerForClass(SpiderDependencyProvider.class);

	public SpiderDependencyProvider(Map<String, String> initInfo) {
		this.initInfo = initInfo;
		readInitInfo();
		try {
			tryToInitialize();
		} catch (InvocationTargetException e) {
			throw new RuntimeException(createInvocationErrorExceptionMessage(e));
		} catch (Exception e) {
			throw new RuntimeException(createExceptionMessage(e));
		}
	}

	private String createInvocationErrorExceptionMessage(InvocationTargetException e) {
		return "Error starting " + getImplementingClassName() + ": "
				+ e.getTargetException().getMessage();
	}

	private String createExceptionMessage(Exception e) {
		return "Error starting " + getImplementingClassName() + ": " + e.getMessage();
	}

	private String getImplementingClassName() {
		return this.getClass().getSimpleName();
	}

	public final RecordStorage getRecordStorage() {
		return recordStorageProvider.getRecordStorage();
	}

	public final void setRecordStorageProvider(RecordStorageProvider recordStorageProvider) {
		this.recordStorageProvider = recordStorageProvider;
	}

	public final StreamStorage getStreamStorage() {
		return streamStorageProvider.getStreamStorage();
	}

	public final void setStreamStorageProvider(StreamStorageProvider streamStorageProvider) {
		this.streamStorageProvider = streamStorageProvider;
	}

	public final void setRecordIdGeneratorProvider(
			RecordIdGeneratorProvider recordIdGeneratorProvider) {
		this.recordIdGeneratorProvider = recordIdGeneratorProvider;
	}

	public final RecordIdGenerator getRecordIdGenerator() {
		return recordIdGeneratorProvider.getRecordIdGenerator();
	}

	public void setMetadataStorageProvider(MetadataStorageProvider metadataStorageProvider) {
		this.metadataStorageProvider = metadataStorageProvider;
	}

	public SpiderAuthorizator getSpiderAuthorizator() {
		return SpiderAuthorizatorImp.usingSpiderDependencyProviderAndAuthorizatorAndRulesProvider(
				this, new AuthorizatorImp(), new RulesProviderImp(getRecordStorage()));
	}

	public DataValidator getDataValidator() {
		return new DataValidatorImp(metadataStorageProvider.getMetadataStorage());
	}

	public DataRecordLinkCollector getDataRecordLinkCollector() {
		return new DataRecordLinkCollectorImp(metadataStorageProvider.getMetadataStorage());
	}

	public DataGroupTermCollector getDataGroupTermCollector() {
		return new DataGroupTermCollectorImp(metadataStorageProvider.getMetadataStorage());
	}

	public PermissionRuleCalculator getPermissionRuleCalculator() {
		return new BasePermissionRuleCalculator();
	}

	protected void ensureKeyExistsInInitInfo(String key) {
		if (keyNotFoundInInitInfo(key)) {
			String message = createErrorMessage(key);
			log.logFatalUsingMessage(message);
			throw new SpiderInitializationException(message);
		}
	}

	private String createErrorMessage(String key) {
		String simpleName = this.getClass().getSimpleName();
		return "InitInfo in " + simpleName + " must contain: " + key;
	}

	private boolean keyNotFoundInInitInfo(String key) {
		return !initInfo.containsKey(key);
	}

	protected abstract void tryToInitialize() throws Exception;

	protected abstract void readInitInfo();

	public abstract ExtendedFunctionalityProvider getExtendedFunctionalityProvider();

	public abstract Authenticator getAuthenticator();

	public abstract RecordSearch getRecordSearch();

	public abstract RecordIndexer getRecordIndexer();

}
