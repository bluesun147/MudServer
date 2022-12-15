package org.example;

import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

// https://wakestand.tistory.com/167
// https://blog.naver.com/PostView.nhn?blogId=duddnddl9&logNo=220614912095
public class Server {


    // 시작 시 포트 설정하고 서버 소켓 만든 뒤 while문 안에서
    // 클라 접속 시 socket정보 받음 -> 쓰레에 클라의 소켓 정보 넣고 start

    public static void main(String[] args) {
        try {
            int socketPort = 1234; // 소켓 포트 설정용
            ServerSocket serverSocket = new ServerSocket(socketPort); // 서버 소켓 만들기
            // 서버 오픈 확인용
            System.out.println("socket : 포트 " + socketPort + "으로 서버가 열렸습니다");

            HashMap hm = new HashMap(); // 스레드간 공유할 수 있는 hashmap 객체

            // 소켓 서버가 종료될 때까지 무한루프
            while (true) {
                Socket socket = serverSocket.accept(); // 서버에 클라이언트 접속 시
                // Thread 안에 클라이언트 정보를 담아줌

                ServerThread serverThread = new ServerThread(socket, hm); // 하나의 hm 객체를 공유한다는 뜻
                serverThread.start(); // 스레드 실행 (run 실행)
            }

        } catch (IOException e) {
            e.printStackTrace(); // 예외처리
        }
    }
}

class ServerThread extends Thread {
    private Socket socket;
    private String name;
    private BufferedReader br;
    private HashMap hm;
    private boolean initFlag = false;

    // 생성자
    public ServerThread(Socket socket, HashMap hm) {
        this.socket = socket; // 유저 socket을 할당
        this.hm = hm;
        JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT);
        Jedis jedis = pool.getResource();
        try {
            // 소켓으로부터 OutputStream 얻어서 PrintWriter로 변환,
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            // InputStream 얻어서 BufferedReader로 변환.
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // BR 구현 후 readLine() 호출. -> 클라가 제일 먼저 전송하는 문자열이 id이기 때문.
            // 받은 id 필드에 저장.

            // 어차피 id 는 한번 받으니까 생성자에서 그냥 받는식으로!!!!!
            // id = br.readLine();
            String readValue = br.readLine();
            JSONObject jsonData = new JSONObject(readValue); // 클라에서 json으로 보낸 데이터를 파싱
            name = jsonData.getString("text");

            System.out.println("접속한 유저의 이름은 " + name + "입니다.");
            synchronized (hm) { // 여러 스레드가 HM 공유하기 때문에 동기화. 데이터 접근 동시에 일어날 수 있기 때문
                // *** 중요 !
                // 해시맵에 id를 키로, pw를 밸류로 저장.
                // 해시맵에 클라의 pw 객체 저장하는 이유는 해시맵 공유함으로써 BR로 전달받은
                // 문자열을 HM에 저장되어 있는 모든 PW 이용해 쓰게 만들기 위해서
                hm.put(this.name, pw);

                Object obj = hm.get(name);
                PrintWriter writer = (PrintWriter) obj;

                // 이미 있는 유저라면 ---> 여기서 중복 체크?
                // 지금 접속한 나는 제외해야 하는데
                if ("1".equals(jedis.get("user:" + name))) {

                    JSONObject jsonWelcome = new JSONObject();

                    // 중복 로그인 처리
                    // 지금은 신규 회원일 경우 메시지 안뜸.
                    // 중복 접속일 경우만 뜨긴 하는데 기존애를 강제 종료 시켜야 함.
                    if (obj != null) {
                        jsonWelcome.put("fromServerJsonKey", "중복접속!!!!!!!!");
                        writer.println(jsonWelcome);
                    }

                    else {

                        jsonWelcome.put("fromServerJsonKey", name + "님 환영합니다.\n명령어를 입력하세요");
                        pw.println(jsonWelcome);

                        System.out.println(name + "님은 이미 회원가입한 유저입니다.");
                    }
                } else { // 신규 유저라면

                    User.createUser(name); // 유저 생성

                    JSONObject jsonWelcome = new JSONObject();
                    jsonWelcome.put("fromServerJsonKey", name + "님 환영합니다.\n명령어를 입력하세요");
                    pw.println(jsonWelcome);
                    System.out.println(name + "님 회원가입 완료");
                }
            }
            initFlag = true;
        } catch (Exception ex) {
            System.out.println(ex);
        }
    } // 생성자

    // Thread 에서 start() 메소드 사용 시 자동으로 해당 메소드 시작 (Thread별로 개별적 수행)
    public void run() {
        try {
            JSONObject jo = new JSONObject();
            // 연결 확인용
            System.out.println("서버 : " + socket.getInetAddress() + " IP의 클라이언트와 연결되었습니다");

            // 몬스터 관리
            Monster monster = new Monster(hm, jo);
            monster.manageMonsterGenerate(); // 1분마다 몬스터 카운트해서 10마리 될때까지 생성
            monster.manageMonsterAttack(); // 몬스터 공격

            // OutputStream - 서버에서 클라이언트로 메세지 보내기
            OutputStream out = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(out, true);

            // 클라이언트에게 연결되었다는 메세지 보내기
            JSONObject jsonGreeting = new JSONObject();
            //jsonGreeting.put("fromServerJsonKey", "서버에 연결되었습니다! ID를 입력해 주세요!");
            //writer.println(jsonGreeting);

            String readValue; // Client에서 보낸 값 저장

            // 클라이언트가 메세지 입력시마다 수행
            while ((readValue = br.readLine()) != null) {

                // 클라에서 json으로 보내온 명령문 파싱
                JSONObject jsonData = new JSONObject(readValue);
                String execution = jsonData.getString("text");


                /*
                 < 명령문 >
                - attack : 사용자 9칸 내 모든 몬스터 체력 감소
                - bot
                - chat : chat 유저이름 메시지
                - monsters : 모든 몬스터들 좌표
                - move : move x y
                - my : 나의 위치
                - users : 전체 유저의 위치
                - useHpPotion : 체력 포션 사용
                - useStrPotion : 힘 포션 사용
                 */

                Execute execute = new Execute(socket, name, br, hm, jo, jsonData, writer);

                switch (execution) {

                    ///////////////////////////////////////////////////////

                    case "attack":
                        execute.attack();
                        break;

                    ///////////////////////////////////////////////////////

                    // 1초에 한번씩 명령 랜덤 수행
                    case "bot":
                        execute.bot();
                        break;

                    ///////////////////////////////////////////////////////

                    // chat "유저이름" "대화내용"
                    case "chat":
                        execute.chat();
                        break;

                    ///////////////////////////////////////////////////////

                    // 모든 몬스터들 좌표
                    case "monsters":
                        execute.monsters();
                        break;

                    ///////////////////////////////////////////////////////

                    // 유저 이동
                    case "move":
                        int x = Integer.parseInt(jsonData.getString("x"));
                        int y = Integer.parseInt(jsonData.getString("y"));
                        execute.move(x, y);
                        break;

                    ///////////////////////////////////////////////////////

                    // 나의 위치 출력
                    case "my":
                        execute.my();
                        break;

                    ///////////////////////////////////////////////////////

                    // 모든 유저 위치 출력
                    case "users":
                        execute.users();
                        break;

                    ///////////////////////////////////////////////////////

                    // hp 포션 사용
                    case "useHpPotion":
                        execute.useHpPotion();
                        break;

                    ///////////////////////////////////////////////////////

                    // str 포션 사용
                    case "useStrPotion":
                        execute.useStrPotion();
                        break;

                    ///////////////////////////////////////////////////////

                    // 잘못된 명령문
                    default:
                        execute.defaults();
                        break;
                }

                jsonGreeting.put("fromServerJsonKey", "명령어를 입력하세요");
                writer.println(jsonGreeting);
                System.out.println(name + " : " + execution + " 입력"); // 사용자 3명 접속 시 3번 찍힘
            }

        } catch (Exception e) { // 예외처리
            System.out.println(name + "의 연결 끊어짐!!");
            TimerTask deleteTask = new TimerTask() {
                @Override
                public void run() {
                    User.deleteUser(name);
                    System.out.println(name + " 정보 삭제!");
                }
            };

            // 유저 연결 끊긴 후 5분 뒤 정보 삭제
            // 5분 이내에 재접속하면 복원해야 함. --> 어떻게?
            Timer timer = new Timer();
            timer.schedule(deleteTask, 10 * 1000);
//            while (true) {
//                if (name.equals("tom")) { // 지금 연결 되어있는지 한번 더 확인.
//                    timer.cancel();
//                }
//            }
        }
    }
}