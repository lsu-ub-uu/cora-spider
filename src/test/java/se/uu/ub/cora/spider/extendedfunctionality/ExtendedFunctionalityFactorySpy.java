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
package se.uu.ub.cora.spider.extendedfunctionality;

import java.util.List;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;

public class ExtendedFunctionalityFactorySpy implements ExtendedFunctionalityFactory {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public List<ExtendedFunctionalityContext> extendedFunctionalityContexts;

	public ExtendedFunctionalityFactorySpy(
			List<ExtendedFunctionalityContext> extendedFunctionalityContexts) {
		this.extendedFunctionalityContexts = extendedFunctionalityContexts;
	}

	@Override
	public void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		MCR.addCall("dependencyProvider", dependencyProvider);
	}

	@Override
	public ExtendedFunctionality factor(
			ExtendedFunctionalityPosition createBeforeMetadataValidation, String recordType) {
		MCR.addCall("createBeforeMetadataValidation", createBeforeMetadataValidation, "recordType",
				recordType);
		ExtendedFunctionality returnedFunctionality = new ExtendedFunctionalitySpy();
		MCR.addReturned(returnedFunctionality);
		return returnedFunctionality;
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return extendedFunctionalityContexts;
	}

}
