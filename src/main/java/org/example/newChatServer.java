package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


// https://kj84.tistory.com/entry/TCP-프로그래밍-간단한-채팅-클라이언트-서버-프로그래밍
/*
동시에 -> 스레드
클라: 입출력 동시에 하기 위해 스레드 사용
서버: 클라 여러개로부터 입출력 위해 스레드 사용

채팅 서버는 스레드 간 연관 맺고 있음.
ㄴ 하나의 스레드가 클라로부터 문자열 전송 받으면 다른 스레드의 outputStream 통해 받은 문자열
재전송 할 수 있기 때문

<서버>
접속한 클라마다 스레드 하나 생성성 */

public class newChatServer {

    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(10001);
            System.out.println("접속을 기다립니다.");
            HashMap hm = new HashMap(); // 스레드간 공유할 수 있는 hashmap 객체
            while (true) {
                Socket sock = server.accept(); // accept 메서드로 클라의 접속 기다림
                // 접속한 경우 클라와 통신 도와주는 소켓 객체와 해시맵 객체 ChatThread 생성자에 전달 후
                // ChatThread가 클라와 실질적으로 통신하는 객체.
                ChatThread chatthread = new ChatThread(sock, hm); // 하나의 hm 객체를 공유한다는 뜻
                chatthread.start(); // 스레드 실행 (run 실행)
            } // while
        } catch (Exception e) {
            System.out.println(e);
        }
    } // main
}

class ChatThread extends Thread {
    private Socket sock;
    private String id;
    private BufferedReader br;
    private HashMap hm;
    private boolean initFlag = false;

    public ChatThread(Socket sock, HashMap hm) {
        this.sock = sock;
        this.hm = hm;
        try {
            // 소켓으로부터 OutputStream 얻어서 PrintWriter로 변환,
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            // InputStream 얻어서 BufferedReader로 변환.
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            // BR 구현 후 readLine() 호출. -> 클라가 제일 먼저 전송하는 문자열이 id이기 때문.
            // 받은 id 필드애 저장.
            id = br.readLine();
            broadcast(id + "님이 접속하였습니다.");
            System.out.println("접속한 사용자의 아이디는 " + id + "입니다.");
            synchronized (hm) { // 여러 스레드가 HM 공유하기 때문에 동기화. 데이터 접근 동시에 일어날 수 있기 때문
                // *** 중요 !
                // 해시맵에 id를 키로, pw를 밸류로 저장.
                // 해시맵에 클라의 pw 객체 저장하는 이유는 해시맵 공유함으로써 BR로 전달받은
                // 문자열을 HM에 저장되어 있는 모든 PW 이용해 쓰게 만들기 위해서
                hm.put(this.id, pw);
            }
            initFlag = true;
        } catch (Exception ex) {
            System.out.println(ex);
        }
    } // 생성자


    // 실제 스레드 동작 정의하는 가장 중요한 메소드!
    // 소켓 통해 한줄씩 읽은 문자열이 /quit 이면 while 빠져나가고,
    // /to면 특정 클라에게만 보냄.
    // 제외한 나머지 일반 문자열은 broadcast 메소드 통해서 접속한 모든 클라에게 문자열 전송
    public void run() {
        try {
            String line = null;
            while ((line = br.readLine()) != null) {

                // 클라의 종료한다는 메시지
                if (line.equals("/quit"))
                    break;


                // 특정 사용자에게 보내는 메시지
                if (line.indexOf("/to ") == 0) {
                    sendmsg(line);
                } else {
                    broadcast(id + " : " + line);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex);
        } finally { // 클라가 quit 혹은 강제 종료 시 finally 실행
            synchronized (hm) {
                hm.remove(id);
            }
            // 나머지 클라에게 접속 종료 메시지 전송송            broadcast(id + " 님이 접속 종료하였습니다.");
            try {
                if (sock != null)
                    sock.close();
            } catch (Exception ex) {
            }
        }
    } // run


    // 특정 사용자에게 보내는 메시지
    public void sendmsg(String msg) {
        int start = msg.indexOf(" ") + 1;
        int end = msg.indexOf(" ", start);
        if (end != -1) {
            String to = msg.substring(start, end); // 여기에 id 담김
            String msg2 = msg.substring(end + 1); // 메시지 담김
            Object obj = hm.get(to);
            if (obj != null) {
                PrintWriter pw = (PrintWriter) obj;
                pw.println(id + " 님이 다음의 귓속말을 보내셨습니다. :" + msg2);
                pw.flush();
            } // if
        }
    } // sendmsg

    // 접속한 모든 클라에세 문자열 전송하는 메소드
    // 문자열 인자로 받고, hm에 저장되어 있는 pw를 하나씩 얻어 사용함.
    // hm에는 접속한 모든 클라의 pw 있기 때문에 hm 으로 부터 pw 객체 얻어와
    // 출력한다는 것은 접속한 모든 클라에게 문자열 전송과 같은 효과
    public void broadcast(String msg) {
        synchronized (hm) {
            Collection collection = hm.values();
            Iterator iter = collection.iterator();
            while (iter.hasNext()) {
                PrintWriter pw = (PrintWriter) iter.next();
                pw.println(msg);
                pw.flush();
            }
        }
    } // broadcast
}