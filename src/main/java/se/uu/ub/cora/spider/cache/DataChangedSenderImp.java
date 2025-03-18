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

import java.util.HashMap;
import java.util.Map;

import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.messaging.AmqpMessageSenderRoutingInfo;
import se.uu.ub.cora.messaging.MessageRoutingInfo;
import se.uu.ub.cora.messaging.MessageSender;
import se.uu.ub.cora.messaging.MessagingProvider;

public class DataChangedSenderImp implements DataChangedSender {
	public static DataChangedSender create() {
		return new DataChangedSenderImp();
	}

	@Override
	public void sendDataChanged(String type, String id, String action) {
		MessageSender sender = getMessageSenderUsingExchange(type);
		Map<String, Object> headers = crateHeadersMap(type, id, action);

		sender.sendMessage(headers, "");
	}

	private MessageSender getMessageSenderUsingExchange(String routingKey) {
		MessageRoutingInfo messageRoutingInfo = createRoutingInfoFromSettingsProvider(routingKey);
		return MessagingProvider.getTopicMessageSender(messageRoutingInfo);
	}

	private MessageRoutingInfo createRoutingInfoFromSettingsProvider(String routingKey) {
		String hostname = SettingsProvider.getSetting("rabbitMqHostname");
		int port = Integer.parseInt(SettingsProvider.getSetting("rabbitMqPort"));
		String vhost = SettingsProvider.getSetting("rabbitMqVirtualHost");
		String exchange = SettingsProvider.getSetting("rabbitMqDataExchange");

		return new AmqpMessageSenderRoutingInfo(hostname, port, vhost, exchange, routingKey);
	}

	private Map<String, Object> crateHeadersMap(String type, String id, String action) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("type", type);
		headers.put("id", id);
		headers.put("action", action);
		return headers;
	}
}
