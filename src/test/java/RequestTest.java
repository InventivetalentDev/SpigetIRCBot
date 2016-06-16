import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class RequestTest {

	@Test
	public void sortTest() {
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spiget.org/v1/metrics/requests/1?ut=" + System.currentTimeMillis()).openConnection();
			connection.setRequestProperty("User-Agent", "SpigetIRCBot/1.0");

			try (InputStream in = connection.getInputStream()) {
				JsonObject result = new JsonParser().parse(new InputStreamReader(in)).getAsJsonObject();
				JsonObject today = result.entrySet().iterator().next().getValue().getAsJsonObject();

				// Total
				System.out.println(": Today's total: " + today.get("total").getAsInt() + " requests");

				// User Agents
				List<Map.Entry<String, JsonElement>> userAgentMap = new LinkedList<>(today.getAsJsonObject("user_agents").entrySet());
				Collections.sort(userAgentMap, (o1, o2) -> -Integer.compare(o1.getValue().getAsInt(), o2.getValue().getAsInt()));// Use - to reverse the order
				System.out.println(userAgentMap);

				System.out.println();
				System.out.println();
				System.out.println();

				// Methods
				List<Map.Entry<String, JsonElement>> methodMap = new LinkedList<>(today.getAsJsonObject("methods").entrySet());
				Collections.sort(methodMap, (o1, o2) -> -Integer.compare(o1.getValue().getAsInt(), o2.getValue().getAsInt()));
				System.out.println(methodMap);

				String format = "%-3s | %-32.32s | %-32.48s";
				List<String> messages = new ArrayList<>();
				messages.add(String.format(format, "#", "User-Agent", "Method"));
				messages.add(String.format(format, "---", Strings.repeat("-", 32), Strings.repeat("-", 32)));
				for (int i = 0; i < 10; i++) {
					Map.Entry<String, JsonElement> userAgentEntry = userAgentMap.size() > i ? userAgentMap.get(i) : null;
					Map.Entry<String, JsonElement> methodEntry = methodMap.size() > i ? methodMap.get(i) : null;

					String userAgent = userAgentEntry != null ? "(" + userAgentEntry.getValue().getAsInt() + ") " + userAgentEntry.getKey() : "";
					String method = methodEntry != null ? "(" + methodEntry.getValue().getAsInt() + ") " + methodEntry.getKey() : "";

					messages.add(String.format(format, "#" + (i + 1), userAgent, method));
				}

				for (String s : messages) {
					System.out.println(s);
				}
			}
		} catch (Exception e) {
			System.err.println("Connection failed");
			e.printStackTrace();
		}
	}

}
