package servlet;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/WebSocketServlet")
public class WebSocketServlet {

    // 使用 ConcurrentHashMap 存储用户名和对应的 Session 对象
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // WebSocket建立连接
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("连接" + session.getId()+"建立");
    }

    // WebSocket关闭连接
    @OnClose
    public void onClose(Session session) {
        // 关闭所有连接
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                sessions.remove(entry.getKey());// 从 sessions 集合中移除该客户端的会话
                System.out.println("连接" + session.getId()+"关闭");
                break;
            }
        }
    }


    /**
     * 当接收到客户端发送的消息时，此方法会被调用
     * @param message 客户端发送的消息内容
     * @param session 发送消息的客户端的会话对象
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        // 判断消息是否以 "username:" 开头，如果是，则表示客户端在发送用户名
        if (message.startsWith("username:")) {
            // 提取出用户名
            String username = message.replace("username:", "");
            System.out.println("连接" + session.getId()+"昵称为"+username);

            // 将用户名和对应的 Session 对象存入 sessions 集合中
            sessions.put(username, session);
            try {
                // 向客户端返回确认消息，告知客户端服务器已经接收到用户名
                session.getBasicRemote().sendText("username:" + username);
            } catch (IOException e) {
                // 若发送消息时出现异常，打印异常堆栈信息
                e.printStackTrace();
            }
            // 处理完用户名消息后，直接返回，不再进行后续消息广播处理
            return;
        }
        // 如果消息不是用户名消息，则提取出发送者的用户名
        String sender = message.split(":")[0];
        // 提取出消息的内容
        String content = message.split(":", 2)[1];

        System.out.println(message);// 输出到控制台


        // 遍历所有已连接的客户端会话
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            // 获取当前遍历到的客户端的用户名
            String username = entry.getKey();
            // 获取当前遍历到的客户端的会话对象
            Session s = entry.getValue();
            // 如果当前客户端不是消息的发送者，并且该客户端的会话是打开状态
            if (!username.equals(sender) && s.isOpen()) {
                try {
                    // 将接收到的消息广播给该客户端
                    s.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    // 若发送消息时出现异常，打印异常堆栈信息，并从 sessions 集合中移除该客户端的会话
                    e.printStackTrace();
                    sessions.remove(username);
                }
            }
        }
    }


    /**
     * 当 WebSocket 连接发生错误时，此方法会被调用
     * @param session 发生错误的会话对象
     * @param error 发生的错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        // 打印日志，记录发生错误的会话 ID
        System.out.println("发生错误: " + session.getId());
        // 打印错误的堆栈信息
        error.printStackTrace();
    }




    private void saveMessageToLog(String username, String message) {
        try (FileWriter writer = new FileWriter(username + ".log", true)) {
            writer.write(message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadOfflineMessages(String username, Session session) {
        File file = new File(username + ".log");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    session.getBasicRemote().sendText(line);
                }
                // 加载完后清空日志文件
                file.delete();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}