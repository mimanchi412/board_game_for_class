# board_game_for_class

## 数据说明
- 用户战绩、积分、等级等统计仅以 `user_stats` 表为准，所有接口也从该表读取。`users` 表中的 `score/win_count/lose_count/level` 为历史遗留字段，不再更新、不用于展示。
- 对局历史与详情接口：
  - `GET /api/game/history/my` 返回当前用户的对局列表（分页）。
  - `GET /api/game/history/{matchId}` 返回指定对局的玩家结果和出牌流水。
- 结算结果、托管/掉线惩罚等逻辑均落库到 `game_match`、`game_match_player`、`game_move` 和 `user_stats`。

## 接口说明

### GET /api/game/history/my
- 作用：获取当前登录用户的历史对局列表（分页）。
- 请求参数（query）：
  - `page`：页码，默认 1
  - `size`：每页条数，默认 20
- 返回字段（PageResult）：
  - `data[]`：
    - `matchId`：对局 ID
    - `landlordUserId`：地主用户 ID
    - `winnerSide`：胜方（`LANDLORD` / `FARMER`）
    - `startTime` / `endTime`：对局起止时间
    - `role`：当前用户在该局的角色（`LANDLORD` / `FARMER`）
    - `result`：胜负（`WIN` / `LOSE`）
    - `scoreDelta`：本局积分变化（含投降/逃跑惩罚）
    - `bombs`：炸弹次数
    - `leftCards`：结束时手牌数
    - `escaped`：是否逃跑/掉线判负
- 示例：
```json
GET /api/game/history/my?page=1&size=10
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "matchId": 5,
      "landlordUserId": 2,
      "winnerSide": "LANDLORD",
      "startTime": "2025-11-21T14:13:48.197517Z",
      "endTime": "2025-11-21T14:16:20.855899Z",
      "role": "FARMER",
      "result": "LOSE",
      "scoreDelta": -20,
      "bombs": 0,
      "leftCards": 17,
      "escaped": false
    }
  ],
  "current": 1,
  "size": 10,
  "total": 1,
  "pages": 1,
  "success": true
}
```

### 认证接口
- `POST /api/auth/register` body: `{username,password,nickname,email,gender,verificationCode}` 注册
- `POST /api/auth/login` body: `{username,password}` -> 返回 `token`
- `POST /api/auth/logout` header: `Authorization: Bearer <token>` 退出并清理 Redis token
- `POST /api/auth/reset-password` body: `{usernameOrEmail,newPassword,verificationCode}` 重置密码
- `POST /api/auth/send-code` body: `{email}` 发送邮箱验证码
示例：
```json
POST /api/auth/login
{ "username": "user1", "password": "123456" }
// 响应 data.token = "Bearer ..."
```

### 房间/匹配接口
- `POST /api/game/rooms/random/join` 加入随机匹配池（可能立即匹配成功）
- `POST /api/game/rooms/custom` body: `{roomName?}` 创建自定义房间
- `POST /api/game/rooms/custom/join` body: `{roomCode}` 通过房间码加入
- `POST /api/game/rooms/{roomId}/ready` body: `{ready:true|false}` 切换准备
- `POST /api/game/rooms/{roomId}/start` 开始游戏
- `GET /api/game/rooms/my` 查询我所在房间
- `GET /api/game/rooms/{roomId}` 查询房间详情
示例：
```json
POST /api/game/rooms/custom
{ "roomName": "测试房间" }
// 响应 data: { "roomId": "...", "roomCode": "123456" }

POST /api/game/rooms/{roomId}/ready
{ "ready": true }
```

### 用户战绩接口
- `GET /api/game/user-stats/me` 当前用户累计战绩
- `GET /api/game/user-stats/{userId}` 指定用户战绩
- `GET /api/game/user-stats/leaderboard?page=1&size=20` 排行榜
- `GET /api/game/user-stats/leaderboard/around-me?radius=5` 以当前用户为中心的排行榜片段
- `POST /api/game/user-stats/batch` body: `{userIds:[1,2,3]}` 批量查询
示例：
```json
GET /api/game/user-stats/me
// 响应 data: { "score": 1020, "winCount": 12, "loseCount": 8, "level": 2, ... }

GET /api/game/user-stats/leaderboard?page=1&size=3
// 响应 data: [ { "userId":1,"score":1300 }, ... ]
```

### 用户资料接口
- `GET /api/user/profile` 当前用户资料
- `PUT /api/user/profile` body: `{nickname,gender,bio,...}` 更新资料
- `PUT /api/user/email` body: `{oldEmail,newEmail,verificationCode}` 更新邮箱
- `POST /api/user/avatar` form-data: `file` 上传头像
示例：
```json
PUT /api/user/profile
{ "nickname": "小明", "gender": 1, "bio": "hello" }
```

### 牌局内 WebSocket/STOMP
- 握手：`/ws`（支持 SockJS），携带 `Authorization: Bearer <token>` 或 query `token=`.
- 订阅：
  - `/topic/rooms/{roomId}` 房间广播（发牌、回合开始、出牌、结果等）
  - `/user/queue/room/{roomId}/cards` 下发手牌
  - `/user/queue/room/{roomId}/snapshot` 快照返回
  - `/user/queue/room/{roomId}/heartbeat` 心跳 ACK
  - `/user/queue/errors` 错误提示（如“当前不是你的回合”）
- 发送：
  - `/app/room/{roomId}/snapshot` 请求快照
  - `/app/room/{roomId}/heartbeat` 心跳
  - `/app/room/{roomId}/surrender` 投降
  - `/app/room/{roomId}/bid` body: `{callLandlord:true|false}`
  - `/app/room/{roomId}/play` body: `{cards:["S3","H3"],pattern:"PAIR"}`
  - `/app/room/{roomId}/pass` 不出
示例发送：
```
CONNECT headers: Authorization=Bearer <token>
SUBSCRIBE /topic/rooms/{roomId}
SEND /app/room/{roomId}/bid  {"callLandlord":true}
SEND /app/room/{roomId}/play {"cards":["S3","H3"],"pattern":"PAIR"}
```

### GET /api/game/history/{matchId}
- 作用：获取指定对局的详细信息（玩家结果 + 出牌流水）。
- 路径参数：
  - `matchId`：对局 ID（数值主键）
- 返回字段：
  - `matchId` / `landlordUserId` / `winnerSide` / `startTime` / `endTime`
  - `remark`：结算备注（含 spring、scoreUnit、bidMultiplier 等 JSON 字符串）
  - `players[]`：
    - `userId` / `seat` / `role` / `result` / `scoreDelta` / `bombs` / `leftCards` / `durationSec` / `escaped`
  - `moves[]`（出牌流水，按 `stepNo` 升序）：
    - `stepNo`：步号
    - `playerId`：出牌人
    - `seat`：座位号（0/1/2）
    - `pattern`：牌型
    - `cards`：牌面列表
    - `beatsPrev`：是否压过上一手
    - `createdAt`：时间戳
- 示例：
```json
GET /api/game/history/5
{
  "code": 200,
  "data": {
    "matchId": 5,
    "landlordUserId": 2,
    "winnerSide": "LANDLORD",
    "startTime": "2025-11-21T14:13:48.197517Z",
    "endTime": "2025-11-21T14:16:20.855899Z",
    "remark": "{\"spring\":true,\"scoreUnit\":20,\"bidMultiplier\":1}",
    "players": [
      { "userId": 1, "seat": 0, "role": "FARMER", "result": "LOSE", "scoreDelta": -20, "bombs": 0, "leftCards": 17, "durationSec": 152, "escaped": false },
      { "userId": 2, "seat": 1, "role": "LANDLORD", "result": "WIN", "scoreDelta": 40, "bombs": 0, "leftCards": 0, "durationSec": 152, "escaped": false },
      { "userId": 3, "seat": 2, "role": "FARMER", "result": "LOSE", "scoreDelta": -20, "bombs": 0, "leftCards": 17, "durationSec": 152, "escaped": false }
    ],
    "moves": [
      { "stepNo": 1, "playerId": 2, "seat": 1, "pattern": "SINGLE", "cards": ["H4"], "beatsPrev": true, "createdAt": "2025-11-21T14:14:15.377483Z" },
      { "stepNo": 2, "playerId": 3, "seat": 2, "pattern": "PASS", "cards": [], "beatsPrev": false, "createdAt": "2025-11-21T14:14:17.448194Z" }
    ]
  },
  "success": true
}
```
