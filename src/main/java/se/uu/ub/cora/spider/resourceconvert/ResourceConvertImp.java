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

import se.uu.ub.cora.messaging.MessageRoutingInfo;
import se.uu.ub.cora.messaging.MessageSender;
import se.uu.ub.cora.messaging.MessagingProvider;

public class ResourceConvertImp implements ResourceConvert {

	private static final String ANALYZE_AND_CONVERT_TO_THUMBNAILS = "analyzeAndConvertToThumbnails";
	private String hostname;
	private String port;

	public static ResourceConvertImp usingHostnameAndPort(String hostname, String port) {
		return new ResourceConvertImp(hostname, port);
	}

	private ResourceConvertImp(String hostname, String port) {
		this.hostname = hostname;
		this.port = port;
	}

	@Override
	public void analyzeAndConvertToThumbnails(String dataDivider, String type, String id) {
		MessageRoutingInfo messageRoutingInfo = new MessageRoutingInfo(hostname, port,
				ANALYZE_AND_CONVERT_TO_THUMBNAILS);
		MessageSender sender = MessagingProvider.getTopicMessageSender(messageRoutingInfo);
		sender.sendMessage(null, null);

	}

}
