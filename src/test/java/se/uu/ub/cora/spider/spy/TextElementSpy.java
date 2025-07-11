/*
 * Copyright 2025 Uppsala University Library
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
package se.uu.ub.cora.spider.spy;

import java.util.Collections;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.text.TextElement;
import se.uu.ub.cora.bookkeeper.text.Translation;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class TextElementSpy implements TextElement {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public TextElementSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getId", () -> "someTextId");
		MRV.setDefaultReturnValuesSupplier("getTranslations", Collections::emptySet);
		MRV.setDefaultReturnValuesSupplier("getTranslationByLanguage", String::new);
	}

	@Override
	public String getId() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<Translation> getTranslations() {
		return (Set<Translation>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String getTranslationByLanguage(String language) {
		return (String) MCR.addCallAndReturnFromMRV("language", language);
	}

	@Override
	public void addTranslation(String language, String text) {
		MCR.addCall("language", language, "text", text);
	}
}
