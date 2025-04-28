/*
 * Copyright 2015 Uppsala University Library
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

import se.uu.ub.cora.bookkeeper.decorator.DataDecarator;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactorFactory;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandlerFactory;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorFactory;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.dependency.spy.DataRedactorFactorySpy;
import se.uu.ub.cora.spider.dependency.spy.DataValidatorFactoySpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerFactorySpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class SpiderDependencyProviderTestHelper extends DependencyProviderAbstract {

	public SpiderAuthorizator spiderAuthorizator;
	public PermissionRuleCalculator ruleCalculator;
	public Uploader uploader;
	public DataValidator dataValidator;
	public DataRecordLinkCollector linkCollector;
	public ExtendedFunctionalityProvider extendedFunctionalityProvider;
	public Authenticator authenticator;
	public RecordSearch recordSearch;
	public DataGroupTermCollector searchTermCollector;
	public RecordIndexer recordIndexer;
	public boolean readInitInfoWasCalled;
	public boolean tryToInitializeWasCalled;
	public DataRedactorFactorySpy dataRedactorFactorySpy = new DataRedactorFactorySpy();
	DataValidatorFactoySpy dataValidatorFactory = new DataValidatorFactoySpy();
	boolean standardDataValidatorFactory = false;
	public RecordTypeHandlerFactory recordTypeHandlerFactory = new RecordTypeHandlerFactorySpy();
	// public static Exception exceptionToThrow;

	public static MethodCallRecorder MCR = new MethodCallRecorder();
	public static MethodReturnValues MRV = new MethodReturnValues();

	public SpiderDependencyProviderTestHelper() {
		super();
	}

	@Override
	public Authenticator getAuthenticator() {
		return authenticator;
	}

	@Override
	public RecordSearch getRecordSearch() {
		return recordSearch;
	}

	@Override
	public RecordIndexer getRecordIndexer() {
		return recordIndexer;
	}

	@Override
	protected void tryToInitialize() {
		MCR.useMRV(MRV);
		tryToInitializeWasCalled = true;

		MCR.addCall();
		// if (initInfo.containsKey("runtimeException")) {
		// throw new RuntimeException(initInfo.get("runtimeException"));
		// }
		// if (initInfo.containsKey("invocationTargetException")) {
		// throw new InvocationTargetException(
		// new RuntimeException(initInfo.get("invocationTargetException")));
		// }
		// if (exceptionToThrow != null) {
		// throw exceptionToThrow;
		// }
	}

	@Override
	protected void readInitInfo() {
		// MCR.useMRV(MRV);
		readInitInfoWasCalled = true;
	}

	@Override
	DataValidatorFactory getDataValidatorFactory() {
		if (standardDataValidatorFactory) {
			return super.getDataValidatorFactory();
		}
		return dataValidatorFactory;
	}

	public DataRedactorFactory useOriginalGetDataRedactorFactory() {
		return super.createDataRedactorFactory();
	}

	@Override
	DataRedactorFactory createDataRedactorFactory() {
		return dataRedactorFactorySpy;
	}

	public RecordTypeHandlerFactory useOriginalGetRecordTypeHandlerFactory() {
		return super.createRecordTypeHandlerFactory();
	}

	@Override
	RecordTypeHandlerFactory createRecordTypeHandlerFactory() {
		return recordTypeHandlerFactory;
	}

	@Override
	public DataDecarator getDataDecorator() {
		// TODO Auto-generated method stub
		return null;
	}

}
