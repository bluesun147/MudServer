# 1.	프로젝트 개요 </br>
TCP 네트워킹과 소켓 통신을 이용해 MUD 게임 프로젝트를 JAVA로 진행하였다. TCP는 Transmission Control Protocol의 약자로 연결지향적 프로토콜을 말한다. 연결 지향 프로토콜이란 서버와 클라이언트가 연결되어 있느 상태에서 데이터를 주고 받는 프로토콜을 뜻하는데 이를 이용한 TCP 소켓으로 프로젝트를 진행했다.
TCP 서버에서의 기본적인 함수 호출 순서는 1. 소켓 생성 (socket()), 2. 소켓 주소 할당 (bind()), 3. 연결 요청 대기 상태 (listen()), 4. 연결 허용 (accept()), 5. 데이터 송수신 (send()/recv()), 6. 연결 종료(close()) 순으로 나타낼 수 있다.
TCP 클라이언트에서는 1. 소켓 생성 (socket()), 2. 소켓 주소 할당 (bind()), 3. 연결 요청 대기 상태 (listen()), 4. 연결 허용 (accept()), 5. 데이터 송수신 (send()/recv()), 6. 연결 종료(close()) 순으로 진행된다.
서버와 클라이언트 모두 localhost의 1234번 포트를 통해 통신하도록 하였다.
서버와 클라이언트는 JSON으로 통신을 하는데 JAVA의 org.json 라이브러리가 제공하는 JSONObject, JSONArray 클래스 등을 사용하였다.
데이터의 입력과 전송은 BufferedReader와 PrintWriter를 사용하였다.
사용자 정보는 Redis에 저장하였고 Jedis 라이브러리를 사용하였다.
서버와 클라이언트를 따로 구현하였고 서버 코드는 서버, 몬스터, 명령, 유저 관련 클래스로 역할을 나눠 코드를 작성했다. 클라이언트의 경우 클라이언트 클래스만을 사용하였다.
해당 게임에서 사용할 수 있는 명령어는 다음과 같다.
-	attack : 유저의 상하좌우 9칸에 대해서 몬스터에게 공격을 가한다.
-	bot : 4초에 한번씩 attack, monsters, move, my, users, userHpPition, useStoPotion 명령 중 하나를 시행한다.
-	chat : 현재 접속중인 유저에게 메시지를 전송한다. chat "유저이름" "메시지" 형태로 사용할 수 있다.
-	monsters : 모든 몬스터들의 위치 좌표를 표시한다.
-	move : 현재 좌표에서 주어진 값만큼 이동한다. move x y 형태로 사용한다.
-	my : 나의 위치 좌표를 표시한다.
-	users : 전체 유저의 위치를 표시하다.
-	useHpPotion : 체력 포션을 사용한다.
-	useStrPotion : 힘 포션을 사용한다.
-	quit : 연결을 종료한다.

# 2. 서버 </br>
### 가. Server.java </br>
여러 클라이언트의 접속을 동시에 처리해야 하므로 스레드를 이용해 ServerThread를 구현하였다. HashMap에 유저들의 정보를 저장해 각각을 구분하였다. 서버스레드를 생성할 때 생성자에 hashmap을 넣어주는데 이는 하나의 hashmap을 사용해 사용자들을 서장한다는 의미이다. 클라이언트의 연결이 끊어지는 것은 예외로 처리를 하였고 Timer를 이용해 끊긴 후 5분이 지나면 유저 정보를 삭제하도록 하였다. 하지만 5분 내에 재접속 시 정보를 복원하는 것은 구현하지 못하였다. 중복 접속을 캐치할 수는 있지만 기존 접속을 강제 종료하는 기능도 구현하지 못하였다. 30개의 클라이언트를 동시에 서버에 접속하도록 하여 연결을 처리하는 것을 확인했다. </br> </br>
### 나. Execute.java </br>
명령어 구현과 관련된 메소드들을 구현했다. attack의 경우 hashmap을 이용해 현재 접속중인 모든 클라이언트들에게 공격 메시지를 보내도록 구현하였다. 몬스터의 체력이 0이하가 되었을 때 사망 처리를 구현하였다. 채팅의 경우 받을 사람이 현재 접속중인지 확인하고 그에 해당하는 경우에만 메시지를 전송하도록 구현하였다. </br> </br>
### 다. Monster.java </br>
몬스터 생성과 사망 처리, 정해진 시간마다 몬스터가 스폰되는 기능을 구현하였다. 자동으로 스터가 계속 생성되어 10마리를 채워야 하므로 몬스터의 고유 이름은 UUID로 설정하였다. </br> </br>
### 라. User.java </br>
유저 생성과 삭제 등 유저와 관련한 클래스이다. Redis에 정보를 저장하기 위해 Jedis 라이브러리를 이용했는데 JedisPool pool = new JedisPool("localhost", Protocol.DEFAULT_PORT); 와 같이 Jedis 풀을 만들고 Jedis jedis = pool.getResource(); 로 받아서 사용하는 형식으로 사용했다. 유저의 위치를 배정하는 경우 hash를 사용했다. x값의 경우 다음과 같이 데이터를 저장했다. jedis.hset("user:" + name + ":space", "X", userX); </br> </br>
# 3.	클라이언트  </br>
### 가. Client.java </br>
클라이언트 클래스 내부에 서버로 메시지를 보내는 WritingThread와 서버로 부터 오는 메시지를 읽는 ListeningThread를 따로 구분해 코드를 작성하였다. main 메소드에서 소켓을 통해 서버가 열어둔 포트로 접하고 WritingThread와 ListeningThread를 스타트시킨다. quit 명령을 통해 close 함수를 호출하여 강제로 소켓을 닫아 연결을 종료한다. </br></br>
### 나. WritingThread 클래스 </br>
유저 생성과 PrintWriter에 OutputStream을 넣는 식으로 사용해 데이터를 서버로부터 보낸다. JSONObject.put을 이용해 json 형태의 메시지를 전송한다. </br></br>
### 다. ListeningThread클래스 </br>
BufferedReader에 InputStream을 넣어 서버로부터 넘어오는 데이터를 읽는다. json 형태로 넘어온 데이터에서 "fromServerJsonKey"가 키인 데이터의 값을 읽어 사용한다. while 반복문 내에서 서버로부터 메시지가 넘어올때마다 바로 읽을 수 있다. SocketException이 나는 경우 연결 종료를 띄운다. </br></br>

# 4.	정리  </br>
소켓 통신과 redis에 대해 미숙한 상태에서 프로젝트를 진행함에 있어 기본부터 공부하는 시간이 많이 들었다. C++보다는 JAVA가 익숙했기에 JAVA를 사용해 프로젝트를 진행했다. 달성하지 못한 요구조건이 있어 아쉽지만 소켓 통신에 대해 많이 생각해보고 배울 수 있는 좋은 기회가 되었다. 프로젝트 제출 이후에도 GUI를 구현 한다거나 명령들을 더욱 추가해 실제 즐길 수 있는 게임으로 발전시켜본다면 좋은 공부가 될것이라고 생각한다. </br>

## 사용법  </br>
자바 IDE에서 서버를 실행 시킨 뒤 클라이언트를 실행 해 연결 메시지가 나오면 명령어를 입력한다.  </br>
![image](https://user-images.githubusercontent.com/86697585/230768015-39525157-a3c5-4376-a6cf-1037ca6d4337.png)

 
