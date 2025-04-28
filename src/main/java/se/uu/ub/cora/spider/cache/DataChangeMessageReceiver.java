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
package se.uu.ub.cora.spider.cache;

import java.util.Map;

import se.uu.ub.cora.bookkeeper.metadata.MetadataHolderProvider;
import se.uu.ub.cora.bookkeeper.text.TextHolderProvider;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.messaging.MessageReceiver;
import se.uu.ub.cora.messaging.MessagingProvider;
import se.uu.ub.cora.storage.RecordStorageProvider;

public class DataChangeMessageReceiver implements MessageReceiver {
	protected Logger log = LoggerProvider.getLoggerForClass(DataChangeMessageReceiver.class);

	@Override
	public void receiveMessage(Map<String, String> headers, String message) {
		String type = headers.get("type");
		String id = headers.get("id");
		String action = headers.get("action");

		if (messageSentFromAnotherInstance(headers)) {
			tryToUpdateCachedRecordStorageData(type, id, action);
		}
		if ("metadata".equals(type)) {
			MetadataHolderProvider.dataChanged(id, action);
		}
		if ("text".equals(type)) {
			TextHolderProvider.dataChanged(id, action);
		}
	}

	private boolean messageSentFromAnotherInstance(Map<String, String> headers) {
		String messagingId = headers.get("messagingId");
		return !messagingId.equals(MessagingProvider.getMessagingId());
	}

	private void tryToUpdateCachedRecordStorageData(String type, String id, String action) {
		try {
			updateCachedRecordStorageData(type, id, action);
		} catch (Exception e) {
			log.logFatalUsingMessageAndException("Shuting down due to error keeping data in sync,"
					+ "continued operation would lead to system inconsistencies.", e);
			shutdownSystemToPreventDataInconsistency();
		}
	}

	private void updateCachedRecordStorageData(String type, String id, String action) {
		RecordStorageProvider.dataChanged(type, id, action);
	}

	void shutdownSystemToPreventDataInconsistency() {
		// Important! The JVM will be shutdown.
		System.exit(-1);
	}

	@Override
	public void topicClosed() {
		log.logFatalUsingMessage("Shuting down due to lost connection with message broker,"
				+ "continued operation would lead to system inconsistencies.");
		shutdownSystemToPreventDataInconsistency();
	}

}
