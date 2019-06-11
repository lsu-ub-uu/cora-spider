/*
 * Copyright 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerFactory;

public class LoggerFactorySpy implements LoggerFactory {

	public Map<String, LoggerSpy> createdLoggers = new HashMap<>();

	@Override
	public Logger factorForClass(Class<? extends Object> javaClass) {
		String name = javaClass.getSimpleName();
		createdLoggers.put(name, new LoggerSpy());
		return createdLoggers.get(name);
	}

	public int getNoOfFatalLogMessagesUsingClassName(String className) {
		List<String> fatalMessages = (createdLoggers.get(className)).fatalMessages;
		return fatalMessages.size();
	}

	public String getFatalLogMessageUsingClassNameAndNo(String className, int messageNo) {
		List<String> fatalMessages = (createdLoggers.get(className)).fatalMessages;
		return fatalMessages.get(messageNo);
	}

	public String getInfoLogMessageUsingClassNameAndNo(String className, int messageNo) {
		List<String> fatalMessages = (createdLoggers.get(className)).infoMessages;
		return fatalMessages.get(messageNo);
	}

}
