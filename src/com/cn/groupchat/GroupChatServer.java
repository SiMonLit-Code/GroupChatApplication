package com.cn.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @author czz
 * @version 1.0
 * @date 2020/10/14 21:40
 */
public class GroupChatServer {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private int port = 9898;

    /**
     * 初始化服务器
     * @throws IOException
     */
    public GroupChatServer() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        //设置非堵塞(设置非堵塞才能注册选择器，否则会报IllegalBlockingModeException)
        serverSocketChannel.configureBlocking(false);
        //设置连接状态
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //监听
        listener();
    }

    /**
     * 监听
     * @throws IOException
     */
    private void listener() throws IOException {
        System.out.println("服务器已启动，正在监听...");
        while (true) {
            //设置返回时间
            int select = selector.select(3000);
            //select>0有连接
            if (select > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    //判断是否为连接状态
                    if (key.isAcceptable()) {//key.channel??
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        System.out.println(socketChannel.getRemoteAddress() + " 上线了");

                        //判断是否可读
                    } else if (key.isReadable()) {
                        //调用读方法
                        serverRead(key);
                    }
                    keyIterator.remove();
                }

            }
        }
    }

    /**
     * 读
     * @param key
     */
    public void serverRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = null;
        StringBuilder sb = new StringBuilder();
        try {
            socketChannel = (SocketChannel) key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

            while (socketChannel.read(byteBuffer) != 0){
                byteBuffer.flip();
                byte[] bytes = new byte[byteBuffer.limit()];
                byteBuffer.get(bytes);
                sb.append(new String(bytes));
                byteBuffer.clear();
            }
            String msg = sb.toString();
            System.out.println(msg);
            serverGroupChat(msg, socketChannel);

        }catch (Exception e){
//            e.printStackTrace();
            System.out.println(socketChannel.getRemoteAddress() + " 离线了..");
            //取消注册
            key.cancel();
            if (socketChannel != null){
                //关闭通道
                socketChannel.close();
            }
        }
    }

    /**
     * 群发
     * @param msg
     * @param selfChannel
     */
    public void serverGroupChat(String msg, SocketChannel selfChannel) throws IOException {
        for (SelectionKey selectionKey : selector.keys()) {
            Channel targetChannel = selectionKey.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

            byte[] bytes = msg.getBytes();
            int length = bytes.length;

            //排除自己
            if (targetChannel instanceof SocketChannel && targetChannel != selfChannel){
                SocketChannel socketChannel = (SocketChannel) targetChannel;
                int i = 0;
                int num = -1;
                if (length > byteBuffer.capacity()){
                    while (length > 0){
                        num = length > byteBuffer.capacity()? byteBuffer.capacity():length;
                        byteBuffer.put(bytes, 1024*i++, num);
                        length -= 1024;
                        byteBuffer.flip();
                        socketChannel.write(byteBuffer);
                        byteBuffer.clear();
                    }
                }else {
                    byteBuffer.put(bytes);
                    byteBuffer.flip();
                    socketChannel.write(byteBuffer);
                }
            }
        }

    }

    public static void main(String[] args) throws IOException {
        new GroupChatServer();
    }
}
