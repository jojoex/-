package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NIOServer {

    private final String IP = "localhost";
    private final int PORT = 8888;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private int count;

    public NIOServer init() {
        try {
            serverChannel = ServerSocketChannel.open();
            selector = Selector.open();
            serverChannel.bind(new InetSocketAddress(IP,PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            count = 0;
        } catch (IOException e) {
            System.out.println("初始化失败...");
        } finally {
            return this;
        }
    }

    public void start() {
        while(true) {
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        userConnect(key);
                    }
                    if (key.isReadable()) {
                        userSendMsg(key);
                    }
                    iterator.remove();
                }
            } catch (IOException e) {
                System.out.println("未知的bug...");
            }
        }
    }

    public void userConnect(SelectionKey key) throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        String msg = "有新的用户接入：" + channel.socket().getRemoteSocketAddress() +
                "，在线用户总数：" + ++count + "个";
        System.out.println(msg);
        transfer(msg, channel);
    }

    public void userSendMsg(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String msg = null;
        try {
            int len = channel.read(buffer);
            if (len == -1)
                throw new IOException();
            msg = channel.socket().getPort() + "：" + new String(buffer.array(), 0, len);
        } catch (IOException e) {
            msg = channel.socket().getPort() + "断开连接，在线用户总数：" + --count + "个";
            key.cancel();
            channel.socket().close();
            channel.close();
        } finally {
            System.out.println(msg);
            transfer(msg, channel);
        }
    }

    public void transfer(String msg, SocketChannel self) throws IOException{
        Iterator<SelectionKey> iterator = selector.keys().iterator();
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        while(iterator.hasNext()) {
            SelectionKey key = iterator.next();
            if(key.channel() instanceof ServerSocketChannel)
                continue;
            SocketChannel channel = (SocketChannel) key.channel();
            //转发用户消息
            if(self != key.channel()) {
                channel.write(buffer);
                buffer.clear();
            }
        }
    }

    public static void main(String[] args) {
        new NIOServer().init().start();
    }

}

