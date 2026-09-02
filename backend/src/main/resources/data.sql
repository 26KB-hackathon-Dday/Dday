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

-- ── 포켓 ──────────────────────────────────────────────────────────────────
-- 기획서의 네 포켓. 금액은 시연용 예시값이다.
-- id를 고정하고 ON DUPLICATE KEY UPDATE를 써서, 몇 번을 재기동해도 결과가 같다.
-- 여기 값을 고쳐서 다시 띄우면 그 값으로 갱신된다.
INSERT INTO pocket (id, type, monthly_budget, spent_this_month) VALUES
    (1, 'HOUSING',   650000.00, 650000.00),
    (2, 'LIVING',    600000.00, 412000.00),
    (3, 'EMERGENCY', 200000.00,  35000.00),
    (4, 'ASSET',     100000.00,      0.00)
ON DUPLICATE KEY UPDATE
    type             = VALUES(type),
    monthly_budget   = VALUES(monthly_budget),
    spent_this_month = VALUES(spent_this_month);
