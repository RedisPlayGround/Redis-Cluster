# Redis-Cluster

### 확장성이란?

- 소프트웨어나 서비스의 요구사항 수준이 증가할 때 대응할 수 있는 능력
- 주로 규모에 대한 확장성을 뜻함(데이터의 크기, 요청 트래픽 등)
- 수직 확장(scale-up)과 수평 확장(scale-out)이 사용됨

![image](https://user-images.githubusercontent.com/40031858/221726078-a760f8c8-de49-49c4-8fa0-70c588216f0b.png)

#### 수직 확장(scale--out)
- 처리 요소(ex: 서버) 를 여러개 두어서 작업을 분산
- 무중단 확장이 가능
- 이론적으로는 무한대로 확장이 가능

![image](https://user-images.githubusercontent.com/40031858/221726581-a294a516-efd8-45d5-bf6d-0fee75f75c42.png)

`분산 시스템에 따라오는 문제`
- 부분 장애
- 네트워크 실패
- 데이터 동기화
- 로드밸런싱(또는 discovery)
- 개발 및 관리의 복잡성

`분산 시스템의 적용`
- 분산 시스템으로 인한 trade-off를 판단해서 적합하다면 사용
- 서비스의 복잡도와 규모의 증가로 분산은 피할 수 없는 선택
- 분산 시스템의 구현체들은 세부적인 부분에서 튜닝이 가능하게 옵션이 제공됨 

    (즉,  분산 시스템의 장단점을 세부적으로 조절 가능)


---

# Redis Cluster

- Redis Cluster가 제공하는 것
  - 여러 노드에 자동적인 데이터 분산
  - 일부 노드의 실패나 통신 단절에도 계속 작동하는 가용성
  - 고성능을 보장하면서 선형 확장성을 제공

### Redis Cluster 특징

- full-mesh 구조로 통신
- cluster bus라는 추가 채널(port) 사용
- gossip protocol 사용
- hash slot을 사용한 키 관리
- DB0만 사용 가능
- multi key 명령어가 제한됨
- 클라이언트는 모든 노드에 접속

![image](https://user-images.githubusercontent.com/40031858/221729107-4ce25f21-901c-4317-b122-5286eda1f186.png)

### Sentinel과의 차이점

- 클러스터는 데이터 분산(샤딩)을 제공함
- 클러스터는 자동 장애조치를 위한 모니터링 노드(Sentinel)를 추가 배치할 필요가 없음
- 클러스터에서는 multi key 오퍼레이션이 제한됨
- Sentinel은 비교적 단순하고 소규모의 시스템에서 HA(고가용성)가 필요할 때 채택

----

## 데이터 분산과 Key 관리


### 데이터를 분산하는 기준 

- 특정 key의 데이터가 어느 노드(shard)에 속할 것인지 결정하는 메커니즘이 있어야 함
- 보통 분산 시스템에서 해싱이 사용됨
- 단순 해싱으로는 노드의 개수가 변할 때 모든 매핑이 새로 계산되어야 하는 문제가 있음

<img width="986" alt="image" src="https://user-images.githubusercontent.com/40031858/224210414-90ef3d55-bf62-4bc0-a6a9-1c38149dec10.png">

### Hash Slot을 이용한 데이터 분산

- Redis는 16384개의 hash slot으로 key 공간을 나누어 관리
- 각 키는 CRC16 해싱 후 16384로 modulo 연산을 해 각 hash slot에 매핑
- hash slot은 각 노드들에게 나누어 분배됨

<img width="932" alt="image" src="https://user-images.githubusercontent.com/40031858/224210914-be1998d0-30fc-4ffa-9a03-3962169d9acf.png">

### 클라이언트의 데이터 접근

- 클러스터 노드는 요청이 온 key에 해당하는 노드로 자동 redirect를 해주지 않음
- 클라이언트는 MOVED 에러를 받으면 해당 노드로 다시 요청해야함

<img width="1048" alt="image" src="https://user-images.githubusercontent.com/40031858/224211078-b1606634-ec6c-42a3-994d-461a2d5b2e86.png">


---

## 성능과 가용성

### 클러스터를 사용할 때의 성능

- 클라이언트가 MOVED 에러에 대해 재 요청을 해야하는 문제
  - 클라이언트(라이브러리)는 key-node 맵을 캐싱하므로 대부분의 경우 발생하지 않음
- 클라이언트는 단일 인스턴스의 Redis를 이용할 때와 같은 성능으로 이용 가능
- 분산 시스템에서 성능은 데이터 일관성(consistency)과 trade-off가 있음
  - Redis Cluster는 고성능의 확장성을 제공하면서 적절한 수준의 데이터 안정성과 가용성을 유지하는 것을 목표로 설계됨

### 클러스터의 데이터 일관성

- Redis Cluster는 strong consistency를 제공하지 않음
- 높은 성능을 위해 비동기 복제를 하기 때문

<img width="1046" alt="image" src="https://user-images.githubusercontent.com/40031858/224212127-a594f97e-4912-41fd-8a4b-e27ae961f323.png">


### 클러스터의 가용성 - auto failover

- 일부 노드(master)가 실패(또는 네트워크 단절) 하더라도 과반수 이상의 master가 남아있고 사라진 master의 replica들이 있다면 클러스터는 failover되어 가용한 상태가 된다
- node timeout동안 과반수의 master와 통신하지 못한 master는 스스로 error state로 빠지고 write 요청을 받지 않음


  예) master1과 replica2가 죽더라도, 2/3의 master가 남아있고, master1이 커버하던 hash slot은 replica1이 master로 승격되어 커버할 수 있다

<img width="434" alt="image" src="https://user-images.githubusercontent.com/40031858/224212999-f1da0b73-c83a-41e2-86d6-6fa8bb567594.png">

### 클러스터의 가용성 - replica migration

- replica가 다른 master로 migrate해서 가용성을 높인다

<img width="954" alt="image" src="https://user-images.githubusercontent.com/40031858/224213065-7a4d1e2d-3663-444a-b6f8-cc6b20af5a6e.png">

---

## 클러스터의 제약 사항

### 클러스터에서는 DB0만 사용 가능

- Redis는 한 인스턴스에 여러 데이터베이스를 가질 수 있으며 디폴트는 16
  - 설정) databases 16
- Multi DB는 용도별로 분리해서 관리를 용이하게 하기 위한 목적
- 클러스터에서는 해당 기능을 사용할 수 없고 DB0으로 고정된다

### Multi key operation 사용의 제약

- key들이 각각 다른 노드에 저장되므로 MSET과 같ㅇ느 multi-key operation은 기본적으로 사용할 수 없다
- 같은 노드 안에 속한 key들에 대해서는 multi-key operation이 가능
- hash tags 기능을 사용하면 여러 key들을 같은 hash slot에 속하게 할 수 있음
  - key 값 중 {} 안에 들어간 문자열에 대해서만 해싱을 수행하는 원리

```markdown
MSET {user:a}:age 20 {user:a} city seoul
```

### 클라이언트 구현의 강제

- 클라이언트는 클러스터의 모든 노드에 접속해야 함
- 클라이언트는 redirect 기능을 구현해야함(MOVED 에러의 대응)
- 클라이언트 구현이 잘 된 라이브러리가 없는 환경도 있을 수 있음



---

# 클러스터 구성 실습

## 클러스터 설정 파일 이해하기

- cluster-enabled < yes/no> : 클러스터 모드로 실행할지 여부를 결정
- cluster-config-file < filename> : 해당 노드의 클러스터를 유지하기 위한 설정을 저장하는 파일로, 사용자가 수정하지 않음
- cluster-node-timeout < milliseconds>
  - 특정 노드가 정상이 아닌 것으로 판단하는 기준 시간
  - 이 시간동안 감지되지 않는 master는 replica에 의해 failover가 이루어짐.
- cluster-replica-validity-factor < factor>
  - master와 통신한지 오래된 replica가 failover를 수행하지 않게 하기 위한 설정
  - (cluster-node-timeout * factor) 만큼 master와 통신이 없었던 replica는 failover 대상에서 제외된다
- cluster-migration-barrier < count> : 
  - 한 master가 유지해야 하는 최소 replica의 개수
  - 이 개수를 충족하는 선에서 일부 replica는 replica를 가지지 않은 master의 replica로 migrate 될 수 있다.
- cluster-require-full-coverage < yes/no> :
  - 일부 hash slot이 커버되지 않을 때 write 요청을 받지 않을까 여부
  - no로 설정하게 되면 일부 노드에 장애가 생겨 해당 hash slot이 정상 작동하지 않더라도 나머지 hash slot에 대해서는 작동하도록 할 수 있다
- cluster-allow-reads-when-down < yes/no> :
  - 클러스가 정상 상태가 아닐 때도 read 요청은 받도록 할지 여부
  - 어플리케이션에서 read 동작의 consistency가 중요치 않은 경우에 yes로 설정할 수 있다


레디스 conf 파일의 포트를 변경 후 cluster 모드를 키고 실행한다

<img width="743" alt="image" src="https://user-images.githubusercontent.com/40031858/224523582-8a5cf127-bf5d-4cef-9a17-94fbea13342a.png">

총 6개의 레디스를 띄웠으며 (7000~ 7005)

```bash
redis-cli --cluster create localhost:7000 localhost:7001 localhost:7002 localhost:7003 localhost:7004 localhost:7005 --cluster-replicas 1
```

<img width="691" alt="image" src="https://user-images.githubusercontent.com/40031858/224523659-3cf3ac72-9fc9-4586-8651-d94cbf6123fc.png">

<img width="933" alt="image" src="https://user-images.githubusercontent.com/40031858/224523696-5f9d8348-600c-40e8-81cc-e4e66c2de1fc.png">



값에 대한 수정은 replica에서 할 수 없고 replica에서는 readonly 명령어를 통해 값을 읽기만 가능하도록 설정할 수 있다.

<img width="1713" alt="image" src="https://user-images.githubusercontent.com/40031858/224523803-e55e5dc5-e1ae-4777-8b1a-15ecdada51f8.png">

7001번 레디스를 종료할 경우 failover가 일어난다.

<img width="1682" alt="image" src="https://user-images.githubusercontent.com/40031858/224523846-0c2e2923-dd6f-4b88-bddb-ee839c3612cd.png">

새로운 레디스를 띄운 후 클러스터에 추가하려면 다음과 같이 하면된다

```bash
redis-cli --cluster add-node localhost:7006 localhost:7001
```

<img width="783" alt="image" src="https://user-images.githubusercontent.com/40031858/224524280-6343615c-f8bd-44d9-a571-b22b2028d736.png">

add node를 할경우 master로 추가되는데 slave로 추가하고 싶다면 다음과 같이 할 수 있다

```bash
redis-cli --cluster add-node localhost:7007 localhost:7006 --cluster-slave
```

<img width="921" alt="image" src="https://user-images.githubusercontent.com/40031858/224524376-18190537-b00c-4d1f-81b9-b21c9967ec02.png">