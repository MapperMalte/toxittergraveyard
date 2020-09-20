import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class ChatServer extends WebSocketServer
{

    public ChatServer(int port) throws UnknownHostException
    {
        super(new InetSocketAddress(port));
    }

    public ChatServer(InetSocketAddress address)
    {
        super(address);
    }

    public ChatServer(int port, Draft_6455 draft)
    {
        super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake)
    {
        conn.send("Welcome to the server!"); //This method sends a message to the new client
        broadcast("new connection: " + handshake
                .getResourceDescriptor()); //This method sends a message to all clients connected
        System.out.println(
                conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote)
    {
        broadcast(conn + " has left the room!");
        System.out.println(conn + " has left the room!");
    }
    Reservoir<String,FriendRequestDTO> receivedUnresolvedFriendRequestes = new Reservoir<>();

    @Override
    public void onMessage(WebSocket conn, String message)
    {
        if ( message.startsWith("##Login") )
        {
            message = message.substring("##Login".length(),message.length()-2);
            Gson gson = new Gson();
            LoginDTO loginDTO = gson.fromJson(message,LoginDTO.class);
            loginDTO.webSocket = conn;
            System.out.println("LOGIN: "+loginDTO.username+"/"+loginDTO.password);
            UserList.fulfillLoginDTO(loginDTO);
            conn.send(loginDTO.messageAnswer);
        } else if ( message.startsWith("##ADD_FRIEND ") )
        {
            String who = message.substring("##ADD_FRIEND ".length());
            FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
            friendRequestDTO.to = who;
            friendRequestDTO.from = UserList.get(conn).name;
            UserList.fulfillSendFriendRequestDTO(friendRequestDTO);
            System.out.println("Friendrequest: "+friendRequestDTO.from+"/"+friendRequestDTO.to);
        } else if ( message.startsWith("##ACCEPT_FRIEND_REQUEST") )
        {
            String who = message.substring("##ACCEPT_FRIEND_REQUEST ".length());
            FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
            friendRequestDTO.to = UserList.get(conn).name;
            friendRequestDTO.from = who;
            UserList.fulfillAcceptFriendRequestDTO(friendRequestDTO);

        } else if ( message.startsWith("##GET_FRIENDS_LIST") )
        {

        } else
        {
            broadcast(message);
            System.out.println(conn + ": " + message);
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message)
    {
        broadcast(message.array());
        System.out.println(conn + ": " + message);
    }


    public static void main(String[] args) throws InterruptedException, IOException
    {
        int port = 8887; // 843 flash policy port
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception ex) {
        }
        ChatServer s = new ChatServer(port);
        s.start();
        System.out.println("ChatServer started on port: " + s.getPort());

        BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String in = sysin.readLine();
            s.broadcast(in);
            if (in.equals("exit")) {
                s.stop(1000);
                break;
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex)
    {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    @Override
    public void onStart()
    {
        System.out.println("Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }
}