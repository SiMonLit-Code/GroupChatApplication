package com.cn.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * @author czz
 * @version 1.0
 * @date 2020/10/14 21:40
 */
public class GroupChatServer {
    ServerSocketChannel serverSocketChannel;
    Selector selector;
    int port = 9898;

    public void server() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        String msg = null;

        //设置连接状态
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //设置非堵塞
        serverSocketChannel.configureBlocking(false);

        //设置返回时间
        int select = selector.select(3000);
        //select>0有连接
        if (select > 0){
            Iterator<SelectionKey> keyIterator = selector.keys().iterator();
            while (keyIterator.hasNext()){
                SelectionKey key = keyIterator.next();
                //判断是否为连接状态
                if (key.isAcceptable()){//key.channel??
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);

                    //判断是否可读
                }else if (key.isReadable()){
                    //调用读方法
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    msg = serverRead(socketChannel);
                    socketChannel.register(selector, SelectionKey.OP_WRITE);

                    //判断是否可写
                }else if (key.isWritable()){
                    //群发
                    serverGroupChat(msg, (SocketChannel) key.channel());
                }
            }
        }
    }

    /**
     * 读
     * @param socketChannel
     */
    public String serverRead(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        StringBuilder sb = new StringBuilder();
        while (socketChannel.read(byteBuffer) != -1){
            byteBuffer.flip();
            sb.append(new String(byteBuffer.array()));
            System.out.println(new String(byteBuffer.array()));
            byteBuffer.clear();
        }
        return sb.toString();
    }

    /**
     * 群发
     * @param msg
     * @param socketChannel
     */
    public void serverGroupChat(String msg, SocketChannel socketChannel) throws IOException {
        for (SelectionKey selectionKey : selector.keys()) {
            SocketChannel channel = (SocketChannel)selectionKey.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

            byte[] bytes = msg.getBytes();

            //??
            byteBuffer.put(bytes);

            if (channel != socketChannel){
                while (channel.read(byteBuffer) != -1){

                }
            }
        }

    }

}
