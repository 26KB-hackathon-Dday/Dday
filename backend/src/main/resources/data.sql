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
-- Spring은 application.yml의 UTF-8 설정으로 읽지만, mysql CLI로 직접 적용할 때도
-- 한글을 같은 문자셋으로 해석하도록 세션 문자셋을 명시한다.
SET NAMES utf8mb4;
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

-- 온보딩을 마치지 않은 데모 계정. 회원가입(약관 동의)까지만 하고 멈춘 상태를 재현한다
-- (housing_type·region_code·초기자산 등은 온보딩 단계에서 받는 값이라 전부 NULL).
-- password_hash는 user1과 동일한 해시를 그대로 써서 같은 비밀번호(test1234!)로 로그인된다.
--
--   ID: user2@test.com  /  PW: test1234!
INSERT INTO users (
    user_id, email, password_hash, name, phone, status,
    terms_agreed_at, agreed_location, onboarding_completed, mydata_connected,
    created_at, updated_at
) VALUES (
    9002, 'user2@test.com',
    '$2a$10$magcQpOXpje6Wayj0TD8qO7dR9JOya6f7LQGfTXvsFvtrCWic9unG',
    '온보딩전', '010-7777-2222', 'ACTIVE',
    '2026-09-05 00:00:00', 0, 0, 0,
    '2026-09-05 00:00:00', '2026-09-05 00:00:00'
) ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    phone = VALUES(phone),
    status = VALUES(status),
    onboarding_completed = VALUES(onboarding_completed),
    mydata_connected = VALUES(mydata_connected);

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
-- IGNORE가 아니라 ON DUPLICATE KEY UPDATE를 쓴다. IGNORE로 두면 이미 들어간 행을 절대
-- 고치지 못해서, 한 번 잘못 들어간 이름(인코딩 사고 등)이 영영 남는다.
-- 중복 판정은 uk_user_pocket_type(user_id, pocket_type)이 한다.
INSERT INTO pocket (user_id, pocket_type, pocket_name, created_at)
SELECT u.user_id, t.pocket_type, t.pocket_name, '2026-09-01 00:00:00'
FROM users u
JOIN (
    SELECT 'ESSENTIAL'    AS pocket_type, '필수 포켓'     AS pocket_name
    UNION ALL SELECT 'FREE',         '자유 포켓'
    UNION ALL SELECT 'EMERGENCY',    '비상금 포켓'
    UNION ALL SELECT 'FUTURE_ASSET', '미래자산 포켓'
) t
WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE pocket_name = VALUES(pocket_name);

-- ── 지원제도 (welfare_program) ────────────────────────────────────────────
--
-- 원래는 수집 배치(POST /internal/welfare/collect)가 공공데이터포털에서 긁어와 채운다.
-- 그런데 운영에 WELFARE_API_KEY 가 없어 수집이 한 번도 돌지 않았고, 지원금 화면이
-- 빈 채로 남아 시연을 못 한다. 그래서 **시연용 최소 세트**를 넣는다.
--
-- ⚠️ serv_id 를 'DEMO-' 로 시작하게 잡았다. 실제 제도 ID(숫자)와 절대 겹치지 않게 해서,
--    나중에 수집기가 돌아도 진짜 데이터를 덮어쓰지 않는다. 수집이 정상화되면 이 블록을 지운다.
--
-- source = MANUAL_CURATION 이어야 목록에 뜬다. 조회 쿼리가 NEEDS_REVIEW 인 수집 후보를
-- 걸러내기 때문이다 (WelfareProgramRepository.search).
INSERT INTO welfare_program (
    id, serv_id, agency_type, source, serv_nm, serv_dgst, jur_mnof_nm,
    category, target_description, protection_phase,
    support_type, support_amount, support_amount_type, support_duration_months,
    application_deadline, ongoing_application,
    apply_channel_name, apply_channel_url, apply_channel_phone,
    required_documents, youth_status, curation_status, region_code,
    collected_at, created_at, updated_at
) VALUES
    ('11111111-1111-4111-8111-000000000001', 'DEMO-0001', 'CENTRAL', 'MANUAL_CURATION',
     '자립수당', '보호종료 후 5년간 매월 지급되는 자립수당입니다.', '보건복지부',
     '생계', '아동복지시설·가정위탁 보호가 종료된 지 5년 이내인 청년', 'POST_TERMINATION',
     'CASH', 500000, 'MONTHLY', 60,
     NULL, 1, '주민센터', 'https://www.bokjiro.go.kr', '129',
     '신분증, 보호종료확인서, 통장사본', 'STRONG_YOUTH', 'OK', NULL,
     '2026-09-01 00:00:00', '2026-09-01 00:00:00', '2026-09-01 00:00:00'),

    ('11111111-1111-4111-8111-000000000002', 'DEMO-0002', 'CENTRAL', 'MANUAL_CURATION',
     '자립정착금', '보호종료 시 한 번 지급되는 정착 지원금입니다.', '보건복지부',
     '생계', '보호가 종료되는 자립준비청년', 'PRE_TERMINATION',
     'CASH', 10000000, 'FIXED', NULL,
     NULL, 1, '지자체 아동복지팀', 'https://www.bokjiro.go.kr', '129',
     '신분증, 보호종료확인서', 'STRONG_YOUTH', 'OK', NULL,
     '2026-09-01 00:00:00', '2026-09-01 00:00:00', '2026-09-01 00:00:00'),

    ('11111111-1111-4111-8111-000000000003', 'DEMO-0003', 'CENTRAL', 'MANUAL_CURATION',
     '청년월세 특별지원', '월 최대 20만원의 월세를 최대 12개월 지원합니다.', '국토교통부',
     '주거', '만 19~34세 무주택 청년 중 소득·재산 기준을 충족하는 사람', 'BOTH',
     'CASH', 200000, 'MONTHLY', 12,
     '2026-12-31', 0, '복지로', 'https://www.bokjiro.go.kr', '1600-0777',
     '임대차계약서, 통장사본, 소득증빙', 'AUTO_APPROVED', 'OK', NULL,
     '2026-09-01 00:00:00', '2026-09-01 00:00:00', '2026-09-01 00:00:00'),

    ('11111111-1111-4111-8111-000000000004', 'DEMO-0004', 'CENTRAL', 'MANUAL_CURATION',
     '디딤씨앗통장', '본인이 적립하면 정부가 같은 금액을 매칭 적립합니다.', '보건복지부',
     '자산형성', '보호대상아동 및 보호종료 5년 이내 청년', 'BOTH',
     'CASH', 100000, 'MONTHLY', 60,
     NULL, 1, '주민센터', 'https://www.bokjiro.go.kr', '129',
     '신분증, 통장사본', 'STRONG_YOUTH', 'OK', NULL,
     '2026-09-01 00:00:00', '2026-09-01 00:00:00', '2026-09-01 00:00:00'),

    ('11111111-1111-4111-8111-000000000005', 'DEMO-0005', 'LOCAL', 'MANUAL_CURATION',
     '서울시 자립준비청년 주거지원', '전세보증금 이자를 지원합니다.', '서울특별시',
     '주거', '서울에 거주하는 자립준비청년', 'POST_TERMINATION',
     'LOAN', 50000000, 'LIMIT', NULL,
     '2026-11-30', 0, '서울주거포털', 'https://housing.seoul.go.kr', '02-120',
     '임대차계약서, 보호종료확인서', 'STRONG_YOUTH', 'OK', '11',
     '2026-09-01 00:00:00', '2026-09-01 00:00:00', '2026-09-01 00:00:00'),

    ('11111111-1111-4111-8111-000000000006', 'DEMO-0006', 'CENTRAL', 'MANUAL_CURATION',
     '자립준비청년 심리정서 지원', '전문 상담을 무료로 받을 수 있습니다.', '보건복지부',
     '의료', '보호종료 5년 이내 자립준비청년', 'POST_TERMINATION',
     'SERVICE', NULL, NULL, NULL,
     NULL, 1, '아동권리보장원', 'https://www.ncrc.or.kr', '02-6454-8500',
     '신분증', 'STRONG_YOUTH', 'OK', NULL,
     '2026-09-01 00:00:00', '2026-09-01 00:00:00', '2026-09-01 00:00:00')
ON DUPLICATE KEY UPDATE
    serv_nm = VALUES(serv_nm),
    serv_dgst = VALUES(serv_dgst),
    category = VALUES(category),
    target_description = VALUES(target_description),
    protection_phase = VALUES(protection_phase),
    support_type = VALUES(support_type),
    support_amount = VALUES(support_amount),
    support_amount_type = VALUES(support_amount_type),
    support_duration_months = VALUES(support_duration_months),
    application_deadline = VALUES(application_deadline),
    ongoing_application = VALUES(ongoing_application),
    apply_channel_name = VALUES(apply_channel_name),
    apply_channel_url = VALUES(apply_channel_url),
    apply_channel_phone = VALUES(apply_channel_phone),
    required_documents = VALUES(required_documents),
    youth_status = VALUES(youth_status),
    curation_status = VALUES(curation_status),
    source = VALUES(source);

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
-- 대출 계좌는 balance가 남은 원금이고 interest_rate에 연 이자율이 들어간다.
-- 예금·적금은 이자율이 NULL이다.
--
-- ⚠️ 새 행은 현재 AUTO_INCREMENT보다 큰 id를 써야 한다 (mock_mydata_card와 같은 이유).
-- MockMydataProvisioner가 연동 때마다 행을 만들어 낮은 번호를 소진한다.
INSERT INTO mock_mydata_account (
    mock_account_id, mock_user_id, external_account_id, org_code, account_num,
    account_name, product_name, account_type, balance, available_balance,
    interest_rate, is_active, created_at, updated_at
) VALUES
    (9001, 9001, 'KB-ACC-0001', '004', '110-2345-678901',
     'KB국민 주거래통장', 'KB마이핏통장', 'DEPOSIT',  842000,  842000, NULL, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    (9002, 9001, 'KB-ACC-0002', '004', '110-2345-678902',
     'KB국민 청년적금',   'KB청년도약적금', 'SAVINGS', 3600000,       0, NULL, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    (9003, 9001, 'SH-ACC-0001', '088', '110-9876-543210',
     '신한 비상금통장',   '신한 쏠편한통장', 'DEPOSIT', 1500000, 1500000, NULL, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    -- 제1금융권 대출 한 건, 제2금융권 대출 한 건. 같은 금액이라도 어디서 빌렸는지가
    -- 점수를 가르므로 두 권역이 다 있어야 화면이 의미를 가진다.
    (99001, 9001, 'SH-LOAN-0001', '088', '110-9876-500001',
     '신한 신용대출',     '쏠편한 직장인대출', 'LOAN', 8000000, NULL, 5.40, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    (99002, 9001, 'HC-LOAN-0001', '0602', '620-1234-500002',
     '현대캐피탈 신용대출', '현대캐피탈 다이렉트론', 'LOAN', 3000000, NULL, 15.40, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00')
ON DUPLICATE KEY UPDATE
    org_code = VALUES(org_code),
    account_num = VALUES(account_num),
    account_name = VALUES(account_name),
    product_name = VALUES(product_name),
    account_type = VALUES(account_type),
    balance = VALUES(balance),
    available_balance = VALUES(available_balance),
    interest_rate = VALUES(interest_rate),
    is_active = VALUES(is_active);

-- 현대카드 1장 (체크카드 — 자립준비청년이 신용카드를 만들기 어려운 현실을 반영)
-- 체크카드는 한도가 없어 credit_limit이 NULL이다. 신용카드만 이용률을 낼 수 있다.
--
-- ⚠️ 새 행을 추가할 때는 **현재 AUTO_INCREMENT보다 큰 id**를 써야 한다.
-- MockMydataProvisioner가 연동 때마다 이 테이블에 행을 만들어 id를 소진하므로,
-- 9003 같은 낮은 번호를 쓰면 이미 생성된 다른 회원의 카드를 덮어쓴다
-- (ON DUPLICATE KEY UPDATE가 소유자는 그대로 두고 이름·한도만 바꿔버린다).
-- 그래서 아래 두 카드는 99003·99004를 쓴다.
INSERT INTO mock_mydata_card (
    mock_card_id, mock_user_id, external_card_id, org_code, card_name,
    card_type, credit_limit, is_active, created_at, updated_at
) VALUES
    (9001, 9001, 'HD-CARD-0001', '0302', '현대카드 ZERO Edition3 (체크)',
     'CHECK', NULL, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    (9002, 9001, 'SH-CARD-0001', '0306', '신한카드 Deep Dream',
     'CREDIT', 3000000, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    (99003, 9001, 'KB-CARD-0001', '0301', 'KB국민 탄탄대로 카드',
     'CREDIT', 1000000, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    (99004, 9001, 'WR-CARD-0001', '0313', '우리카드 카드의정석',
     'CREDIT', 1500000, 1, '2026-09-01 00:00:00', '2026-09-01 00:00:00')
ON DUPLICATE KEY UPDATE
    org_code = VALUES(org_code),
    card_name = VALUES(card_name),
    card_type = VALUES(card_type),
    credit_limit = VALUES(credit_limit),
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
    (9004, 9001, 'KB-TX-202608-04', '2026-08-26 08:30:00', 'EXPENSE',   70000, '한국전력공사', NULL, NULL, NULL, '관리비',         'NORMAL', '2026-08-26 08:30:00', '2026-08-26 08:30:00'),
    (9005, 9001, 'KB-TX-202608-05', '2026-08-26 08:35:00', 'EXPENSE',   38500, 'SK텔레콤',     NULL, NULL, NULL, '통신비',         'NORMAL', '2026-08-26 08:35:00', '2026-08-26 08:35:00'),
    (9006, 9001, 'KB-TX-202608-06', '2026-08-26 09:00:00', 'TRANSFER', 200000, '본인',         '110-2345-678902', NULL, NULL, '청년적금 자동이체', 'NORMAL', '2026-08-26 09:00:00', '2026-08-26 09:00:00'),
    -- 9월 KB 주거래 (진행 중)
    (9011, 9001, 'KB-TX-202609-01', '2026-09-01 07:40:00', 'EXPENSE',   12900, '넷플릭스',     NULL, '넷플릭스', NULL, '구독료',   'NORMAL', '2026-09-01 07:40:00', '2026-09-01 07:40:00'),
    (9012, 9001, 'KB-TX-202609-02', '2026-09-05 19:22:00', 'EXPENSE',   35000, '이친구',       '088-3333-444455', NULL, NULL, '회비',     'NORMAL', '2026-09-05 19:22:00', '2026-09-05 19:22:00'),
    (9013, 9001, 'KB-TX-202609-03', '2026-09-06 13:10:00', 'INCOME',    30000, '이친구',       '088-3333-444455', NULL, NULL, '정산 입금', 'NORMAL', '2026-09-06 13:10:00', '2026-09-06 13:10:00'),
    (9014, 9001, 'KB-TX-202609-04', '2026-09-08 09:00:00', 'EXPENSE',  450000, 'LH청년행복주택', NULL, 'LH청년행복주택', '1298200036', '9월 월세', 'NORMAL', '2026-09-08 09:00:00', '2026-09-08 09:00:00'),
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
    (9110, 9001, 'HD-TX-202609-10', '2026-09-08 08:20:00',  1450, '서울교통공사',      '1108600000', 'NORMAL',   NULL, '2026-09-08 08:20:00', '2026-09-08 08:20:00'),
    -- 카테고리별로 자립준비청년이 실제로 쓸 법한 소비를 더 채운 8월 내역
    -- (식비·교통비·편의점·쇼핑·문화·의료·교육 등 카테고리를 고루 채워 화면에서 분류가 비어 보이지 않게 한다)
    (9111, 9001, 'HD-TX-202608-11', '2026-08-16 12:20:00',  8900, '본죽앤비빔밥 강남점', '2211112222', 'NORMAL', NULL, '2026-08-16 12:20:00', '2026-08-16 12:20:00'),
    (9112, 9001, 'HD-TX-202608-12', '2026-08-17 19:05:00',  6900, '맘스터치 신논현점',   '2222223333', 'NORMAL', NULL, '2026-08-17 19:05:00', '2026-08-17 19:05:00'),
    (9113, 9001, 'HD-TX-202608-13', '2026-08-17 21:40:00',  8500, '배스킨라빈스 강남역점', '7733445566', 'NORMAL', NULL, '2026-08-17 21:40:00', '2026-08-17 21:40:00'),
    (9114, 9001, 'HD-TX-202608-14', '2026-08-18 08:05:00',  1450, '서울교통공사',        '1108600000', 'NORMAL', NULL, '2026-08-18 08:05:00', '2026-08-18 08:05:00'),
    (9115, 9001, 'HD-TX-202608-15', '2026-08-18 23:10:00',  9800, '카카오T',             '1199887766', 'NORMAL', NULL, '2026-08-18 23:10:00', '2026-08-18 23:10:00'),
    (9116, 9001, 'HD-TX-202608-16', '2026-08-19 09:30:00',  5400, 'CU 강남대로점',       '3311224455', 'NORMAL', NULL, '2026-08-19 09:30:00', '2026-08-19 09:30:00'),
    (9117, 9001, 'HD-TX-202608-17', '2026-08-19 22:00:00', 12300, 'GS25 역삼점',         '3322335566', 'NORMAL', NULL, '2026-08-19 22:00:00', '2026-08-19 22:00:00'),
    (9118, 9001, 'HD-TX-202608-18', '2026-08-21 15:00:00', 45900, '쿠팡',                '1055667788', 'NORMAL', NULL, '2026-08-21 15:00:00', '2026-08-21 15:00:00'),
    (9119, 9001, 'HD-TX-202608-19', '2026-08-22 19:20:00', 14000, 'CGV 강남',            '1122334455', 'NORMAL', NULL, '2026-08-22 19:20:00', '2026-08-22 19:20:00'),
    (9120, 9001, 'HD-TX-202608-20', '2026-08-23 17:40:00', 16800, '교보문고 강남점',      '1133445566', 'NORMAL', NULL, '2026-08-23 17:40:00', '2026-08-23 17:40:00'),
    (9121, 9001, 'HD-TX-202608-21', '2026-08-24 09:00:00', 14900, '유튜브 프리미엄',      NULL,         'NORMAL', NULL, '2026-08-24 09:00:00', '2026-08-24 09:00:00'),
    (9122, 9001, 'HD-TX-202608-22', '2026-08-27 14:30:00', 18000, '강남서울병원',        '4400556677', 'NORMAL', NULL, '2026-08-27 14:30:00', '2026-08-27 14:30:00'),
    (9123, 9001, 'HD-TX-202608-23', '2026-08-27 14:50:00', 12500, '온누리약국',          '4411667788', 'NORMAL', NULL, '2026-08-27 14:50:00', '2026-08-27 14:50:00'),
    (9124, 9001, 'HD-TX-202608-24', '2026-08-29 20:00:00', 89000, '해커스어학원',        '5500778899', 'NORMAL', NULL, '2026-08-29 20:00:00', '2026-08-29 20:00:00'),
    (9125, 9001, 'HD-TX-202608-25', '2026-08-30 10:00:00', 33000, '클래스101',           NULL,         'NORMAL', NULL, '2026-08-30 10:00:00', '2026-08-30 10:00:00'),
    (9126, 9001, 'HD-TX-202609-11', '2026-09-07 08:20:00',  1450, '서울교통공사',        '1108600000', 'NORMAL', NULL, '2026-09-07 08:20:00', '2026-09-07 08:20:00'),
    -- ── 신용카드(9002) 6개월 이용 내역 ────────────────────────────────────
    (9203, 9002, 'SH-TX-202604-03', '2026-04-19 15:40:00', 120000, '교보문고 강남점',     '1133445566', 'NORMAL', NULL, '2026-04-19 15:40:00', '2026-04-19 15:40:00'),
    (9205, 9002, 'SH-TX-202605-01', '2026-05-02 13:25:00', 150000, '무신사 스토어',       '1048100000', 'NORMAL', NULL, '2026-05-02 13:25:00', '2026-05-02 13:25:00'),
    (9207, 9002, 'SH-TX-202605-03', '2026-05-18 16:00:00', 180000, '나이키 강남',         '2299001122', 'NORMAL', NULL, '2026-05-18 16:00:00', '2026-05-18 16:00:00'),
    (9222, 9002, 'SH-TX-202609-02', '2026-09-03 14:10:00', 180000, '올리브영 강남',       '2208765432', 'NORMAL', NULL, '2026-09-03 14:10:00', '2026-09-03 14:10:00'),
    (9224, 9002, 'SH-TX-202609-04', '2026-09-07 13:20:00', 200000, '무신사 스토어',       '1048100000', 'NORMAL', NULL, '2026-09-07 13:20:00', '2026-09-07 13:20:00'),
    -- ── KB국민 탄탄대로(99003) 이용 내역 ───────────────────────────────────
    (999301, 99003, 'KB-TX-202604-01', '2026-04-08 12:30:00', 200000, '이마트 성수점',       '3302345678', 'NORMAL', NULL, '2026-04-08 12:30:00', '2026-04-08 12:30:00'),
    (999302, 99003, 'KB-TX-202604-02', '2026-04-22 19:10:00', 120000, '올리브영 강남',       '2208765432', 'NORMAL', NULL, '2026-04-22 19:10:00', '2026-04-22 19:10:00'),
    (999303, 99003, 'KB-TX-202605-01', '2026-05-07 13:45:00', 150000, '무신사 스토어',       '1048100000', 'NORMAL', NULL, '2026-05-07 13:45:00', '2026-05-07 13:45:00'),
    (999304, 99003, 'KB-TX-202605-02', '2026-05-21 20:25:00', 130000, '쿠팡',               '1208147521', 'NORMAL', NULL, '2026-05-21 20:25:00', '2026-05-21 20:25:00'),
    (999306, 99003, 'KB-TX-202606-02', '2026-06-24 18:50:00', 160000, '교보문고 강남점',     '1133445566', 'NORMAL', NULL, '2026-06-24 18:50:00', '2026-06-24 18:50:00'),
    (999308, 99003, 'KB-TX-202607-02', '2026-07-23 19:40:00', 150000, '올리브영 강남',       '2208765432', 'NORMAL', NULL, '2026-07-23 19:40:00', '2026-07-23 19:40:00'),
    (999310, 99003, 'KB-TX-202608-02', '2026-08-20 20:15:00', 200000, '무신사 스토어',       '1048100000', 'NORMAL', NULL, '2026-08-20 20:15:00', '2026-08-20 20:15:00'),
    (999312, 99003, 'KB-TX-202609-02', '2026-09-06 18:20:00', 180000, '쿠팡',               '1208147521', 'NORMAL', NULL, '2026-09-06 18:20:00', '2026-09-06 18:20:00'),
    -- ── 우리카드 카드의정석 이용 내역 ───────────────────────────────────────
    (999401, 99004, 'WR-TX-202604-01', '2026-04-14 11:50:00', 180000, '쿠팡',               '1208147521', 'NORMAL', NULL, '2026-04-14 11:50:00', '2026-04-14 11:50:00'),
    (999402, 99004, 'WR-TX-202604-02', '2026-04-28 17:20:00', 120000, '올리브영 강남',       '2208765432', 'NORMAL', NULL, '2026-04-28 17:20:00', '2026-04-28 17:20:00'),
    (999404, 99004, 'WR-TX-202605-02', '2026-05-25 19:55:00', 170000, '무신사 스토어',       '1048100000', 'NORMAL', NULL, '2026-05-25 19:55:00', '2026-05-25 19:55:00')
ON DUPLICATE KEY UPDATE
    transaction_at = VALUES(transaction_at),
    amount = VALUES(amount),
    merchant_name = VALUES(merchant_name),
    merchant_regno = VALUES(merchant_regno),
    transaction_status = VALUES(transaction_status),
    original_transaction_id = VALUES(original_transaction_id);

-- ── 소비 카테고리 (category) ──────────────────────────────────────────────
--
-- 회원과 무관한 마스터 데이터다. 비어 있으면 자동분류가 카테고리를 못 붙여서
-- 소비 내역이 전부 '자유 포켓'으로만 떨어지고 화면의 분류가 비어 보인다.
--
-- default_pocket_type: 그 카테고리 소비가 기본으로 들어갈 포켓.
--   ESSENTIAL(필수) 월세·공과금·통신비처럼 안 쓸 수 없는 것
--   FREE(자유)      식비·쇼핑처럼 조절 가능한 것
--   FUTURE_ASSET    저축·투자
--
-- id를 박고 uk_category_code로 멱등성을 잡는다. 계층은 parent_category_id 자기참조인데
-- 지금은 1단계만 쓴다 — 대분류를 만들면 화면이 두 번 접어야 해서 데모에 득이 없다.
INSERT INTO category (category_id, parent_category_id, category_code, category_name, default_pocket_type, is_active)
VALUES
    (1,  NULL, 'HOUSING',       '주거·관리비',  'ESSENTIAL',    1),
    (2,  NULL, 'UTILITY',       '공과금',       'ESSENTIAL',    1),
    (3,  NULL, 'TELECOM',       '통신비',       'ESSENTIAL',    1),
    (4,  NULL, 'INSURANCE',     '보험료',       'ESSENTIAL',    1),
    (5,  NULL, 'TRANSPORT',     '교통비',       'ESSENTIAL',    1),
    (6,  NULL, 'FOOD',          '식비',         'FREE',         1),
    (7,  NULL, 'CAFE',          '카페·간식',    'FREE',         1),
    (8,  NULL, 'CONVENIENCE',   '편의점·마트',  'FREE',         1),
    (9,  NULL, 'SHOPPING',      '쇼핑',         'FREE',         1),
    (10, NULL, 'BEAUTY',        '뷰티·미용',    'FREE',         1),
    (11, NULL, 'CULTURE',       '문화·여가',    'FREE',         1),
    (12, NULL, 'SUBSCRIPTION',  '구독료',       'FREE',         1),
    (13, NULL, 'MEDICAL',       '의료·건강',    'ESSENTIAL',    1),
    (14, NULL, 'EDUCATION',     '교육·자기계발','FREE',         1),
    (15, NULL, 'SAVINGS',       '저축·투자',    'FUTURE_ASSET', 1),
    (16, NULL, 'ETC',           '기타',         'FREE',         1)
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    default_pocket_type = VALUES(default_pocket_type),
    is_active = VALUES(is_active);

-- ── 가맹점 자동분류 규칙 (user_merchant_rule) ──────────────────────────────
--
-- MyData 원본에는 업종 카테고리가 없어 TransactionClassificationService는
-- 이 규칙이 없으면 전부 '자유 포켓 · 미분류'로만 떨어뜨린다 (§ 클래스 상단 주석 참고).
-- 데모 계정 소비내역을 카테고리별로 보여주려고 위 카드 거래내역의 가맹점마다
-- 규칙을 미리 심어둔다. merchant_key는 서비스 코드(merchantKey())와 같은 규칙으로
-- 계산한다 — 사업자번호가 있으면 REGNO:숫자만, 없으면 NAME:대문자·공백정리.
--
-- 실제 회원이 이 화면에서 직접 분류를 바꾸면 saveFutureRule()이 같은 테이블에
-- 규칙을 새로 쌓는데, 그때도 uk_user_merchant_key가 있어 멱등하게 덮어써진다.
INSERT INTO user_merchant_rule (user_id, merchant_key, merchant_regno, merchant_name, category_id, pocket_id, created_at, updated_at)
SELECT u.user_id, r.merchant_key, r.merchant_regno, r.merchant_name, c.category_id, p.pocket_id,
       '2026-09-01 00:00:00', '2026-09-01 00:00:00'
FROM users u
JOIN pocket p ON p.user_id = u.user_id
JOIN (
    -- 주거비
    SELECT 'REGNO:1298200036' AS merchant_key, '1298200036' AS merchant_regno, 'LH청년행복주택' AS merchant_name, 'HOUSING' AS category_code
    UNION ALL
    -- 식비
    SELECT 'REGNO:2201234567' AS merchant_key, '2201234567' AS merchant_regno, '김밥천국 강남점'     AS merchant_name, 'FOOD'         AS category_code
    UNION ALL SELECT 'REGNO:1208147521', '1208147521', '배달의민족',          'FOOD'
    UNION ALL SELECT 'REGNO:2211112222', '2211112222', '본죽앤비빔밥 강남점',  'FOOD'
    UNION ALL SELECT 'REGNO:2222223333', '2222223333', '맘스터치 신논현점',    'FOOD'
    -- 카페·간식
    UNION ALL SELECT 'REGNO:1208800000', '1208800000', '스타벅스 역삼점',      'CAFE'
    UNION ALL SELECT 'REGNO:1048164000', '1048164000', '투썸플레이스',        'CAFE'
    UNION ALL SELECT 'REGNO:7733445566', '7733445566', '배스킨라빈스 강남역점', 'CAFE'
    -- 편의점·마트
    UNION ALL SELECT 'REGNO:3302345678', '3302345678', '이마트24 논현점',      'CONVENIENCE'
    UNION ALL SELECT 'REGNO:3311224455', '3311224455', 'CU 강남대로점',        'CONVENIENCE'
    UNION ALL SELECT 'REGNO:3322335566', '3322335566', 'GS25 역삼점',          'CONVENIENCE'
    -- 교통비
    UNION ALL SELECT 'REGNO:1108600000', '1108600000', '서울교통공사',         'TRANSPORT'
    UNION ALL SELECT 'REGNO:1199887766', '1199887766', '카카오T',              'TRANSPORT'
    -- 쇼핑
    UNION ALL SELECT 'REGNO:1048100000', '1048100000', '무신사 스토어',        'SHOPPING'
    UNION ALL SELECT 'REGNO:1055667788', '1055667788', '쿠팡',                'SHOPPING'
    -- 뷰티·미용
    UNION ALL SELECT 'REGNO:2208765432', '2208765432', '올리브영 강남',        'BEAUTY'
    -- 문화·여가
    UNION ALL SELECT 'REGNO:1122334455', '1122334455', 'CGV 강남',            'CULTURE'
    UNION ALL SELECT 'REGNO:1133445566', '1133445566', '교보문고 강남점',      'CULTURE'
    -- 구독료 (사업자번호가 없어 이름으로 매칭)
    UNION ALL SELECT 'NAME:유튜브 프리미엄', NULL, '유튜브 프리미엄',          'SUBSCRIPTION'
    UNION ALL SELECT 'NAME:넷플릭스',        NULL, '넷플릭스',                'SUBSCRIPTION'
    -- 의료·건강
    UNION ALL SELECT 'REGNO:4400556677', '4400556677', '강남서울병원',        'MEDICAL'
    UNION ALL SELECT 'REGNO:4411667788', '4411667788', '온누리약국',          'MEDICAL'
    -- 교육·자기계발
    UNION ALL SELECT 'REGNO:5500778899', '5500778899', '해커스어학원',        'EDUCATION'
    UNION ALL SELECT 'NAME:클래스101',      NULL, '클래스101',               'EDUCATION'
) r ON 1 = 1
JOIN category c ON c.category_code = r.category_code
                AND p.pocket_type = c.default_pocket_type
WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    merchant_regno = VALUES(merchant_regno),
    merchant_name = VALUES(merchant_name),
    category_id = VALUES(category_id),
    pocket_id = VALUES(pocket_id);

-- ── 데모 계정 MyData 동기화 결과 (user_account / user_card / financial_transaction) ──
--
-- mock_mydata_*는 외부 제공기관 역할의 원본이고, 포켓 거래 화면은 이 테이블들을 직접
-- 조회하지 않는다. user1은 이미 온보딩·MyData 동의가 끝난 시연 계정이므로 애플리케이션을
-- 처음 띄운 직후에도 거래 화면을 볼 수 있도록 실제 동기화 결과까지 함께 심는다.
--
-- 고정 PK를 쓰지 않고 이메일과 외부 복합키로 연결한다. 모든 INSERT는 서비스의 upsert 키와
-- 같은 유니크 키를 사용하므로 POST /api/mydata/connect를 다시 호출해도 중복되지 않는다.
INSERT INTO user_account (
    user_id, org_code, account_num, account_name, product_name, account_type,
    balance, available_balance, interest_rate,
    is_active, is_selected, last_synced_at, created_at, updated_at
)
SELECT u.user_id, ma.org_code, ma.account_num, ma.account_name, ma.product_name, ma.account_type,
       ma.balance, ma.available_balance, ma.interest_rate,
       ma.is_active, 1, ma.updated_at, ma.created_at, ma.updated_at
FROM users u
JOIN mock_mydata_user mu ON mu.service_user_id = u.user_id
JOIN mock_mydata_account ma ON ma.mock_user_id = mu.mock_user_id
WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    account_name = VALUES(account_name),
    product_name = VALUES(product_name),
    account_type = VALUES(account_type),
    balance = VALUES(balance),
    available_balance = VALUES(available_balance),
    interest_rate = VALUES(interest_rate),
    is_active = VALUES(is_active),
    last_synced_at = VALUES(last_synced_at),
    updated_at = VALUES(updated_at);

INSERT INTO user_card (
    user_id, org_code, card_identifier, card_name, card_type, credit_limit,
    is_active, is_selected, last_synced_at, created_at, updated_at
)
SELECT u.user_id, mc.org_code, mc.external_card_id, mc.card_name, mc.card_type, mc.credit_limit,
       mc.is_active, 1, mc.updated_at, mc.created_at, mc.updated_at
FROM users u
JOIN mock_mydata_user mu ON mu.service_user_id = u.user_id
JOIN mock_mydata_card mc ON mc.mock_user_id = mu.mock_user_id
WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    card_name = VALUES(card_name),
    card_type = VALUES(card_type),
    credit_limit = VALUES(credit_limit),
    is_active = VALUES(is_active),
    last_synced_at = VALUES(last_synced_at),
    updated_at = VALUES(updated_at);

-- 계좌 거래. 정상 지출만 자동분류하고 수입·이체·취소는 서비스 동작과 같이 미분류로 둔다.
INSERT INTO financial_transaction (
    source_type, account_id, card_id, source_transaction_id,
    transaction_at, synced_at, transaction_type, transaction_status, amount,
    merchant_name, merchant_regno, trans_memo,
    category_id, pocket_id, classification_status, classification_source,
    counterparty_account_id, original_transaction_id, new_fund_checked,
    created_at, updated_at
)
SELECT
    'ACCOUNT', ua.account_id, NULL, mt.external_transaction_id,
    mt.transaction_at, mt.updated_at,
    CASE
        WHEN mt.transaction_type = 'TRANSFER' AND counterparty.account_id IS NOT NULL THEN 'SELF_TRANSFER'
        WHEN mt.transaction_type = 'TRANSFER' THEN 'OTHER'
        ELSE mt.transaction_type
    END,
    mt.transaction_status, mt.amount, mt.merchant_name, mt.merchant_regno, mt.trans_memo,
    CASE WHEN mt.transaction_type = 'EXPENSE' AND mt.transaction_status = 'NORMAL'
         THEN rule.category_id ELSE NULL END,
    CASE WHEN mt.transaction_type = 'EXPENSE' AND mt.transaction_status = 'NORMAL'
         THEN COALESCE(rule.pocket_id, free_pocket.pocket_id) ELSE NULL END,
    CASE WHEN mt.transaction_type = 'EXPENSE' AND mt.transaction_status = 'NORMAL'
         THEN 'AUTO_CLASSIFIED' ELSE 'UNCLASSIFIED' END,
    CASE WHEN mt.transaction_type = 'EXPENSE' AND mt.transaction_status = 'NORMAL'
         THEN CASE WHEN rule.merchant_rule_id IS NULL THEN 'DEFAULT_FREE' ELSE 'USER_RULE' END
         ELSE NULL END,
    counterparty.account_id, NULL, 0, mt.created_at, mt.updated_at
FROM users u
JOIN mock_mydata_user mu ON mu.service_user_id = u.user_id
JOIN mock_mydata_account ma ON ma.mock_user_id = mu.mock_user_id
JOIN mock_mydata_account_transaction mt ON mt.mock_account_id = ma.mock_account_id
JOIN user_account ua
  ON ua.user_id = u.user_id AND ua.org_code = ma.org_code AND ua.account_num = ma.account_num
JOIN pocket free_pocket ON free_pocket.user_id = u.user_id AND free_pocket.pocket_type = 'FREE'
LEFT JOIN user_account counterparty
  ON counterparty.user_id = u.user_id AND counterparty.account_num = mt.counterparty_account_num
LEFT JOIN user_merchant_rule rule
  ON rule.user_id = u.user_id
 AND rule.merchant_key = CASE
       WHEN mt.merchant_regno IS NOT NULL
         THEN CONCAT('REGNO:', REGEXP_REPLACE(mt.merchant_regno, '[^0-9]', ''))
       WHEN mt.merchant_name IS NOT NULL
         THEN CONCAT('NAME:', UPPER(TRIM(mt.merchant_name)))
       ELSE NULL
     END
WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    -- 포켓·카테고리 분류와 확인 상태는 보존하고 MyData 원본 표시값만 복구한다.
    transaction_at = VALUES(transaction_at),
    synced_at = VALUES(synced_at),
    transaction_type = VALUES(transaction_type),
    transaction_status = VALUES(transaction_status),
    amount = VALUES(amount),
    merchant_name = VALUES(merchant_name),
    merchant_regno = VALUES(merchant_regno),
    trans_memo = VALUES(trans_memo),
    counterparty_account_id = VALUES(counterparty_account_id),
    updated_at = VALUES(updated_at);

-- 카드 거래는 MyData 동기화 서비스가 모두 EXPENSE로 저장한다.
INSERT INTO financial_transaction (
    source_type, account_id, card_id, source_transaction_id,
    transaction_at, synced_at, transaction_type, transaction_status, amount,
    merchant_name, merchant_regno, trans_memo,
    category_id, pocket_id, classification_status, classification_source,
    counterparty_account_id, original_transaction_id, new_fund_checked,
    created_at, updated_at
)
SELECT
    'CARD', NULL, uc.card_id, mt.external_transaction_id,
    mt.transaction_at, mt.updated_at, 'EXPENSE', mt.transaction_status, mt.amount,
    mt.merchant_name, mt.merchant_regno, NULL,
    CASE WHEN mt.transaction_status = 'NORMAL' THEN rule.category_id ELSE NULL END,
    CASE WHEN mt.transaction_status = 'NORMAL'
         THEN COALESCE(rule.pocket_id, free_pocket.pocket_id) ELSE NULL END,
    CASE WHEN mt.transaction_status = 'NORMAL' THEN 'AUTO_CLASSIFIED' ELSE 'UNCLASSIFIED' END,
    CASE WHEN mt.transaction_status = 'NORMAL'
         THEN CASE WHEN rule.merchant_rule_id IS NULL THEN 'DEFAULT_FREE' ELSE 'USER_RULE' END
         ELSE NULL END,
    NULL, NULL, 0, mt.created_at, mt.updated_at
FROM users u
JOIN mock_mydata_user mu ON mu.service_user_id = u.user_id
JOIN mock_mydata_card mc ON mc.mock_user_id = mu.mock_user_id
JOIN mock_mydata_card_transaction mt ON mt.mock_card_id = mc.mock_card_id
JOIN user_card uc
  ON uc.user_id = u.user_id AND uc.org_code = mc.org_code
 AND uc.card_identifier = mc.external_card_id
JOIN pocket free_pocket ON free_pocket.user_id = u.user_id AND free_pocket.pocket_type = 'FREE'
LEFT JOIN user_merchant_rule rule
  ON rule.user_id = u.user_id
 AND rule.merchant_key = CASE
       WHEN mt.merchant_regno IS NOT NULL
         THEN CONCAT('REGNO:', REGEXP_REPLACE(mt.merchant_regno, '[^0-9]', ''))
       WHEN mt.merchant_name IS NOT NULL
         THEN CONCAT('NAME:', UPPER(TRIM(mt.merchant_name)))
       ELSE NULL
     END
WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    -- 수동 분류·새 자금 확인 상태는 보존하고 MyData 원본 표시값만 복구한다.
    transaction_at = VALUES(transaction_at),
    synced_at = VALUES(synced_at),
    transaction_status = VALUES(transaction_status),
    amount = VALUES(amount),
    merchant_name = VALUES(merchant_name),
    merchant_regno = VALUES(merchant_regno),
    updated_at = VALUES(updated_at);

-- 취소·환불 행의 원거래 FK는 양쪽 행이 모두 생긴 다음 외부 거래 ID로 연결한다.
UPDATE financial_transaction canceled
JOIN user_account ua ON ua.account_id = canceled.account_id
JOIN users u ON u.user_id = ua.user_id AND u.email = 'user1@test.com'
JOIN mock_mydata_user mu ON mu.service_user_id = u.user_id
JOIN mock_mydata_account ma
  ON ma.mock_user_id = mu.mock_user_id AND ma.account_num = ua.account_num
JOIN mock_mydata_account_transaction mt
  ON mt.mock_account_id = ma.mock_account_id
 AND mt.external_transaction_id = canceled.source_transaction_id
JOIN financial_transaction original
  ON original.account_id = canceled.account_id
 AND original.source_transaction_id = mt.original_transaction_id
SET canceled.original_transaction_id = original.financial_transaction_id
WHERE mt.original_transaction_id IS NOT NULL;

UPDATE financial_transaction canceled
JOIN user_card uc ON uc.card_id = canceled.card_id
JOIN users u ON u.user_id = uc.user_id AND u.email = 'user1@test.com'
JOIN mock_mydata_user mu ON mu.service_user_id = u.user_id
JOIN mock_mydata_card mc
  ON mc.mock_user_id = mu.mock_user_id AND mc.external_card_id = uc.card_identifier
JOIN mock_mydata_card_transaction mt
  ON mt.mock_card_id = mc.mock_card_id
 AND mt.external_transaction_id = canceled.source_transaction_id
JOIN financial_transaction original
  ON original.card_id = canceled.card_id
 AND original.source_transaction_id = mt.original_transaction_id
SET canceled.original_transaction_id = original.financial_transaction_id
WHERE mt.original_transaction_id IS NOT NULL;

-- ── 월 예산 (monthly_budget / monthly_pocket_budget / budget_draft_factor) ─
--
-- 데모 계정의 2026년 9월 예산이다. 온보딩 완료 시 예산 초안을 만드는 로직
-- (OnboardingService의 TODO)이 아직 없어서 시드로 채운다. 그 로직이 붙으면
-- 이 블록은 지운다 — 안 그러면 초안이 두 번 생긴다.
--
-- 숫자는 위 정기수입·주거비와 맞물린다. 한쪽만 고치면 화면 합계가 어긋난다.
--   수입   급여 1,200,000 + 자립수당 500,000            = 1,700,000
--   고정지출 월세 450,000 + 관리비 70,000 + 통신비 38,500 = 558,500
--   가용    1,700,000 - 558,500                          = 1,141,500
--
-- user_id / pocket_id 를 박지 않고 조회해서 넣는다. 시드가 만든 회원 id가 환경마다
-- 다를 수 있고(이메일이 이미 있으면 기존 id를 쓴다), 포켓 id는 auto-increment다.
INSERT INTO monthly_budget (
    monthly_budget_id, user_id, budget_month, total_budget_amount, budget_status,
    draft_summary, draft_created_at, confirmed_at, created_at, updated_at
)
SELECT 9001, u.user_id, '2026-09-01', 1141500, 'CONFIRMED',
       '수입 170만원에서 고정지출 55.85만원을 뺀 114.15만원을 이번 달 예산으로 잡았어요.',
       '2026-09-01 00:00:00', '2026-09-01 09:00:00',
       '2026-09-01 00:00:00', '2026-09-01 00:00:00'
FROM users u WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    total_budget_amount = VALUES(total_budget_amount),
    budget_status = VALUES(budget_status),
    draft_summary = VALUES(draft_summary),
    confirmed_at = VALUES(confirmed_at);

-- 포켓별 배분. 합계가 total_budget_amount(1,141,500)와 같아야 한다.
--   필수 558,500 + 자유 400,000 + 비상금 100,000 + 미래자산 83,000 = 1,141,500
-- 포켓은 이름이 아니라 pocket_type 으로 찾는다 - 이름은 사용자가 바꿀 수 있다.
INSERT INTO monthly_pocket_budget (
    monthly_budget_id, pocket_id, target_amount, allocation_method, created_at, updated_at
)
SELECT mb.monthly_budget_id, p.pocket_id, t.target_amount, t.allocation_method,
       '2026-09-01 00:00:00', '2026-09-01 00:00:00'
FROM monthly_budget mb
JOIN users u ON u.user_id = mb.user_id AND u.email = 'user1@test.com'
JOIN pocket p ON p.user_id = u.user_id
JOIN (
    SELECT 'ESSENTIAL'    AS pocket_type, 558500 AS target_amount, 'SYSTEM_DRAFT'   AS allocation_method
    UNION ALL SELECT 'FREE',         400000, 'USER_INPUT'
    UNION ALL SELECT 'EMERGENCY',    100000, 'USER_INPUT'
    UNION ALL SELECT 'FUTURE_ASSET',  83000, 'AUTO_REMAINDER'
) t ON t.pocket_type = p.pocket_type
WHERE mb.monthly_budget_id = 9001
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    allocation_method = VALUES(allocation_method);

-- 예산이 그 금액이 된 근거. "가용 114만원"만 던지면 사용자가 믿을 이유가 없어서
-- 더하고 뺀 항목을 그대로 보여준다. 금액은 전부 양수고 방향은 factor_type 이 안다.
INSERT INTO budget_draft_factor (
    budget_draft_factor_id, monthly_budget_id, factor_type, factor_name, amount,
    source_type, description, created_at
) VALUES
    (9001, 9001, 'SALARY',        '편의점 아르바이트', 1200000, 'USER_INPUT', '정기수입으로 등록한 금액', '2026-09-01 00:00:00'),
    (9002, 9001, 'ALLOWANCE',     '자립수당',           500000, 'POLICY',     '자립준비청년 자립수당',   '2026-09-01 00:00:00'),
    (9003, 9001, 'RENT',          '월세',               450000, 'USER_INPUT', '온보딩에서 입력한 주거비', '2026-09-01 00:00:00'),
    (9004, 9001, 'UTILITY',       '관리비',              70000, 'USER_INPUT', '온보딩에서 입력한 주거비', '2026-09-01 00:00:00'),
    (9005, 9001, 'TELECOM',       '통신비',              38500, 'MYDATA',     '최근 3개월 평균',          '2026-09-01 00:00:00')
ON DUPLICATE KEY UPDATE
    factor_type = VALUES(factor_type),
    factor_name = VALUES(factor_name),
    amount = VALUES(amount),
    source_type = VALUES(source_type),
    description = VALUES(description);

-- ── 신용점수 이력 (credit_score) ──────────────────────────────────────────
--
-- 신용관리 홈과 예상 금리 화면이 전부 이 표에서 나온다. 없으면 두 화면이 빈 채로 뜬다.
--
-- 여섯 달을 넣는 이유는 화면이 다섯 줄을 보여주기 때문이다. 가장 오래된 줄의 증감을
-- 계산하려면 목록 밖에 기록이 한 건 더 있어야 한다 (없으면 그 줄만 '-'로 뜬다).
--
-- ⚠️ 이 표에는 (user_id, updated_at) 유일 제약이 없다. 고정 id를 박지 않으면 앱을
-- 다시 띄울 때마다 같은 이력이 새로 쌓인다. id는 현재 AUTO_INCREMENT보다 큰 대역을 쓴다
-- (mock_mydata_card와 같은 이유 — 낮은 번호는 런타임에 생긴 행과 부딪힌다).
INSERT INTO credit_score (credit_score_id, user_id, agency, score, created_at, updated_at)
SELECT t.credit_score_id, u.user_id, 'KCB', t.score, t.at, t.at
FROM users u
JOIN (
    SELECT 99001 AS credit_score_id, 798 AS score, '2026-04-01 09:00:00' AS at
    UNION ALL SELECT 99002, 802, '2026-05-01 09:00:00'
    UNION ALL SELECT 99003, 809, '2026-06-01 09:00:00'
    UNION ALL SELECT 99004, 807, '2026-07-01 09:00:00'
    UNION ALL SELECT 99005, 810, '2026-08-01 09:00:00'
    UNION ALL SELECT 99006, 815, '2026-09-01 09:00:00'
) t
WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    agency = VALUES(agency),
    score = VALUES(score),
    updated_at = VALUES(updated_at);

-- ── 비금융 납부 이력 (nonfinancial_payment) ───────────────────────────────
--
-- 통신요금·건강보험료·국민연금을 제때 냈는지. 납부 기록 화면이 이 표에서 나온다.
--
-- 세 항목 모두 **익월 납부**다 (통신 25일, 건강보험·국민연금 10일). 그래서 아직 납부일이
-- 오지 않은 달은 넣지 않는다 — 미래에 낸 기록이 화면에 뜨면 그게 더 이상하다.
-- 2026-08분(기한 9/10·9/25)은 그래서 빠져 있다.
--
-- 통신요금 2026-03분만 연체다. 전부 정상이면 화면이 연체 상태를 한 번도 못 그린다.
-- 그 결과 통신요금은 "4개월 연속", 나머지 둘은 "6개월 연속"이 된다.
--
-- (user_id, payment_type, billing_month) 유일 제약이 있어 고정 id 없이도 멱등하다.
INSERT INTO nonfinancial_payment (
    user_id, payment_type, institution_name, billing_month, amount,
    due_date, paid_date, status, created_at, updated_at
)
SELECT u.user_id, t.payment_type, t.institution_name, t.billing_month, t.amount,
       t.due_date, t.paid_date, t.status, '2026-09-01 00:00:00', '2026-09-01 00:00:00'
FROM users u
JOIN (
    -- 통신요금 (SK텔레콤, 익월 25일)
    SELECT 'TELECOM' AS payment_type, 'SK텔레콤' AS institution_name,
           '2026-02-01' AS billing_month, 38500 AS amount,
           '2026-03-25' AS due_date, '2026-03-24' AS paid_date, 'PAID' AS status
    UNION ALL SELECT 'TELECOM', 'SK텔레콤', '2026-03-01', 38500, '2026-04-25', '2026-05-01', 'LATE'
    UNION ALL SELECT 'TELECOM', 'SK텔레콤', '2026-04-01', 38500, '2026-05-25', '2026-05-24', 'PAID'
    UNION ALL SELECT 'TELECOM', 'SK텔레콤', '2026-05-01', 38500, '2026-06-25', '2026-06-24', 'PAID'
    UNION ALL SELECT 'TELECOM', 'SK텔레콤', '2026-06-01', 38500, '2026-07-25', '2026-07-24', 'PAID'
    UNION ALL SELECT 'TELECOM', 'SK텔레콤', '2026-07-01', 38500, '2026-08-25', '2026-08-24', 'PAID'
    -- 건강보험료 (국민건강보험공단, 익월 10일)
    UNION ALL SELECT 'HEALTH_INSURANCE', '국민건강보험공단', '2026-02-01', 68200, '2026-03-10', '2026-03-09', 'PAID'
    UNION ALL SELECT 'HEALTH_INSURANCE', '국민건강보험공단', '2026-03-01', 68200, '2026-04-10', '2026-04-09', 'PAID'
    UNION ALL SELECT 'HEALTH_INSURANCE', '국민건강보험공단', '2026-04-01', 68200, '2026-05-10', '2026-05-09', 'PAID'
    UNION ALL SELECT 'HEALTH_INSURANCE', '국민건강보험공단', '2026-05-01', 68200, '2026-06-10', '2026-06-09', 'PAID'
    UNION ALL SELECT 'HEALTH_INSURANCE', '국민건강보험공단', '2026-06-01', 68200, '2026-07-10', '2026-07-09', 'PAID'
    UNION ALL SELECT 'HEALTH_INSURANCE', '국민건강보험공단', '2026-07-01', 68200, '2026-08-10', '2026-08-09', 'PAID'
    -- 국민연금 (국민연금공단, 익월 10일)
    UNION ALL SELECT 'NATIONAL_PENSION', '국민연금공단', '2026-02-01', 90000, '2026-03-10', '2026-03-09', 'PAID'
    UNION ALL SELECT 'NATIONAL_PENSION', '국민연금공단', '2026-03-01', 90000, '2026-04-10', '2026-04-09', 'PAID'
    UNION ALL SELECT 'NATIONAL_PENSION', '국민연금공단', '2026-04-01', 90000, '2026-05-10', '2026-05-09', 'PAID'
    UNION ALL SELECT 'NATIONAL_PENSION', '국민연금공단', '2026-05-01', 90000, '2026-06-10', '2026-06-09', 'PAID'
    UNION ALL SELECT 'NATIONAL_PENSION', '국민연금공단', '2026-06-01', 90000, '2026-07-10', '2026-07-09', 'PAID'
    UNION ALL SELECT 'NATIONAL_PENSION', '국민연금공단', '2026-07-01', 90000, '2026-08-10', '2026-08-09', 'PAID'
) t
WHERE u.email = 'user1@test.com'
ON DUPLICATE KEY UPDATE
    institution_name = VALUES(institution_name),
    amount = VALUES(amount),
    due_date = VALUES(due_date),
    paid_date = VALUES(paid_date),
    status = VALUES(status),
    updated_at = VALUES(updated_at);
