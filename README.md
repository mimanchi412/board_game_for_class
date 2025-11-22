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
