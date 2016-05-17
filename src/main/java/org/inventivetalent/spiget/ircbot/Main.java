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

import com.google.common.io.Files;
import org.jibble.pircbot.IrcException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {

	public static void main(String... args) throws IOException, IrcException {
		String password = Files.readFirstLine(new File("password.txt"), StandardCharsets.UTF_8);
		List<String> connection = Files.readLines(new File("connection.txt"), StandardCharsets.UTF_8);
		String host = connection.get(0);
		int port = 6667;
		if (host.contains(":")) {
			String[] split = host.split(":");
			host = split[0];
			port = Integer.parseInt(split[1]);
		}

		final SpigetBot bot = new SpigetBot();
		bot.setVerbose(true);
		bot.connect(host, port, password);

		boolean first = true;
		for (String s : connection) {
			if (first) {
				first = false;
				continue;
			}
			bot.joinChannel(s);
		}

		bot.changeNick("SpigetBot");
		bot.identify(password);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				bot.quitServer("System.exit(0); - https://spiget.org");
				bot.disconnect();
			}
		});
	}

}
