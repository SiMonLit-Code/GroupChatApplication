package com.cn.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

/**
 * @author : czz
 * @version : 1.0.0
 * @create : 2020-10-15 10:16:00
 * @description :
 */
public class GroupChatClient {
    private SocketChannel socketChannel ;
    private Selector selector;
    private String host = "127.0.0.1";
    private int port = 9898;
    private String userName;

    /**
     * 初始化客户端
     */
    public GroupChatClient() {
        try {
            socketChannel= SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host,port));
            socketChannel.configureBlocking(false);
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_READ);
            userName = socketChannel.getLocalAddress().toString().substring(1);
            System.out.println("已上线..开始聊天:");
        } catch (IOException e) {
            System.out.println("找不到服务器，或服务器已下线");
//            e.printStackTrace();
        }
    }

    /**
     * 发送消息
     * @param next
     * @throws IOException
     */
    public void sendMsg(String next) throws IOException {
        String msg = userName+"说了："+next;
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byte[] bytes = msg.getBytes();
        int length = bytes.length;
        int i= 0;
        int num = -1;
        //长文本判断
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
//        socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
    }

    /**
     * 接受消息
     */
    public void readInfo()  {
        try {
            int select = selector.select();
            if (select > 0){//有通道
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()){
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()){
                        //接收消息
                        SocketChannel socketChannel = (SocketChannel)key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        StringBuilder sb = new StringBuilder();
                        while (socketChannel.read(buffer) != 0){
                            buffer.flip();
                            byte[] bytes = new byte[buffer.limit()];
                            buffer.get(bytes);
                            sb.append(new String(bytes));
                            buffer.clear();
                        }
                        System.out.println(sb.toString());
                    }
                    keyIterator.remove();
                }

            }
        } catch (IOException e) {
            System.out.println("----找不到服务器或服务器已关闭----");
//            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {
        GroupChatClient groupChatClient = new GroupChatClient();
        new Thread(() -> {
            while (true){
                groupChatClient.readInfo();
                try {//三秒返回一次
                    Thread.currentThread().sleep(3000);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()){
            String next = scanner.next();
            groupChatClient.sendMsg(next);
        }

    }
}
