package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Execute {

    private Socket socket;
    private String name;
    private BufferedReader br;
    private HashMap hm;
    private JSONObject jo;
    private JSONObject jsonData;
    private PrintWriter writer;

    public Execute(Socket socket, String name, BufferedReader br, HashMap hm, JSONObject jo, JSONObject jsonData, PrintWriter writer) {
        this.socket = socket;
        this.name = name;
        this.br = br;
        this.hm = hm;
        this.jo = jo;
        this.jsonData = jsonData;
        this.writer = writer;
    }

    // jedis 사용
    JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT);
    Jedis jedis = pool.getResource();

    Iterator<String> iter = null;
    JSONArray jArray = null;
    String userX, userY, monsterX, monsterY;
    List<String> monsterList = null;
    Set<String> monsters = null;

    public void bot() {
        System.out.println(name + " bot 실행");
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // (int) (Math.random() * (최댓값-최소값+1) + 최소값)
                // 최대 6, 최소 0
                int num = (int) (Math.random() * (6 - 0 + 1) + 0); // 랜덤 숫자
                switch (num) {
                    case 0:
                        jo.put("fromServerJsonKey", "attack 실행");
                        writer.println(jo);
                        attack();
                        break;
                    case 1:
                        jo.put("fromServerJsonKey", "monsters 실행");
                        writer.println(jo);
                        monsters(); // 왜 두번 실행함???
                        break;
                    case 2:
                        // 최대 3, 최소 -3
                        int x = new Random().nextInt(7) - 3; // 랜덤 x 좌표
                        int y = new Random().nextInt(7) - 3; // 랜덤 y 좌표
                        jo.put("fromServerJsonKey", "move " + x + " " + y + " 실행");
                        writer.println(jo);
                        move(x, y);
                        break;
                    case 3:
                        jo.put("fromServerJsonKey", "my 실행");
                        writer.println(jo);
                        my();
                        break;
                    case 4:
                        jo.put("fromServerJsonKey", "users 실행");
                        writer.println(jo);
                        users(); // 이것도 두번 실행됨.
                        break;

                    case 5:
                        jo.put("fromServerJsonKey", "useHpPotion 실행");
                        writer.println(jo);
                        useHpPotion(); // 이것도 두번 실행됨.
                        break;

                    case 6:
                        jo.put("fromServerJsonKey", "useStrPotion 실행");
                        writer.println(jo);
                        useStrPotion(); // 이것도 두번 실행됨.
                        break;

                    default:
                        jo.put("fromServerJsonKey", "default 실행");
                        writer.println(jo);
                        defaults();
                        break;
                }
            }
        };

        // 1초마다 반복
        new Timer().scheduleAtFixedRate(task, 3000L, 4 * 1000);
    }

    // 사용자 9칸 내 모든 몬스터 체력 감소
    // 공격 발생 시 전체 클라이언트에세 알림 가게 함.
    public void attack() {
        System.out.println(name + " attack 실행");
        // 유저 위치
        userX = jedis.hget("user:" + name + ":space", "X");
        userY = jedis.hget("user:" + name + ":space", "Y");

        String plusMessage = ""; // 추가로 보낼 메시지

        monsters = jedis.keys("monster:*:hp");
        iter = monsters.iterator(); // set을 Iterator 안에 담기
        monsterList = new ArrayList<String>();
        while (iter.hasNext()) { // iterator에 다음 값이 있다면
            monsterList.add(iter.next().split(":")[1]); // 이름만 꺼내기
        }
        String[] monsterArr = monsterList.toArray(new String[monsterList.size()]); // 몬스터 이름 배열

        int nearMonsterCount = 0;

        for (int i = 0; i < monsterArr.length; i++) {

            monsterX = jedis.hget("monster:" + monsterArr[i] + ":space", "X");
            monsterY = jedis.hget("monster:" + monsterArr[i] + ":space", "Y");

            int userStr = Integer.parseInt(jedis.get("user:" + name + ":str"));

            int monsterXInt = Integer.parseInt(monsterX);
            int monsterYInt = Integer.parseInt(monsterY);

            // 유저의 9칸 범위내에 있는 몬스터가 있으면 체력 감소시킴
            if (((monsterXInt == Integer.parseInt(userX)) || (monsterXInt == Integer.parseInt(userX) - 1) || (monsterXInt == Integer.parseInt(userX) + 1))
                    && ((monsterYInt == Integer.parseInt(userY)) || (monsterYInt == Integer.parseInt(userY) - 1) || (monsterYInt == Integer.parseInt(userY) + 1))) {
                nearMonsterCount++;
                // 몬스터 있다면 체력 감소시킴
                jedis.incrBy("monster:" + monsterArr[i] + ":hp", -1 * userStr);

                String monsterHp = jedis.get("monster:" + monsterArr[i] + ":hp");

                // 몬스터 사망
                if (Integer.parseInt(monsterHp) <= 0) {

                    plusMessage += "\n" + monsterArr[i] + " 사망!";

                    jedis.del("monster:" + monsterArr[i]); // 이름
                    jedis.del("monster:" + monsterArr[i] + ":hp"); // hp
                    jedis.del("monster:" + monsterArr[i] + ":str"); // str
                    jedis.hdel("monster:" + monsterArr[i] + ":space", "X");
                    jedis.hdel("monster:" + monsterArr[i] + ":space", "Y"); // 좌표

                    // 순서 바꿔서 비교하면 null 이라 오류 뜸!!
                    // null.equals("aa") 이게 안되는듯
                    if ("1".equals(jedis.hget("monster:" + monsterArr[i] + ":potions", "hpPotion"))) { // hp 포션 갖고있는 몬스터일때
                        jedis.hdel("monster:" + monsterArr[i] + ":potions", "hpPotion"); // 몬스터 포션 삭제
                        jedis.hincrBy("user:" + name + ":potions", "hpPotion", 1); // 유저에게 포션 추가
                        plusMessage += "\n" + name + "이 hpPotion을 획득하였습니다!";
                    }
                    if ("1".equals(jedis.hget("monster:" + monsterArr[i] + ":potions", "strPotion"))) { // hp 포션 갖고있는 몬스터일때
                        jedis.hdel("monster:" + monsterArr[i] + ":potions", "strPotion"); // 몬스터 포션 삭제
                        jedis.hincrBy("user:" + name + ":potions", "strPotion", 1); // 유저에게 포션 추가
                        plusMessage += "\n" + name + "이 strPotion을 획득하였습니다!";
                    }
                }

                // 모든 클라에게 메시지 전송!!!!
                ////// 이게 broadcast
                jo.put("fromServerJsonKey", name + "이 " + monsterArr[i] + "을 공격해서 데미지 " + userStr + "을 가했습니다. 몬스터 hp : " + monsterHp + plusMessage);
                broadcast(jo.toString());
            }
        }

        if (nearMonsterCount == 0) {
            jo.put("fromServerJsonKey", "주변에 몬스터가 없습니다!");
            writer.println(jo);
            writer.flush();
        }
    }

    public void chat() {
        System.out.println(name + " chat 실행");
        String user = jsonData.getString("user");
        String message = jsonData.getString("message");
        sendChat(user, message);
    }

    public void monsters() {
        System.out.println(name + " monsters 실행");
        monsters = jedis.keys("monster:*:hp");
        iter = monsters.iterator(); // set을 Iterator 안에 담기
        // 리스트에 몬스터 이름 담기
        monsterList = new ArrayList<String>();
        while (iter.hasNext()) { // iterator에 다음 값이 있다면
            monsterList.add(iter.next().split(":")[1]); // 이름만 꺼내기
        }
        // 리스트 배열로 변환 -> jsonObject에 넣기 위해
        // https://shlee0882.tistory.com/260 // 배열
        // https://velog.io/@minwest/Java-JSONObject-데이터-List에-저장하기 // 리스트
        String monsterArr[] = monsterList.toArray(new String[monsterList.size()]); // 몬스터 이름 배열

        // jsonArray 생성
        jArray = new JSONArray();

        for (int i = 0; i < monsterArr.length; i++) {
            JSONObject json = new JSONObject();
            json.put("name", monsterArr[i]);

            monsterX = jedis.hget("monster:" + monsterArr[i] + ":space", "X");
            monsterY = jedis.hget("monster:" + monsterArr[i] + ":space", "Y");

            json.put("x", monsterX);
            json.put("y", monsterY);

            // JsonArray에 json 추가
            jArray.put(json);
        }

        jo.put("monsters", "dummy");
        writer.println(jo);

        writer.println(jArray);
    }

    public void move(int x, int y) {
        System.out.println(name + " move " + x + " " + y + " 실행");
        // 3칸 이하로만 move 가능
        if (Math.abs(x) > 3 || Math.abs(y) > 3) {
            jo.put("fromServerJsonKey", "3이하로만 이동할 수 있습니다!!");
            writer.println(jo);
            return;
        }

        // 움직이기 전 좌표
        userX = jedis.hget("user:" + name + ":space", "X");
        userY = jedis.hget("user:" + name + ":space", "Y");

        // 벽 넘어서려는 경우 벽에 닿게
        // x 좌표
        if (Integer.parseInt(userX) + x > 29) { // 오른쪽 벽 넘어서는 경우 29에서 멈추도록 처리함.
            jedis.hset("user:" + name + ":space", "X", "29");
        } else if (Integer.parseInt(userX) + x < 0) { // 왼쪽 벽 넘어서는 경우 0에서 멈추도록 처리함.
            jedis.hset("user:" + name + ":space", "X", "0");
        } else { // 정상적인 경우
            jedis.hincrBy("user:" + name + ":space", "X", x);
        }

        // y 좌표
        if (Integer.parseInt(userY) + y > 29) { // 29에서 멈추도록 처리함.
            jedis.hset("user:" + name + ":space", "Y", "29");
        } else if (Integer.parseInt(userY) + y < 0) { // 0에서 멈추도록 처리함.
            jedis.hset("user:" + name + ":space", "Y", "0");
        } else {
            jedis.hincrBy("user:" + name + ":space", "Y", y);
        }

        // 움직인 뒤 좌표
        userX = jedis.hget("user:" + name + ":space", "X");
        userY = jedis.hget("user:" + name + ":space", "Y");

        // jo.put("fromServerJsonKey", "x로 "+x+"만큼 이동, y로 "+y+"만큼 이동\n");
        jo.put("fromServerJsonKey", "<<" + name + " 위치>>\nx좌표: " + userX + ", " + "y좌표: " + userY); // 추가되는게 아니라 이걸로 대체됨
        writer.println(jo);
    }

    public void my() {
        System.out.println(name + " my 실행");
        userX = jedis.hget("user:" + name + ":space", "X");
        userY = jedis.hget("user:" + name + ":space", "Y");
        jo.put("fromServerJsonKey", "<<" + name + " 위치>>\nx좌표: " + userX + ", " + "y좌표: " + userY);
        writer.println(jo);
    }

    public void users() {
        System.out.println(name + " users 실행");
        Set<String> keys = jedis.keys("user:*:hp");
        iter = keys.iterator(); // set을 Iterator 안에 담기
        // 리스트에 유저 이름 담기
        List<String> userList = new ArrayList<String>();
        while (iter.hasNext()) { // iterator에 다음 값이 있다면
            userList.add(iter.next().split(":")[1]); // 이름만 꺼내기
        }
        // 리스트 배열로 변환 -> jsonObject에 넣기 위해
        // https://shlee0882.tistory.com/260 // 배열
        // https://velog.io/@minwest/Java-JSONObject-데이터-List에-저장하기 // 리스트
        String userArr[] = userList.toArray(new String[userList.size()]); // 유저 이름 배열

        // jsonArray 생성
        jArray = new JSONArray();

        for (int i = 0; i < userArr.length; i++) {
            JSONObject json = new JSONObject();
            json.put("name", userArr[i]);

            userX = jedis.hget("user:" + userArr[i] + ":space", "X");
            userY = jedis.hget("user:" + userArr[i] + ":space", "Y");

            json.put("x", userX);
            json.put("y", userY);

            // JsonArray에 json 추가
            jArray.put(json);
        }

        jo.put("users", "dummy");
        writer.println(jo);

        writer.println(jArray);
    }

    public void useHpPotion() {
        System.out.println(name + " useHpPotion 실행");
        // hp 10 증가
        jedis.incrBy("user:" + name + ":hp", 10);
        String userHp = jedis.get("user:" + name + ":hp");
        jo.put("fromServerJsonKey", "hp포션을 사용하였습니다! hp: " + userHp);
        writer.println(jo);
    }

    public void useStrPotion() {
        System.out.println(name + " useStrPotion 실행");
        // 공격력 우선 3 증가 --> 1분뒤 3 감소
        jedis.incrBy("user:" + name + ":str", 3);
        String userStr = jedis.get("user:" + name + ":str");
        jo.put("fromServerJsonKey", "str포션을 사용하였습니다! str: " + userStr);
        writer.println(jo);


        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                jedis.incrBy("user:" + name + ":str", -3);
                String userStr = jedis.get("user:" + name + ":str");
                jo.put("fromServerJsonKey", "str포션 유지 기간이 지났습니다! str: " + userStr);
                writer.println(jo);
            }
        };

        new Timer().schedule(task, 60 * 1000); // 1분 뒤 str 감소
    }

    public void defaults() {
        jo.put("fromServerJsonKey", "없는 명령문 입니다.");
        writer.println(jo);
    }

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
    }


    // 특정 사용자에게 보내는 메시지
    public void sendChat(String user, String message) {
        JSONObject jo = new JSONObject();
        String to = user;
        Object obj = hm.get(to);
        if (obj != null) {
            PrintWriter pw = (PrintWriter) obj;
            jo.put("fromServerJsonKey", name + " 님이 채팅을 보내셨습니다. :" + message);
            pw.println(jo);
            pw.flush();
        } else { // 접속하지 않은 유저인 경우
            jo.put("fromServerJsonKey", "없는 유저 혹은 로그인 하지 않은 유저입니다!!");
            writer.println(jo);
        }
    }
}