package com.thedeveloperfriend.websocketclient.memoryinfo;

import java.io.IOException;
import java.net.URI;
import javax.websocket.*;

/**
 * Annotated WebSocket client endpoint class to connect to the server and
 * send/receive messages
 */
@ClientEndpoint
public class MemoryInfoClientApplication {
	// Each client should have a unique name assigned, so that server can able to
	// differentiate them
	private static String unique_client_name = "CLIENT_01";
	// Hostname of the WebSocket server
	private static String serverHostName = "memoryinfowebsocketserver.com";

	private static Object waitLock = new Object();
	static WebSocketContainer container = null;
	static Session session = null;

	private static Thread sessionAliveCheckThread;

	/**
	 * This life cycle method will be invoked whenever the client receive some
	 * message from server.
	 */
	@OnMessage
	public void onMessage(String infoRequest) {
		System.out.println("infoRequest = " + infoRequest);
		try {

			long totalMemory = (Runtime.getRuntime().totalMemory() / 1024) / 1024;
			long availableMemory = (Runtime.getRuntime().freeMemory() / 1024) / 1024;
			String responseString = "RESPONSE MESSAGE|" + totalMemory + "|" + availableMemory;
			String infoResponse = infoRequest + "^" + responseString;
			System.out.println("infoResponse = " + infoResponse);
			// Send the memory info as a response back to the server
			session.getBasicRemote().sendText(infoResponse);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void wait4TerminateSignal() {
		synchronized (waitLock) {
			try {
				waitLock.wait();
			} catch (InterruptedException e) {
			}
		}
	}

	public static void main(String[] args) {

		try {
			System.out.println("Starting WebSocket Client. Client ID = " + unique_client_name);
			container = ContainerProvider.getWebSocketContainer();

			// This thread will keep checking the connection with the WebSocket server.
			// If the connection is lost it will try to connect again
			sessionAliveCheckThread = new Thread() {
				public void run() {
					while (true) {
						try {
							Thread.sleep(1000);

							if (session == null || !session.isOpen()) {
								// Note in the WSS URL we are passing the unique client name
								session = container.connectToServer(MemoryInfoClientApplication.class,
										URI.create("wss://" + serverHostName
												+ "/memory_info_websocket_server/MemoryInfoWebSocketEndPoint/"
												+ unique_client_name));
								System.out.println("Connected to WebSocket Server ....");

							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
			sessionAliveCheckThread.start();
			wait4TerminateSignal();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}