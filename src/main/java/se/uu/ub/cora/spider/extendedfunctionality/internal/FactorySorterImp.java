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
package se.uu.ub.cora.spider.extendedfunctionality.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class FactorySorterImp implements FactorySorter {
	private Logger log = LoggerProvider.getLoggerForClass(FactorySorterImp.class);
	private Iterable<ExtendedFunctionalityFactory> extendedFunctionalityFactories;
	private Map<ExtendedFunctionalityPosition, Map<String, List<FactoryRunBy>>> factories = new EnumMap<>(
			ExtendedFunctionalityPosition.class);
	private SpiderDependencyProvider dependencyProvider;

	public FactorySorterImp(SpiderDependencyProvider dependencyProvider,
			Iterable<ExtendedFunctionalityFactory> extendedFunctionalityFactories) {
		this.dependencyProvider = dependencyProvider;
		this.extendedFunctionalityFactories = extendedFunctionalityFactories;
		logStartupMessage();
		sortFactories();
	}

	private void logStartupMessage() {
		log.logInfoUsingMessage("Extended functionality found and sorted as follows:");
	}

	private void sortFactories() {
		initializeFactoriesAndSortIntoMaps();
		orderListsInFactories();
	}

	private void initializeFactoriesAndSortIntoMaps() {
		for (ExtendedFunctionalityFactory extendedFactory : extendedFunctionalityFactories) {
			extendedFactory.initializeUsingDependencyProvider(dependencyProvider);
			sortIntoMapForEachFactory(extendedFactory);
		}
	}

	private void sortIntoMapForEachFactory(ExtendedFunctionalityFactory extendedFactory) {
		List<ExtendedFunctionalityContext> efcs = extendedFactory
				.getExtendedFunctionalityContexts();
		for (ExtendedFunctionalityContext efc : efcs) {
			sortByContext(extendedFactory, efc);
		}
	}

	private void sortByContext(ExtendedFunctionalityFactory extendedFactory,
			ExtendedFunctionalityContext efc) {
		ExtendedFunctionalityPosition position = efc.extendedFunctionalityPosition;
		ensureContextHolderExists(efc, position);
		factories.get(position).get(efc.recordType)
				.add(new FactoryRunBy(extendedFactory, efc.runAsNumber));
	}

	private void ensureContextHolderExists(ExtendedFunctionalityContext efc,
			ExtendedFunctionalityPosition position) {
		ensureContextPositionExists(position);
		ensureContextRecordTypeExists(efc, position);
	}

	private void ensureContextPositionExists(ExtendedFunctionalityPosition position) {
		if (!factories.containsKey(position)) {
			factories.put(position, new HashMap<>());
		}
	}

	private void ensureContextRecordTypeExists(ExtendedFunctionalityContext efc,
			ExtendedFunctionalityPosition position) {
		Map<String, List<FactoryRunBy>> positionMap = factories.get(position);
		if (!positionMap.containsKey(efc.recordType)) {
			positionMap.put(efc.recordType, new ArrayList<>());
		}
	}

	private void orderListsInFactories() {
		for (Entry<ExtendedFunctionalityPosition, Map<String, List<FactoryRunBy>>> entry : factories
				.entrySet()) {
			Map<String, List<FactoryRunBy>> recordTypeMap = entry.getValue();
			logPosition(entry);
			orderListForRecordType(recordTypeMap);
		}
	}

	private void logPosition(
			Entry<ExtendedFunctionalityPosition, Map<String, List<FactoryRunBy>>> entry) {
		log.logInfoUsingMessage(" position: " + entry.getKey());
	}

	private void orderListForRecordType(Map<String, List<FactoryRunBy>> recordTypeMap) {
		for (Entry<String, List<FactoryRunBy>> entry : recordTypeMap.entrySet()) {
			logRecordType(entry);
			sortListInEntry(entry);
			logFactories(entry);
		}
	}

	private void logRecordType(Entry<String, List<FactoryRunBy>> entry) {
		log.logInfoUsingMessage("  recordType: " + entry.getKey());
	}

	private void sortListInEntry(Entry<String, List<FactoryRunBy>> entry) {
		List<FactoryRunBy> unsortedFactoryList = entry.getValue();
		List<FactoryRunBy> sortedFactoryList = unsortedFactoryList.stream()
				.sorted((o1, o2) -> o1.runAsNumber.compareTo(o2.runAsNumber))
				.collect(Collectors.toList());
		entry.setValue(sortedFactoryList);
	}

	private void logFactories(Entry<String, List<FactoryRunBy>> entry) {
		for (FactoryRunBy factoryRunBy : entry.getValue()) {
			log.logInfoUsingMessage("   class: " + factoryRunBy.factory.getClass().getName());
		}
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForPositionAndRecordType(
			ExtendedFunctionalityPosition position, String recordType) {
		if (factoryExistsForPositionAndRecordType(position, recordType)) {
			return addFunctionalityForPositionAndRecordType(position, recordType);
		}
		return Collections.emptyList();
	}

	private List<ExtendedFunctionality> addFunctionalityForPositionAndRecordType(
			ExtendedFunctionalityPosition position, String recordType) {
		List<FactoryRunBy> currentFactories = factories.get(position).get(recordType);
		List<ExtendedFunctionality> functionalities = new ArrayList<>();
		for (FactoryRunBy extendedFactory : currentFactories) {
			functionalities.add(extendedFactory.factory.factor(position, recordType));
		}
		return functionalities;
	}

	private boolean factoryExistsForPositionAndRecordType(ExtendedFunctionalityPosition position,
			String recordType) {
		return factories.containsKey(position) && factories.get(position).containsKey(recordType);
	}

	class FactoryRunBy {
		private ExtendedFunctionalityFactory factory;
		private Integer runAsNumber;

		public FactoryRunBy(ExtendedFunctionalityFactory factory, int runAsNumber) {
			this.factory = factory;
			this.runAsNumber = runAsNumber;
		}
	}

	Iterable<ExtendedFunctionalityFactory> getExtendedFunctionalityFactoriesNeededForTest() {
		return extendedFunctionalityFactories;
	}

	public SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}
}
