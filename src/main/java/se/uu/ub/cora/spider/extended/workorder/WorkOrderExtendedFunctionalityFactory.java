/*
 * Copyright 2020 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.workorder;

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.internal.RecordDeleterImp;

public class WorkOrderExtendedFunctionalityFactory implements ExtendedFunctionalityFactory {

	private SpiderDependencyProvider dependencyProvider;
	private List<ExtendedFunctionalityContext> contexts = new ArrayList<>();

	@Override
	public void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		createListOfContexts();
	}

	private void createListOfContexts() {
		createContext(CREATE_AFTER_AUTHORIZATION);
		createContext(CREATE_AFTER_METADATA_VALIDATION);
		createContext(CREATE_BEFORE_ENHANCE);
	}

	private void createContext(ExtendedFunctionalityPosition position) {
		contexts.add(new ExtendedFunctionalityContext(position, "workOrder", 0));
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return contexts;
	}

	@Override
	public List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position,
			String recordType) {
		if (CREATE_AFTER_METADATA_VALIDATION == position) {
			return Collections.singletonList(factorExecutor());
		}
		if (CREATE_BEFORE_ENHANCE == position) {
			return Collections.singletonList(factorDeleter());
		}
		return Collections.singletonList(factorEnhancer());
	}

	private ExtendedFunctionality factorExecutor() {
		return WorkOrderExecutor.usingDependencyProvider(dependencyProvider);
	}

	private ExtendedFunctionality factorDeleter() {
		RecordDeleter recordDeleter = RecordDeleterImp.usingDependencyProvider(dependencyProvider);
		return WorkOrderDeleter.usingDeleter(recordDeleter);
	}

	private WorkOrderEnhancer factorEnhancer() {
		return new WorkOrderEnhancer();
	}

}
