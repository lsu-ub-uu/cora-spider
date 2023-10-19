/*
 * Copyright 2023 Uppsala University Library
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
package se.uu.ub.cora.spider.resourceconvert;

import java.util.HashMap;
import java.util.Map;

import se.uu.ub.cora.messaging.AmqpMessageSenderRoutingInfo;
import se.uu.ub.cora.messaging.MessageRoutingInfo;
import se.uu.ub.cora.messaging.MessageSender;
import se.uu.ub.cora.messaging.MessagingProvider;

public class ResourceConvertImp implements ResourceConvert {

	private String hostname;
	private int port;
	private String vhost;
	private String exchange;
	private String routingkey;

	public static ResourceConvertImp usingHostnamePortVHostExchangeRoutingKey(String hostname,
			int port, String vhost, String exchage, String routingkey) {
		return new ResourceConvertImp(hostname, port, vhost, exchage, routingkey);
	}

	private ResourceConvertImp(String hostname, int port, String vhost, String exchange,
			String routingkey) {
		this.hostname = hostname;
		this.port = port;
		this.vhost = vhost;
		this.exchange = exchange;
		this.routingkey = routingkey;

	}

	@Override
	public void sendMessageForAnalyzeAndConvertToThumbnails(String dataDivider, String type,
			String id) {
		MessageRoutingInfo messageRoutingInfo = new AmqpMessageSenderRoutingInfo(hostname, port,
				vhost, exchange, routingkey);
		MessageSender sender = MessagingProvider.getTopicMessageSender(messageRoutingInfo);

		Map<String, Object> headers = crateHeadersMap(dataDivider, type, id);
		sender.sendMessage(headers, "Read metadata and convert to small formats");
	}

	private Map<String, Object> crateHeadersMap(String dataDivider, String type, String id) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("dataDivider", dataDivider);
		headers.put("type", type);
		headers.put("id", id);
		return headers;
	}

	public String onlyForTestGetHostName() {
		return hostname;
	}

	public int onlyForTestGetPort() {
		return port;
	}

	public String onlyForTestGetVirtualHost() {
		return vhost;
	}

	public String onlyForTestGetExchange() {
		return exchange;
	}

	public String onlyForTestGetRoutingKey() {
		return routingkey;
	}
}
