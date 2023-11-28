/*
 * Copyright 2015, 2016, 2019, 2021 Uppsala University Library
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

import se.uu.ub.cora.spider.binary.Downloader;
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.binary.internal.DownloaderImp;
import se.uu.ub.cora.spider.binary.internal.MimeTypeToBinaryType;
import se.uu.ub.cora.spider.binary.internal.MimeTypeToBinaryTypeImp;
import se.uu.ub.cora.spider.binary.internal.UploaderImp;
import se.uu.ub.cora.spider.index.BatchRunnerFactory;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.BatchRunnerFactoryImp;
import se.uu.ub.cora.spider.index.internal.DataGroupHandlerForIndexBatchJob;
import se.uu.ub.cora.spider.index.internal.DataGroupHandlerForIndexBatchJobImp;
import se.uu.ub.cora.spider.index.internal.IndexBatchHandlerImp;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.record.internal.DataGroupToRecordEnhancerImp;
import se.uu.ub.cora.spider.record.internal.IncomingLinksReaderImp;
import se.uu.ub.cora.spider.record.internal.RecordCreatorImp;
import se.uu.ub.cora.spider.record.internal.RecordDeleterImp;
import se.uu.ub.cora.spider.record.internal.RecordListIndexerImp;
import se.uu.ub.cora.spider.record.internal.RecordListReaderImp;
import se.uu.ub.cora.spider.record.internal.RecordReaderImp;
import se.uu.ub.cora.spider.record.internal.RecordSearcherImp;
import se.uu.ub.cora.spider.record.internal.RecordUpdaterImp;
import se.uu.ub.cora.spider.record.internal.RecordValidatorImp;
import se.uu.ub.cora.spider.resourceconvert.ResourceConvert;
import se.uu.ub.cora.spider.resourceconvert.ResourceConvertImp;

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
	public RecordReader factorRecordReader() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return RecordReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public RecordListReader factorRecordListReader() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return RecordListReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public RecordCreator factorRecordCreator() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public RecordUpdater factorRecordUpdater() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return RecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public RecordDeleter factorRecordDeleter() {
		return RecordDeleterImp.usingDependencyProvider(dependencyProvider);
	}

	@Override
	public Uploader factorUploader() {

		String hostName = dependencyProvider.getInitInfoValueUsingKey("rabbitMqHostname");
		int port = Integer.parseInt(dependencyProvider.getInitInfoValueUsingKey("rabbitMqPort"));
		String vHost = dependencyProvider.getInitInfoValueUsingKey("rabbitMqVirtualHost");
		String exchange = dependencyProvider.getInitInfoValueUsingKey("rabbitMqExchange");
		String routingKey = dependencyProvider.getInitInfoValueUsingKey("rabbitMqRoutingKey");

		ResourceConvert recsourceConvert = ResourceConvertImp
				.usingHostnamePortVHostExchangeRoutingKey(hostName, port, vHost, exchange,
						routingKey);
		MimeTypeToBinaryType mimeTypeToBinaryType = new MimeTypeToBinaryTypeImp();
		return UploaderImp.usingDependencyProviderAndResourceConvertAndMimeTypeToBinaryType(
				dependencyProvider, recsourceConvert, mimeTypeToBinaryType);
	}

	@Override
	public Downloader factorDownloader() {
		return DownloaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Override
	public RecordSearcher factorRecordSearcher() {
		DataGroupToRecordEnhancer dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerImp(
				dependencyProvider);
		return RecordSearcherImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public IncomingLinksReader factorIncomingLinksReader() {
		return IncomingLinksReaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Override
	public RecordValidator factorRecordValidator() {
		return RecordValidatorImp.usingDependencyProvider(dependencyProvider);
	}

	@Override
	public String getDependencyProviderClassName() {
		return dependencyProvider.getClass().getName();
	}

	@Override
	public RecordListIndexer factorRecordListIndexer() {
		BatchRunnerFactory batchRunnerFactory = new BatchRunnerFactoryImp(dependencyProvider);
		IndexBatchHandler indexBatchHandler = IndexBatchHandlerImp
				.usingBatchRunnerFactory(batchRunnerFactory);
		DataGroupHandlerForIndexBatchJob dataGroupHandlerForIndexBatchJob = new DataGroupHandlerForIndexBatchJobImp();
		return RecordListIndexerImp.usingDependencyProvider(dependencyProvider, indexBatchHandler,
				dataGroupHandlerForIndexBatchJob);
	}

}
