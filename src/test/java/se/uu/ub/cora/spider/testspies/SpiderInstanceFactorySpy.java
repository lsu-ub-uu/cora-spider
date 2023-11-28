/*
 * Copyright 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.testspies;

import se.uu.ub.cora.spider.binary.Downloader;
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class SpiderInstanceFactorySpy implements SpiderInstanceFactory {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public SpiderInstanceFactorySpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getDependencyProviderClassName", String::new);
		MRV.setDefaultReturnValuesSupplier("factorRecordReader", RecordReaderSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorIncomingLinksReader",
				IncomingLinksReaderSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorRecordListReader", RecordListReaderSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorRecordCreator", RecordCreatorSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorRecordUpdater", RecordUpdaterSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorRecordDeleter", RecordDeleterSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorUploader", UploaderSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorDownloader", DownloaderSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorRecordSearcher", RecordSearcherSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorRecordValidator", RecordValidatorSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorRecordListIndexer", RecordListIndexerSpy::new);
	}

	@Override
	public String getDependencyProviderClassName() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordReader factorRecordReader() {
		return (RecordReader) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public IncomingLinksReader factorIncomingLinksReader() {
		return (IncomingLinksReader) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordListReader factorRecordListReader() {
		return (RecordListReader) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordCreator factorRecordCreator() {
		return (RecordCreator) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordUpdater factorRecordUpdater() {
		return (RecordUpdater) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordDeleter factorRecordDeleter() {
		return (RecordDeleter) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Uploader factorUploader() {
		return (Uploader) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Downloader factorDownloader() {
		return (Downloader) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordSearcher factorRecordSearcher() {
		return (RecordSearcher) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordValidator factorRecordValidator() {
		return (RecordValidator) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordListIndexer factorRecordListIndexer() {
		return (RecordListIndexer) MCR.addCallAndReturnFromMRV();
	}
}
