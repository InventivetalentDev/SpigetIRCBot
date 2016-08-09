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
import com.google.common.base.Strings;
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
import java.text.DecimalFormat;
import java.util.*;

public class SpigetBot extends PircBot {

	static String VERSION;

	static {
		try {
			try (InputStream in = SpigetBot.class.getResourceAsStream("/pom.properties")) {
				Properties properties = new Properties();
				properties.load(in);
				VERSION = properties.getProperty("spiget.ircbot.version");
			}
		} catch (IOException e) {
			e.printStackTrace();
			VERSION = "unknown";
		}
	}

	SpigetBot() {
		setName("SpigetBot");
		setVersion("SpigetIRCBot " + VERSION + " - https://github.com/InventivetalentDev/SpigetIRCBot");
	}

	@Override
	protected void onJoin(String channel, String sender, String login, String hostname) {
		super.onJoin(channel, sender, login, hostname);
		if (getNick().equals(sender)) {
			setMode(getNick(), "+B");
		}
	}

	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		String lowerMessage = message.toLowerCase();
		if (lowerMessage.startsWith("?spiget")) {
			Iterable<String> iterable = Splitter.on(" ").trimResults().omitEmptyStrings().split(message.substring("?spiget".length()));
			String[] args = Iterables.toArray(iterable, String.class);

			if (args.length <= 0) {
				sendMessage(channel, sender + ": https://spiget.org | https://github.com/InventivetalentDev/SpigetIRCBot");
				return;
			}

			// Request stats
			if ("requests".equalsIgnoreCase(args[0])) {
				int amount = 10;
				boolean fullUserAgents = false;
				if (args.length > 1) {
					try {
						amount = Integer.parseInt(args[1]);
					} catch (NumberFormatException ignored) {
					}
					if (args.length > 2 && args[2] != null) {
						fullUserAgents = args[2].toLowerCase().contains("full-user-agents");
					}
				}
				try {
					HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spiget.org/v2/metrics/requests/1?ut=" + System.currentTimeMillis() + (fullUserAgents ? "" : "&stripUaVersion=true")).openConnection();
					connection.setRequestProperty("User-Agent", "SpigetIRCBot/1.4");

					try (InputStream in = connection.getInputStream()) {
						JsonObject result = new JsonParser().parse(new InputStreamReader(in)).getAsJsonObject();
						JsonObject today = result.entrySet().iterator().next().getValue().getAsJsonObject();

						DecimalFormat decimalFormat = new DecimalFormat("###,##0");

						// Total
						sendMessage(channel, sender + ": Today's total: " + decimalFormat.format(today.get("total").getAsInt()) + " requests");

						// User Agents
						List<Map.Entry<String, JsonElement>> userAgentMap = new LinkedList<>(today.getAsJsonObject("user_agents").entrySet());
						Collections.sort(userAgentMap, (o1, o2) -> -Integer.compare(o1.getValue().getAsInt(), o2.getValue().getAsInt()));// Use - to reverse the order

						// Methods
						List<Map.Entry<String, JsonElement>> methodMap = new LinkedList<>(today.getAsJsonObject("methods").entrySet());
						Collections.sort(methodMap, (o1, o2) -> -Integer.compare(o1.getValue().getAsInt(), o2.getValue().getAsInt()));// Use - to reverse the order

						String format = "%-3s | %-32.32s | %-32.48s";
						List<String> messages = new ArrayList<>();
						messages.add(String.format(format, "#", "User-Agent", "Method"));
						messages.add(String.format(format, "---", Strings.repeat("-", 32), Strings.repeat("-", 32)));
						for (int i = 0; i < amount; i++) {
							Map.Entry<String, JsonElement> userAgentEntry = userAgentMap.size() > i ? userAgentMap.get(i) : null;
							Map.Entry<String, JsonElement> methodEntry = methodMap.size() > i ? methodMap.get(i) : null;

							String userAgent = userAgentEntry != null ? "(" + decimalFormat.format(userAgentEntry.getValue().getAsInt()) + ") " + userAgentEntry.getKey() : "";
							String method = methodEntry != null ? "(" + decimalFormat.format(methodEntry.getValue().getAsInt()) + ") " + methodEntry.getKey() : "";

							messages.add(String.format(format, "#" + (i + 1), userAgent, method));
						}

						for (String s : messages) {
							sendMessage(channel, s);
						}
					}
				} catch (Exception e) {
					sendMessage(channel, sender + ": Sorry, I couldn't connect to spiget");
					System.err.println("Connection failed");
					e.printStackTrace();
				}

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
				HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spiget.org/v2/search/" + searchType + "/" + searchQuery + "?size=1000&ut=" + System.currentTimeMillis()).openConnection();
				connection.setRequestProperty("User-Agent", "SpigetIRCBot/1.4");
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
						sendMessage(channel, sender + ": " + singleResult.get("name").getAsString() + ", https://" + (searchTypeId == 0 ? "r" : "a") + ".spiget.org/" + singleResult.get("id").getAsInt());
						return;
					} else {
						sendMessage(channel, "(" + result.size() + " results)");

						double smallest = 1024;// This should be more than enough
						String bestName = null;
						int bestId = 0;
						for (JsonElement jsonElement : result) {
							String name = jsonElement.getAsJsonObject().get("name").getAsString();
							double distance = LevenshteinDistance.computeDistance(searchQuery, name);
							if (distance < smallest) {
								smallest = distance;
								bestName = name;
								bestId = jsonElement.getAsJsonObject().get("id").getAsInt();
							}
						}

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
