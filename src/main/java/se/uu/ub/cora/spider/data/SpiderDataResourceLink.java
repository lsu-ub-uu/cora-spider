/*
 * Copyright 2015, 2016 Uppsala University Library
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
package se.uu.ub.cora.spider.data;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;

public final class SpiderDataResourceLink extends SpiderDataGroup implements SpiderDataLink {

	private List<Action> actions = new ArrayList<>();

	private SpiderDataResourceLink(String nameInData) {
		super(nameInData);
	}

	private SpiderDataResourceLink(DataGroup dataResourceLink) {
		super(dataResourceLink);
	}

	public static SpiderDataResourceLink withNameInData(String nameInData) {
		return new SpiderDataResourceLink(nameInData);
	}

	public static SpiderDataResourceLink fromDataRecordLink(DataGroup dataResourceLink) {
		return new SpiderDataResourceLink(dataResourceLink);
	}

	@Override
	public void addAction(Action action) {
		actions.add(action);
	}

	@Override
	public List<Action> getActions() {
		return actions;
	}
}
