/*
 * Copyright 2016 Uppsala University Library
 * Copyright 2016 Olov McKie
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

package se.uu.ub.cora.spider.extended;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.spider.apptoken.AppTokenEnhancer;
import se.uu.ub.cora.spider.apptoken.UserUpdaterForAppToken;
import se.uu.ub.cora.spider.consistency.MetadataConsistencyValidatorFactory;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.workorder.WorkOrderDeleter;
import se.uu.ub.cora.spider.workorder.WorkOrderEnhancer;
import se.uu.ub.cora.spider.workorder.WorkOrderExecutor;

public class BaseExtendedFunctionalityProvider implements ExtendedFunctionalityProvider {

	private static final String WORK_ORDER = "workOrder";
	protected SpiderDependencyProvider dependencyProvider;

	public BaseExtendedFunctionalityProvider(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeMetadataValidation(
			String recordType) {
		List<ExtendedFunctionality> list = new ArrayList<>();
		if ("appToken".equals(recordType)) {
			list.add(new AppTokenEnhancer());
		}
		if (WORK_ORDER.equals(recordType)) {
			list.add(new WorkOrderEnhancer());
		}
		return list;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateAfterMetadataValidation(
			String recordType) {
		List<ExtendedFunctionality> list = new ArrayList<>();
		if ("metadataGroup".equals(recordType) || "metadataCollectionVariable".equals(recordType)) {
			addConsistencyValidatorToListUsingRecordType(list, recordType);
		}
		if (WORK_ORDER.equals(recordType)) {
			list.add(WorkOrderExecutor
					.usingDependencyProvider(dependencyProvider));
		}
		return list;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeReturn(String recordType) {
		List<ExtendedFunctionality> list = new ArrayList<>();
		if ("appToken".equals(recordType)) {
			list.add(UserUpdaterForAppToken
					.usingSpiderDependencyProvider(dependencyProvider));
		}
		if (WORK_ORDER.equals(recordType)) {
			addDeleteForWorkOrder(list);
		}
		return list;
	}

	private void addDeleteForWorkOrder(List<ExtendedFunctionality> list) {
		SpiderInstanceFactory spiderInstanceFactory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderRecordDeleter spiderRecordDeleter = spiderInstanceFactory.factorSpiderRecordDeleter();
		list.add(WorkOrderDeleter.usingDeleter(spiderRecordDeleter));
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeMetadataValidation(
			String recordType) {
		return Collections.emptyList();
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateAfterMetadataValidation(
			String recordType) {
		List<ExtendedFunctionality> list = new ArrayList<>();
		if ("metadataGroup".equals(recordType) || "metadataCollectionVariable".equals(recordType)) {
			addConsistencyValidatorToListUsingRecordType(list, recordType);
		}
		return list;
	}

	private void addConsistencyValidatorToListUsingRecordType(List<ExtendedFunctionality> list,
			String recordType) {
		MetadataConsistencyValidatorFactory factory = MetadataConsistencyValidatorFactory
				.usingRecordStorage(dependencyProvider.getRecordStorage());
		list.add(MetadataConsistencyValidatorAsExtendedFunctionality
				.usingValidator(factory.factor(recordType)));
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityBeforeDelete(String recordType) {
		return Collections.emptyList();
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityAfterDelete(String recordType) {
		return Collections.emptyList();
	}

}
