/*
 * Copyright 2015, 2016 Uppsala University Library
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

package se.uu.ub.cora.spider.dependency;

import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerImp;
import se.uu.ub.cora.spider.record.SpiderDownloader;
import se.uu.ub.cora.spider.record.SpiderDownloaderImp;
import se.uu.ub.cora.spider.record.SpiderRecordCreator;
import se.uu.ub.cora.spider.record.SpiderRecordCreatorImp;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.SpiderRecordDeleterImp;
import se.uu.ub.cora.spider.record.SpiderRecordListReader;
import se.uu.ub.cora.spider.record.SpiderRecordListReaderImp;
import se.uu.ub.cora.spider.record.SpiderRecordReader;
import se.uu.ub.cora.spider.record.SpiderRecordReaderImp;
import se.uu.ub.cora.spider.record.SpiderRecordSearcher;
import se.uu.ub.cora.spider.record.SpiderRecordSearcherImp;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterImp;
import se.uu.ub.cora.spider.record.SpiderUploader;
import se.uu.ub.cora.spider.record.SpiderUploaderImp;

public final class SpiderInstanceFactoryImp implements SpiderInstanceFactory {

	private SpiderDependencyProvider dependencyProvider;

	private SpiderInstanceFactoryImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	public static SpiderInstanceFactory usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderInstanceFactoryImp(dependencyProvider);
	}

	@Override
	public SpiderRecordReader factorSpiderRecordReader() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return SpiderRecordReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderRecordListReader factorSpiderRecordListReader() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return SpiderRecordListReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderRecordCreator factorSpiderRecordCreator() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return SpiderRecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderRecordUpdater factorSpiderRecordUpdater() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return SpiderRecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderRecordDeleter factorSpiderRecordDeleter() {
		return SpiderRecordDeleterImp.usingDependencyProvider(dependencyProvider);
	}

	@Override
	public SpiderUploader factorSpiderUploader() {
		return SpiderUploaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Override
	public SpiderDownloader factorSpiderDownloader() {
		return SpiderDownloaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Override
	public SpiderRecordSearcher factorSpiderRecordSearcher() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return SpiderRecordSearcherImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

}
