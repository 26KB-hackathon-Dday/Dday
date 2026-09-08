-- 시드 데이터. **앱이 뜰 때마다 실행된다.**
--
-- 로컬에서 띄우면 로컬 MySQL에, 서버에서 띄우면 RDS에 들어간다.
-- 두 DB를 서로 동기화하는 게 아니라, 이 파일 하나에서 양쪽이 각각 채워지는 구조다.
-- 그래서 이 파일이 시드의 정본이고, DB를 날려도 앱만 다시 띄우면 복구된다.
--
-- ── 반드시 지킬 것 ────────────────────────────────────────────────────────
--
-- 1. **멱등하게 쓴다.** 매 기동마다 도니까 그냥 INSERT를 쓰면 재시작할 때마다 중복이 쌓인다.
--    id를 명시적으로 박고 `ON DUPLICATE KEY UPDATE`를 붙인다.
--
--      INSERT INTO pocket (id, name) VALUES (1, '지갑')
--      ON DUPLICATE KEY UPDATE name = VALUES(name);
--
--    이렇게 쓰면 몇 번을 돌려도 결과가 같고, 값을 고쳐서 재기동하면 그 값으로 갱신된다.
--    (수정을 원치 않고 "없을 때만 넣기"면 `INSERT IGNORE INTO ...`)
--
-- 2. **id를 auto-increment에 맡기지 않는다.** 값을 안 박으면 매번 새 행이 생겨
--    멱등성이 깨진다. 시드 데이터는 id를 고정한다.
--
-- 3. **테이블은 엔티티가 만든다.** 여기에 CREATE TABLE을 쓰지 않는다
--    (ddl-auto: update가 담당한다). 여기는 INSERT/UPDATE 전용이다.
--
-- 4. **여기서 실패하면 앱이 안 뜬다** (continue-on-error: false).
--    시드가 조용히 깨진 채로 서비스되는 것보다 낫다는 판단이다.
--
-- 5. **대량 데이터(수만 행 이상)는 여기 넣지 않는다.** 라인 단위로 파싱해서 느리고,
--    매 기동마다 돌고, 저장소가 비대해진다. 그건 별도 적재 스크립트로 한 번만 넣는다.
--
-- ─────────────────────────────────────────────────────────────────────────

-- ⚠️ **이 파일을 주석만 남기고 비우면 앱이 안 뜬다.**
--    스프링이 실행할 문장이 하나도 없는 스크립트를 빈 파일로 보고
--    `IllegalArgumentException: 'script' must not be null or empty`를 던진다.
--    그래서 아래 무해한 한 줄을 남겨둔다. 실제 시드를 넣은 뒤에도 지우지 말 것
--    (전부 지웠다가 이 함정을 다시 밟는다).
SELECT 1;

-- ── 데모 계정 ─────────────────────────────────────────────────────────────
--
-- ⚠️ **이 파일은 운영 RDS에서도 실행된다.** 아래 계정은 비밀번호가 저장소에 공개돼 있으므로
--    배포 서버에도 누구나 로그인할 수 있는 계정이 하나 생긴다. 팀 전원이 같은 화면을 보며
--    데모하기 위해 감수한 선택이다(해커톤 한정).
--    **공개 서비스로 전환할 때는 이 블록과 아래 마이데이터 목데이터를 전부 지운다.**
--
--   ID: user1@test.com  /  PW: test1234!
--
-- user_id를 9001로 박는 이유:
--   1) 시드는 멱등해야 해서 id를 고정해야 한다 (auto-increment에 맡기면 매 기동마다 새 행).
--   2) 값을 크게 잡아 실제 가입자의 auto-increment 구간과 겹치지 않게 한다. 1로 박으면
--      이미 그 id를 쓰는 진짜 회원을 시드가 덮어쓴다.
--
-- ⚠️ **9001이 실제 id라고 가정하면 안 된다.** 이 이메일이 이미 다른 id로 가입돼 있으면
--    (운영 DB에서 실제로 그랬다) ON DUPLICATE KEY UPDATE가 uk_users_email에 걸려
--    그 기존 행을 갱신할 뿐 9001 행은 생기지 않는다. 그래서 아래 시드는 전부
--    id를 박지 않고 **이메일로 조회해서** 붙인다.
--
-- password_hash는 BCrypt라 매번 값이 다르다. 아래 해시는 test1234! 로 로그인되는 것을
-- 확인한 값이며, **다시 만들지 말고 그대로 둔다** (바꾸면 로그인이 깨진다).
INSERT INTO users (
    user_id, email, password_hash, name, phone, status,
    terms_agreed_at, agreed_location, onboarding_completed, mydata_connected,
    mydata_connected_at, mydata_consent_expires_at,
    protection_end_date, region_code, region_name, district_name,
    housing_type, initial_asset, settlement_received,
    created_at, updated_at
) VALUES (
    9001, 'user1@test.com',
    '$2a$10$magcQpOXpje6Wayj0TD8qO7dR9JOya6f7LQGfTXvsFvtrCWic9unG',
    '막스', '010-7777-1111', 'ACTIVE',
    '2026-09-01 00:00:00', 0, 1, 1,
    -- mydata_connected 만으로는 부족하다. MydataSyncWriter가 동의 만료시각까지 보고
    -- (User.hasValidMydataConsent) 지났으면 POST /api/mydata/sync 를 403으로 막는다.
    -- ⚠️ 아래 날짜가 지나면 데모 계정의 동기화가 막힌다. 그때 날짜만 미루면 된다.
    '2026-09-01 00:00:00', '2027-12-31 23:59:59',
    '2025-03-01', '1168000000', '서울특별시', '강남구',
    'MONTHLY', 15000000, 'RECEIVED',
    '2026-09-01 00:00:00', '2026-09-01 00:00:00'
) ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    phone = VALUES(phone),
    status = VALUES(status),
    onboarding_completed = VALUES(onboarding_completed),
    mydata_connected = VALUES(mydata_connected),
    mydata_connected_at = VALUES(mydata_connected_at),
    mydata_consent_expires_at = VALUES(mydata_consent_expires_at),
    protection_end_date = VALUES(protection_end_date),
    region_code = VALUES(region_code),
    region_name = VALUES(region_name),
    district_name = VALUES(district_name),
    housing_type = VALUES(housing_type),
    initial_asset = VALUES(initial_asset),
    settlement_received = VALUES(settlement_received);

-- 온보딩에서 받는 주거비. users.housing_type = MONTHLY 와 앞뒤가 맞아야 한다.
-- estimated_monthly(월 예상 주거비)는 컬럼이 없다 — 월세+관리비로 매번 계산한다.
INSERT INTO housing_cost (user_id, deposit, monthly_rent, maintenance_fee, updated_at)
SELECT u.user_id, 10000000, 450000, 70000, '2026-09-01 00:00:00'
FROM users u WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    deposit = VALUES(deposit),
    monthly_rent = VALUES(monthly_rent),
    maintenance_fee = VALUES(maintenance_fee);

-- 정기수입. 아래 계좌 입금 내역(급여·자립수당)과 금액이 맞아야 화면이 어긋나지 않는다.
INSERT INTO recurring_income (
    recurring_income_id, user_id, income_name, income_type, expected_amount,
    deposit_timing, source_name, auto_match_enabled, created_at, updated_at
)
SELECT 9001, u.user_id, '편의점 아르바이트', 'SALARY', 1200000, '매월 25일', '지에스리테일', 0, '2026-09-01 00:00:00', '2026-09-01 00:00:00'
FROM users u WHERE u.email = 'user1@test.com'
UNION ALL
SELECT 9002, u.user_id, '자립수당', 'ALLOWANCE', 500000, '매월 20일', '강남구청', 0, '2026-09-01 00:00:00', '2026-09-01 00:00:00'
FROM users u WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    income_name = VALUES(income_name),
    income_type = VALUES(income_type),
    expected_amount = VALUES(expected_amount),
    deposit_timing = VALUES(deposit_timing),
    source_name = VALUES(source_name);

-- ── 포켓 ──────────────────────────────────────────────────────────────────
--
-- 데모 계정 몫만 넣는다. 진짜 회원의 포켓은 여전히 가입·온보딩 흐름
-- (POST /api/pockets/initialize)이 만든다.
--
-- 시드로 만든 계정은 그 흐름을 타지 않아 포켓이 없는데, **포켓이 없으면
-- POST /api/mydata/sync 가 POCKET_NOT_FOUND(404)로 막힌다** — 자동분류가 미분류 소비를
-- 넣을 '자유 포켓'을 찾기 때문이다. 그래서 여기서 함께 만들어 둔다.
--
-- 이름과 순서는 PocketService.initialize 와 같아야 한다. 한쪽만 바꾸면
-- 시드로 만든 계정과 가입으로 만든 계정의 화면이 달라진다.
-- pocket_id를 박지 않고 INSERT IGNORE로 넣는다. uk_user_pocket_type(user_id, pocket_type)이
-- 중복을 막아주므로 멱등하고, 고정 id가 실제 회원의 포켓과 부딪힐 일도 없다.
INSERT IGNORE INTO pocket (user_id, pocket_type, pocket_name, created_at)
SELECT u.user_id, t.pocket_type, t.pocket_name, '2026-09-01 00:00:00'
FROM users u
JOIN (
    SELECT 'ESSENTIAL'    AS pocket_type, '필수 포켓'     AS pocket_name
    UNION ALL SELECT 'FREE',         '자유 포켓'
    UNION ALL SELECT 'EMERGENCY',    '비상금 포켓'
    UNION ALL SELECT 'FUTURE_ASSET', '미래자산 포켓'
) t
WHERE u.email = 'user1@test.com';

-- ── 지원제도 (welfare_program) ────────────────────────────────────────────
-- 시드를 두지 않는다. welfare_program은 수집 배치(POST /internal/welfare/collect)가
-- 공공데이터포털에서 긁어와 채운다. 시드 serv_id가 실제 제도 ID와 겹쳐 upsert되면
-- "수집 필드는 진짜 + 매칭 필드는 추측값"인 하이브리드 행이 생겨서 뺐다.
--
-- 매칭 API용 필드(category·support_amount·support_duration_months 등)는 수집기가 아직
-- 안 채우므로 당분간 NULL이다. 채우는 로직은 후속 (docs/welfare-api/matching-spec.md).

-- ── 마이데이터 목데이터 (mock_mydata_*) ───────────────────────────────────
--
-- 실제 마이데이터 대신 우리가 들고 있는 가짜 금융 데이터다. MockMydataClient가
-- **로그인한 회원의 user_id를 그대로 service_user_id로 넘겨** 조회하므로,
-- service_user_id 는 위 데모 계정의 user_id(9001)와 반드시 같아야 한다.
-- (service_user_id 는 users FK가 아니다 — 목 데이터가 실제 회원 테이블을 오염시키지
--  않으려는 설계라, 정합성은 이 파일이 지켜야 한다.)
--
-- org_code: 은행은 표준 금융기관 코드(국민 004 / 신한 088), 카드사는 목값이다.
--           실제 마이데이터 기관코드와 다를 수 있고, 화면 표시에만 쓴다.
--
-- 금액 단위는 원이고 전부 양수다. 입출금 방향은 transaction_type 이 들고 있다.
INSERT INTO mock_mydata_user (mock_user_id, service_user_id, name, created_at, updated_at)
SELECT 9001, u.user_id, '막스', '2026-09-01 00:00:00', '2026-09-01 00:00:00'
FROM users u WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    service_user_id = VALUES(service_user_id),
    name = VALUES(name);

-- 계좌 3개: KB 주거래 + KB 청년적금 + 신한 비상금
INSERT INTO mock_mydata_account (
    mock_account_id, mock_user_id, external_account_id, org_code, account_num,
    account_name, product_name, account_type, balance, available_balance,
    is_active, created_at, updated_at
) VALUES
    (9001, 9001, 'KB-ACC-0001', '004', '110-2345-678901',
     'KB국민 주거래통장', 'KB마이핏통장', 'DEPOSIT',  842000,  842000, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    (9002, 9001, 'KB-ACC-0002', '004', '110-2345-678902',
     'KB국민 청년적금',   'KB청년도약적금', 'SAVINGS', 3600000,       0, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    (9003, 9001, 'SH-ACC-0001', '088', '110-9876-543210',
     '신한 비상금통장',   '신한 쏠편한통장', 'DEPOSIT', 1500000, 1500000, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00')
ON DUPLICATE KEY UPDATE
    org_code = VALUES(org_code),
    account_num = VALUES(account_num),
    account_name = VALUES(account_name),
    product_name = VALUES(product_name),
    account_type = VALUES(account_type),
    balance = VALUES(balance),
    available_balance = VALUES(available_balance),
    is_active = VALUES(is_active);

-- 현대카드 1장 (체크카드 — 자립준비청년이 신용카드를 만들기 어려운 현실을 반영)
INSERT INTO mock_mydata_card (
    mock_card_id, mock_user_id, external_card_id, org_code, card_name,
    card_type, is_active, created_at, updated_at
) VALUES
    (9001, 9001, 'HD-CARD-0001', '0302', '현대카드 ZERO Edition3 (체크)',
     'CHECK', 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00')
ON DUPLICATE KEY UPDATE
    org_code = VALUES(org_code),
    card_name = VALUES(card_name),
    card_type = VALUES(card_type),
    is_active = VALUES(is_active);

-- 계좌 거래내역 — 2026년 8월 한 달 + 9월 진행분.
--
-- 수입(급여 120만 + 자립수당 50만 = 170만)과 지출(월세 45만 + 관리비 7만 + 적금 20만)이
-- 위 recurring_income · housing_cost 값과 맞물린다. 한쪽만 고치면 화면에서 합계가 어긋난다.
--
-- 적금 이체(9006/9016)는 본인 명의 KB 계좌 간 이동이라 TRANSFER 다. 예산 집계에서
-- 지출로 세면 안 되는 돈이고, 그 판정은 FinancialTransaction 쪽이 한다.
INSERT INTO mock_mydata_account_transaction (
    mock_transaction_id, mock_account_id, external_transaction_id, transaction_at,
    transaction_type, amount, counterparty_name, counterparty_account_num,
    merchant_name, merchant_regno, trans_memo, transaction_status,
    created_at, updated_at
) VALUES
    -- 8월 KB 주거래
    (9001, 9001, 'KB-TX-202608-01', '2026-08-20 09:12:00', 'INCOME',   500000, '강남구청',     NULL, NULL, NULL, '자립수당',       'NORMAL', '2026-08-20 09:12:00', '2026-08-20 09:12:00'),
    (9002, 9001, 'KB-TX-202608-02', '2026-08-25 10:03:00', 'INCOME',  1200000, '지에스리테일', NULL, NULL, NULL, '8월 급여',       'NORMAL', '2026-08-25 10:03:00', '2026-08-25 10:03:00'),
    (9003, 9001, 'KB-TX-202608-03', '2026-08-25 11:00:00', 'EXPENSE',  450000, '김임대',       '110-1111-222233', NULL, NULL, '8월 월세',   'NORMAL', '2026-08-25 11:00:00', '2026-08-25 11:00:00'),
    (9004, 9001, 'KB-TX-202608-04', '2026-08-26 08:30:00', 'EXPENSE',   70000, '한국전력공사', NULL, NULL, NULL, '관리비',         'NORMAL', '2026-08-26 08:30:00', '2026-08-26 08:30:00'),
    (9005, 9001, 'KB-TX-202608-05', '2026-08-26 08:35:00', 'EXPENSE',   38500, 'SK텔레콤',     NULL, NULL, NULL, '통신비',         'NORMAL', '2026-08-26 08:35:00', '2026-08-26 08:35:00'),
    (9006, 9001, 'KB-TX-202608-06', '2026-08-26 09:00:00', 'TRANSFER', 200000, '본인',         '110-2345-678902', NULL, NULL, '청년적금 자동이체', 'NORMAL', '2026-08-26 09:00:00', '2026-08-26 09:00:00'),
    -- 9월 KB 주거래 (진행 중)
    (9011, 9001, 'KB-TX-202609-01', '2026-09-01 07:40:00', 'EXPENSE',   12900, '넷플릭스',     NULL, '넷플릭스', NULL, '구독료',   'NORMAL', '2026-09-01 07:40:00', '2026-09-01 07:40:00'),
    (9012, 9001, 'KB-TX-202609-02', '2026-09-05 19:22:00', 'EXPENSE',   35000, '이친구',       '088-3333-444455', NULL, NULL, '회비',     'NORMAL', '2026-09-05 19:22:00', '2026-09-05 19:22:00'),
    (9013, 9001, 'KB-TX-202609-03', '2026-09-06 13:10:00', 'INCOME',    30000, '이친구',       '088-3333-444455', NULL, NULL, '정산 입금', 'NORMAL', '2026-09-06 13:10:00', '2026-09-06 13:10:00'),
    -- 신한 비상금 — 거의 안 건드리는 계좌라 이자만 붙는다
    (9021, 9003, 'SH-TX-202608-01', '2026-08-31 23:50:00', 'INCOME',     1250, '신한은행',     NULL, NULL, NULL, '이자',     'NORMAL', '2026-08-31 23:50:00', '2026-08-31 23:50:00'),
    -- 적금 계좌로 들어온 자동이체 (위 9006의 상대편)
    (9031, 9002, 'KB-TX-202608-06-IN', '2026-08-26 09:00:00', 'TRANSFER', 200000, '본인', '110-2345-678901', NULL, NULL, '청년적금 납입', 'NORMAL', '2026-08-26 09:00:00', '2026-08-26 09:00:00')
ON DUPLICATE KEY UPDATE
    transaction_at = VALUES(transaction_at),
    transaction_type = VALUES(transaction_type),
    amount = VALUES(amount),
    counterparty_name = VALUES(counterparty_name),
    counterparty_account_num = VALUES(counterparty_account_num),
    trans_memo = VALUES(trans_memo),
    transaction_status = VALUES(transaction_status);

-- 카드 거래내역 (현대카드 체크). 소비 분류·포켓 화면이 읽는 실제 소비 데이터다.
--
-- 9107은 취소 거래다. transaction_status = CANCELED 이고 original_transaction_id 로
-- 원거래(9106)를 가리킨다 — 행을 지우지 않고 이어붙이는 게 우리 규칙이다.
-- 취소된 건은 예산 소진액에서 빠져야 하므로 화면 합계를 볼 때 주의한다.
INSERT INTO mock_mydata_card_transaction (
    mock_card_transaction_id, mock_card_id, external_transaction_id, transaction_at,
    amount, merchant_name, merchant_regno, transaction_status, original_transaction_id,
    created_at, updated_at
) VALUES
    (9101, 9001, 'HD-TX-202609-01', '2026-09-01 08:15:00',  4500, '스타벅스 역삼점',   '1208800000', 'NORMAL',   NULL, '2026-09-01 08:15:00', '2026-09-01 08:15:00'),
    (9102, 9001, 'HD-TX-202609-02', '2026-09-01 12:40:00',  9000, '김밥천국 강남점',   '2201234567', 'NORMAL',   NULL, '2026-09-01 12:40:00', '2026-09-01 12:40:00'),
    (9103, 9001, 'HD-TX-202609-03', '2026-09-02 20:05:00', 23400, '이마트24 논현점',   '3302345678', 'NORMAL',   NULL, '2026-09-02 20:05:00', '2026-09-02 20:05:00'),
    (9104, 9001, 'HD-TX-202609-04', '2026-09-03 09:02:00',  1450, '서울교통공사',      '1108600000', 'NORMAL',   NULL, '2026-09-03 09:02:00', '2026-09-03 09:02:00'),
    (9105, 9001, 'HD-TX-202609-05', '2026-09-04 19:30:00', 32000, '올리브영 강남',     '2208765432', 'NORMAL',   NULL, '2026-09-04 19:30:00', '2026-09-04 19:30:00'),
    (9106, 9001, 'HD-TX-202609-06', '2026-09-05 14:20:00', 89000, '무신사 스토어',     '1048100000', 'NORMAL',   NULL, '2026-09-05 14:20:00', '2026-09-05 14:20:00'),
    (9107, 9001, 'HD-TX-202609-07', '2026-09-06 10:11:00', 89000, '무신사 스토어',     '1048100000', 'CANCELED', 'HD-TX-202609-06', '2026-09-06 10:11:00', '2026-09-06 10:11:00'),
    (9108, 9001, 'HD-TX-202609-08', '2026-09-06 18:45:00', 12800, '배달의민족',        '1208147521', 'NORMAL',   NULL, '2026-09-06 18:45:00', '2026-09-06 18:45:00'),
    (9109, 9001, 'HD-TX-202609-09', '2026-09-07 11:30:00',  6800, '투썸플레이스',      '1048164000', 'NORMAL',   NULL, '2026-09-07 11:30:00', '2026-09-07 11:30:00'),
    (9110, 9001, 'HD-TX-202609-10', '2026-09-08 08:20:00',  1450, '서울교통공사',      '1108600000', 'NORMAL',   NULL, '2026-09-08 08:20:00', '2026-09-08 08:20:00')
ON DUPLICATE KEY UPDATE
    transaction_at = VALUES(transaction_at),
    amount = VALUES(amount),
    merchant_name = VALUES(merchant_name),
    merchant_regno = VALUES(merchant_regno),
    transaction_status = VALUES(transaction_status),
    original_transaction_id = VALUES(original_transaction_id);
