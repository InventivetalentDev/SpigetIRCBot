/*
 * Copyright 2016 inventivetalent. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and contributors and should not be interpreted as representing official policies,
 *  either expressed or implied, of anybody else.
 */

package org.inventivetalent.spiget.ircbot;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jibble.pircbot.PircBot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class SpigetBot extends PircBot {

	static final String VERSION = "1.0";

	SpigetBot() {
		setName("SpigetBot");
		setVersion("SpigetIRCBot " + VERSION + " - spiget.org");

	}

	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		String lowerMessage = message.toLowerCase();
		if (lowerMessage.startsWith("?spiget")) {
			Iterable<String> iterable = Splitter.on(" ").trimResults().omitEmptyStrings().split(message.substring("?spiget".length()));
			String[] args = Iterables.toArray(iterable, String.class);

			if (args.length <= 0) {
				sendMessage(channel, sender + ": https://spiget.org");
				return;
			}
			// Search
			String searchType = "resources";
			int searchTypeId = 0;
			String searchQuery = "";
			if (args.length >= 2) {
				if ("resources".equalsIgnoreCase(args[0]) || "resource".equalsIgnoreCase(args[0]) || "r".equalsIgnoreCase(args[0])) {
					searchType = "resources";
					searchTypeId = 0;
				}
				if ("authors".equalsIgnoreCase(args[0]) || "author".equalsIgnoreCase(args[0]) || "a".equalsIgnoreCase(args[0])) {
					searchType = "authors";
					searchTypeId = 1;
				}
				searchQuery = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));
			} else if ("me".equalsIgnoreCase(args[0])) {
				searchType = "authors";
				searchTypeId = 1;
				searchQuery = sender;
			} else {
				searchQuery = Joiner.on(" ").join(args);
			}

			System.out.println("Searching " + searchType + " for '" + searchQuery + "'");
			try {
				HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spiget.org/v1/search/" + searchType + "/" + searchQuery + "?ut=" + System.currentTimeMillis()).openConnection();
				connection.setRequestProperty("User-Agent", "SpigetIRCBot/1.0");
				if (connection.getResponseCode() != 200) {
					sendMessage(channel, sender + ": Sorry, no results found (" + connection.getResponseCode() + ")");
					return;
				}

				try (InputStream in = connection.getInputStream()) {
					JsonArray result = new JsonParser().parse(new InputStreamReader(in)).getAsJsonArray();
					if (result.size() <= 0) {
						sendMessage(channel, sender + ": Sorry, no results found");
						return;
					}
					if (result.size() == 1) {
						JsonObject singleResult = result.get(0).getAsJsonObject();
						sendMessage(channel, sender + ": " + singleResult.get(searchTypeId == 0 ? "name" : "username").getAsString() + ", https://" + (searchTypeId == 0 ? "r" : "a") + ".spiget.org/" + singleResult.get("id").getAsInt());
						return;
					} else {
						double smallest = 1024;// This should be more than enough
						String bestName = null;
						int bestId = 0;
						for (JsonElement jsonElement : result) {
							String name = jsonElement.getAsJsonObject().get(searchTypeId == 0 ? "name" : "username").getAsString();
							double distance = LevenshteinDistance.computeDistance(searchQuery, name);
							if (distance < smallest) {
								smallest = distance;
								bestName = name;
								bestId = jsonElement.getAsJsonObject().get("id").getAsInt();
							}
						}

						sendMessage(channel, "(Multiple results)");
						if (bestName != null) {
							sendMessage(channel, sender + ": " + bestName + ", https://" + (searchTypeId == 0 ? "r" : "a") + ".spiget.org/" + bestId);
						}
						return;
					}
				}
			} catch (IOException e) {
				sendMessage(channel, sender + ": Sorry, I couldn't connect to spiget");
				System.err.println("Connection failed");
				e.printStackTrace();
			}
		}
	}

}
