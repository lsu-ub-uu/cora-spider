/*
 * Copyright 2022, 2023, 2025 Uppsala University Library
 * Copyright 2025 Olov McKie
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
package se.uu.ub.cora.spider.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import se.uu.ub.cora.bookkeeper.metadata.CollectTermHolder;
import se.uu.ub.cora.bookkeeper.metadata.MetadataElement;
import se.uu.ub.cora.bookkeeper.storage.MetadataStorageView;
import se.uu.ub.cora.bookkeeper.text.TextElement;
import se.uu.ub.cora.bookkeeper.validator.ValidationType;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.spy.TextElementSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class MetadataStorageViewSpy implements MetadataStorageView {
	@SuppressWarnings("exports")
	public MethodCallRecorder MCR = new MethodCallRecorder();
	@SuppressWarnings("exports")
	public MethodReturnValues MRV = new MethodReturnValues();

	public MetadataStorageViewSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getMetadataElements", ArrayList<DataRecordGroup>::new);
		MRV.setDefaultReturnValuesSupplier("getMetadataElement", MetadataElementSpy::new);
		MRV.setDefaultReturnValuesSupplier("getPresentationElements", ArrayList<DataGroup>::new);
		MRV.setDefaultReturnValuesSupplier("getTexts", ArrayList<DataGroup>::new);
		MRV.setDefaultReturnValuesSupplier("getRecordTypes", ArrayList<DataGroup>::new);
		MRV.setDefaultReturnValuesSupplier("getCollectTerms", ArrayList<DataGroup>::new);
		MRV.setDefaultReturnValuesSupplier("getValidationType", Optional::empty);
		MRV.setDefaultReturnValuesSupplier("getValidationTypes", ArrayList<ValidationType>::new);
		MRV.setDefaultReturnValuesSupplier("getCollectTermHolder", CollectTermHolderSpy::new);

		MRV.setDefaultReturnValuesSupplier("getTextElements", Collections::emptyList);
		MRV.setDefaultReturnValuesSupplier("getTextElement", TextElementSpy::new);
	}

	@Override
	public Collection<DataRecordGroup> getMetadataElements() {
		return (Collection<DataRecordGroup>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public MetadataElement getMetadataElement(String elementId) {
		return (MetadataElement) MCR.addCallAndReturnFromMRV("elementId", elementId);
	}

	@Override
	public Collection<DataGroup> getPresentationElements() {
		return (Collection<DataGroup>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Collection<DataGroup> getTexts() {
		return (Collection<DataGroup>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Collection<DataGroup> getRecordTypes() {
		return (Collection<DataGroup>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Collection<DataGroup> getCollectTermsAsDataGroup() {
		return (Collection<DataGroup>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Optional<ValidationType> getValidationType(String validationId) {
		return (Optional<ValidationType>) MCR.addCallAndReturnFromMRV("validationId", validationId);
	}

	@Override
	public Collection<ValidationType> getValidationTypes() {
		return (Collection<ValidationType>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public CollectTermHolder getCollectTermHolder() {
		return (CollectTermHolder) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Collection<TextElement> getTextElements() {
		return (Collection<TextElement>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public TextElement getTextElement(String elementId) {
		return (TextElement) MCR.addCallAndReturnFromMRV("elementId", elementId);
	}

}
