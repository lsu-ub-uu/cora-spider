/*
 * Copyright 2026 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.batchjob;

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_STORE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.index.BatchRunnerFactory;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.BatchRunnerFactoryImp;
import se.uu.ub.cora.spider.index.internal.IndexBatchHandlerImp;

public class BatchJobExtendedFunctionalityFactory implements ExtendedFunctionalityFactory {

	private List<ExtendedFunctionalityContext> contexts = new ArrayList<>();
	private SpiderDependencyProvider dependencyProvider;

	@Override
	public void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		createListOfContexts();
	}

	private void createListOfContexts() {
		createContext(CREATE_AFTER_METADATA_VALIDATION);
		createContext(CREATE_BEFORE_STORE);
		createContext(CREATE_BEFORE_ENHANCE);
	}

	private void createContext(ExtendedFunctionalityPosition position) {
		contexts.add(new ExtendedFunctionalityContext(position, "indexBatchJob", 0));
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return contexts;
	}

	@Override
	public List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position,
			String recordType) {
		if (CREATE_AFTER_METADATA_VALIDATION.equals(position)) {
			return createBatchJobCheckAuthorized();
		}
		if (CREATE_BEFORE_STORE.equals(position)) {
			return createCalculateNumberToIndex();
		}
		return createRunBatchJob();
	}

	private List<ExtendedFunctionality> createBatchJobCheckAuthorized() {
		return Collections.singletonList(
				new BatchJobCheckAuthorizedExtendedFunctionality(dependencyProvider));
	}

	private List<ExtendedFunctionality> createCalculateNumberToIndex() {
		return Collections.singletonList(
				new CalculateNumberToIndexExtendedFunctionality(dependencyProvider));
	}

	private List<ExtendedFunctionality> createRunBatchJob() {
		BatchRunnerFactory batchRunnerFactory = new BatchRunnerFactoryImp(dependencyProvider);
		IndexBatchHandler indexBatchHandler = IndexBatchHandlerImp
				.usingBatchRunnerFactory(batchRunnerFactory);
		return Collections.singletonList(
				new RunBatchJobExtendedFunctionality(dependencyProvider, indexBatchHandler));
	}

}
