#!/usr/bin/env bash
set -euo pipefail

# ─── Rippl Backend E2E Test Script ───────────────────────────────────────────
#
# Usage:
#   ./e2e-test.sh                  # run all test suites
#   ./e2e-test.sh auth             # auth endpoints only
#   ./e2e-test.sh profile          # profile endpoints only
#   ./e2e-test.sh collectors       # collectors endpoints only
#   ./e2e-test.sh trends           # trends endpoints only
#   ./e2e-test.sh insights         # insights endpoints only
#   ./e2e-test.sh account          # account endpoints only
#   ./e2e-test.sh schema           # DB schema checks only
#   ./e2e-test.sh auth profile     # multiple suites
#
# Environment:
#   POSTGRES_CONTAINER    Docker container name (default: rippl-dashboard-postgres-1)
#
# Prerequisites:
#   - Docker running with postgres container (docker compose up -d)
#   - python3 available
#
# The script starts its own backend on port 8081 and stops it on exit.
#
# Idempotent: creates/cleans up its own test user. Safe to run repeatedly.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

PORT=8081
BASE_URL="http://localhost:$PORT"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-rippl-dashboard-postgres-1}"
BACKEND_LOG="/tmp/rippl-e2e-backend.log"
JWT_SECRET="dev-secret-key-for-testing-only-must-be-32-bytes-long"
BACKEND_PID=""
TEST_EMAIL="e2e-test-$(date +%s)@example.com"
OWNS_TEST_USER=false

PASS=0
FAIL=0
SKIP=0
SESSION=""
USER_ID=""

# ─── Helpers ─────────────────────────────────────────────────────────────────

red()    { printf '\033[0;31m%s\033[0m' "$*"; }
green()  { printf '\033[0;32m%s\033[0m' "$*"; }
yellow() { printf '\033[0;33m%s\033[0m' "$*"; }
bold()   { printf '\033[1m%s\033[0m' "$*"; }

assert_status() {
    local label="$1" expected="$2" actual="$3"
    if [[ "$actual" == "$expected" ]]; then
        echo "  $(green ✓) $label (HTTP $actual)"
        PASS=$((PASS + 1))
    else
        echo "  $(red ✗) $label — expected $expected, got $actual"
        FAIL=$((FAIL + 1))
    fi
}

assert_json_field() {
    local label="$1" json="$2" field="$3" expected="$4"
    local actual
    actual=$(echo "$json" | python3 -c "import sys,json; print(json.load(sys.stdin)$field)" 2>/dev/null || echo "__PARSE_ERROR__")
    if [[ "$actual" == "$expected" ]]; then
        echo "  $(green ✓) $label ($field = $actual)"
        PASS=$((PASS + 1))
    else
        echo "  $(red ✗) $label — $field expected '$expected', got '$actual'"
        FAIL=$((FAIL + 1))
    fi
}

assert_json_exists() {
    local label="$1" json="$2" field="$3"
    local actual
    actual=$(echo "$json" | python3 -c "import sys,json; v=json.load(sys.stdin)$field; print('exists')" 2>/dev/null || echo "__MISSING__")
    if [[ "$actual" == "exists" ]]; then
        echo "  $(green ✓) $label ($field present)"
        PASS=$((PASS + 1))
    else
        echo "  $(red ✗) $label — $field missing in: $(echo "$json" | head -c 200)"
        FAIL=$((FAIL + 1))
    fi
}

assert_json_length_gte() {
    local label="$1" json="$2" field="$3" min="$4"
    local actual
    actual=$(echo "$json" | python3 -c "import sys,json; print(len(json.load(sys.stdin)$field))" 2>/dev/null || echo "0")
    if (( actual >= min )); then
        echo "  $(green ✓) $label ($field has $actual items, need >= $min)"
        PASS=$((PASS + 1))
    else
        echo "  $(red ✗) $label — $field has $actual items, need >= $min"
        FAIL=$((FAIL + 1))
    fi
}

db_exec() {
    docker exec "$POSTGRES_CONTAINER" psql -U rippl -d rippl -t -c "$1" 2>/dev/null | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | grep -v '^$'
}

# ─── Preflight ───────────────────────────────────────────────────────────────

preflight() {
    echo ""
    bold "═══ Preflight Checks ═══"
    echo ""

    # python3
    if command -v python3 >/dev/null 2>&1; then
        echo "  $(green ✓) python3 available"
    else
        echo "  $(red ✗) python3 required"
        exit 1
    fi

    # Postgres
    if docker exec "$POSTGRES_CONTAINER" pg_isready -U rippl >/dev/null 2>&1; then
        echo "  $(green ✓) Postgres container running"
    else
        echo "  $(red ✗) Postgres container '$POSTGRES_CONTAINER' not reachable"
        echo "    Run: docker compose up -d"
        exit 1
    fi

    # Check port not already in use
    if lsof -ti:$PORT >/dev/null 2>&1; then
        echo "  $(red ✗) Port $PORT already in use"
        echo "    Kill existing process: kill \$(lsof -ti:$PORT)"
        exit 1
    fi

    echo ""
}

# ─── Backend Lifecycle ───────────────────────────────────────────────────────

start_backend() {
    bold "═══ Starting Backend ═══"
    echo ""
    echo "  Starting on port $PORT ..."

    > "$BACKEND_LOG"

    JWT_SECRET="$JWT_SECRET" \
        "$SCRIPT_DIR/gradlew" :backend:bootRun \
        --args="--server.port=$PORT" \
        > "$BACKEND_LOG" 2>&1 &
    BACKEND_PID=$!

    # Wait for backend to be ready (max 60s)
    local attempts=0
    while (( attempts < 60 )); do
        if curl -s -o /dev/null -w "%{http_code}" --max-time 2 "$BASE_URL/api/auth/me" 2>/dev/null | grep -q "401"; then
            echo "  $(green ✓) Backend ready on $BASE_URL (PID $BACKEND_PID)"
            echo ""
            return
        fi
        # Check if process died
        if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
            echo "  $(red ✗) Backend process died. Last 10 lines:"
            tail -10 "$BACKEND_LOG"
            exit 1
        fi
        sleep 1
        attempts=$((attempts + 1))
    done

    echo "  $(red ✗) Backend failed to start within 60s. Last 10 lines:"
    tail -10 "$BACKEND_LOG"
    kill "$BACKEND_PID" 2>/dev/null || true
    exit 1
}

stop_backend() {
    if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo "  Stopping backend (PID $BACKEND_PID) ..."
        kill "$BACKEND_PID" 2>/dev/null || true
        wait "$BACKEND_PID" 2>/dev/null || true
    fi
}

# ─── Auth: get session cookie ────────────────────────────────────────────────

authenticate() {
    OWNS_TEST_USER=true
    echo "  Authenticating as $TEST_EMAIL ..."

    # Request magic link (creates user in DB)
    local response
    response=$(curl -s -X POST "$BASE_URL/api/auth/magic-link" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$TEST_EMAIL\"}")

    local sent
    sent=$(echo "$response" | python3 -c "import sys,json; print(json.load(sys.stdin).get('sent',''))" 2>/dev/null || echo "")
    if [[ "$sent" != "True" ]]; then
        echo "  $(red ✗) Magic link request failed: $response"
        exit 1
    fi

    sleep 0.3

    # Get user ID from DB
    USER_ID=$(db_exec "SELECT id FROM users WHERE email = '$TEST_EMAIL'")
    if [[ -z "$USER_ID" ]]; then
        echo "  $(red ✗) User not created in DB"
        exit 1
    fi

    # Extract magic link token from backend log
    local token
    token=$(grep "DEV magic link" "$BACKEND_LOG" | tail -1 | grep -o 'token=[^ ]*' | cut -d= -f2 || true)

    if [[ -z "$token" ]]; then
        echo "  $(red ✗) No magic link token found in $BACKEND_LOG"
        echo "    Make sure backend uses RESEND_API_KEY=re_test (default) for DEV mode"
        exit 1
    fi

    # Verify token and capture session cookie
    SESSION=$(curl -s -D - "$BASE_URL/api/auth/verify?token=$token" 2>&1 \
        | grep -i "set-cookie" | head -1 | grep -o 'session=[^;]*' | cut -d= -f2 || true)

    if [[ -z "$SESSION" ]]; then
        echo "  $(red ✗) Token verification failed — no session cookie returned"
        exit 1
    fi

    echo "  $(green ✓) Authenticated — user $USER_ID"
    echo ""
}

# ─── Suites ──────────────────────────────────────────────────────────────────

suite_auth() {
    bold "═══ Auth Endpoints ═══"
    echo ""

    # Unauth → 401
    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/auth/me")
    assert_status "GET /api/auth/me (no cookie) → 401" "401" "$status"

    # Magic link — valid
    local body
    body=$(curl -s -X POST "$BASE_URL/api/auth/magic-link" \
        -H "Content-Type: application/json" \
        -d '{"email":"auth-suite@example.com"}')
    assert_json_field "POST /api/auth/magic-link → sent:true" "$body" "['sent']" "True"

    # Magic link — invalid email
    status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/magic-link" \
        -H "Content-Type: application/json" \
        -d '{"email":"not-an-email"}')
    assert_status "POST /api/auth/magic-link (bad email) → 400" "400" "$status"

    # Magic link — missing email
    status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/magic-link" \
        -H "Content-Type: application/json" \
        -d '{}')
    assert_status "POST /api/auth/magic-link (no email) → 400" "400" "$status"

    # Auth'd /me
    body=$(curl -s -b "session=$SESSION" "$BASE_URL/api/auth/me")
    assert_json_exists "GET /api/auth/me (auth'd) → has id" "$body" "['id']"
    assert_json_field "GET /api/auth/me → correct email" "$body" "['email']" "$TEST_EMAIL"

    # Logout clears cookie
    local headers
    headers=$(curl -s -D - -o /dev/null -X POST -b "session=$SESSION" "$BASE_URL/api/auth/logout" 2>&1)
    if echo "$headers" | grep -qi "max-age=0"; then
        echo "  $(green ✓) POST /api/auth/logout → clears cookie"
        PASS=$((PASS + 1))
    else
        echo "  $(red ✗) POST /api/auth/logout — cookie not cleared"
        FAIL=$((FAIL + 1))
    fi

    # Cleanup auth-suite user
    local auth_uid
    auth_uid=$(db_exec "SELECT id FROM users WHERE email = 'auth-suite@example.com'" || true)
    if [[ -n "$auth_uid" ]]; then
        db_exec "DELETE FROM auth_tokens WHERE user_id = '$auth_uid'" >/dev/null 2>&1 || true
        db_exec "DELETE FROM user_profiles WHERE user_id = '$auth_uid'" >/dev/null 2>&1 || true
        db_exec "DELETE FROM users WHERE id = '$auth_uid'" >/dev/null 2>&1 || true
    fi

    echo ""
}

suite_profile() {
    bold "═══ Profile Endpoints ═══"
    echo ""

    # Unauth
    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/profile")
    assert_status "GET /api/profile (no auth) → 401" "401" "$status"

    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/profile/templates")
    assert_status "GET /api/profile/templates (no auth) → 401" "401" "$status"

    # Ensure profile exists (auto-created on verify, or create manually)
    local profile_exists
    profile_exists=$(db_exec "SELECT COUNT(*) FROM user_profiles WHERE user_id = '$USER_ID'")
    if [[ "$profile_exists" == "0" ]]; then
        db_exec "INSERT INTO user_profiles (user_id, task_mix, personal_adjustment_factor) VALUES ('$USER_ID', '{\"writing\":0.15,\"coding\":0.15,\"research\":0.2,\"planning\":0.15,\"communication\":0.2,\"other\":0.15}', 1.0)" >/dev/null
    fi

    # GET profile
    local body
    body=$(curl -s -b "session=$SESSION" "$BASE_URL/api/profile")
    assert_json_exists "GET /api/profile → has task_mix" "$body" "['task_mix']"
    assert_json_exists "GET /api/profile → has personal_adjustment_factor" "$body" "['personal_adjustment_factor']"

    # GET templates
    body=$(curl -s -b "session=$SESSION" "$BASE_URL/api/profile/templates")
    assert_json_length_gte "GET /api/profile/templates → >= 3 templates" "$body" "" 3

    # PUT valid update — developer template
    body=$(curl -s -X PUT -b "session=$SESSION" \
        -H "Content-Type: application/json" \
        -d '{"task_mix":{"writing":0.0,"coding":0.7,"research":0.2,"planning":0.1,"communication":0.0,"other":0.0}}' \
        "$BASE_URL/api/profile")
    assert_json_field "PUT /api/profile (developer mix) → coding=0.7" "$body" "['task_mix']['coding']" "0.7"

    # Verify persistence
    body=$(curl -s -b "session=$SESSION" "$BASE_URL/api/profile")
    assert_json_field "GET /api/profile (after PUT) → coding persisted" "$body" "['task_mix']['coding']" "0.7"

    # PUT adjustment factor
    body=$(curl -s -X PUT -b "session=$SESSION" \
        -H "Content-Type: application/json" \
        -d '{"personal_adjustment_factor":1.5}' \
        "$BASE_URL/api/profile")
    assert_json_field "PUT /api/profile (adj factor 1.5)" "$body" "['personal_adjustment_factor']" "1.5"

    # Validation: weights sum > 1.0
    status=$(curl -s -o /dev/null -w "%{http_code}" -X PUT -b "session=$SESSION" \
        -H "Content-Type: application/json" \
        -d '{"task_mix":{"writing":0.5,"coding":0.5,"research":0.5,"planning":0.0,"communication":0.0,"other":0.0}}' \
        "$BASE_URL/api/profile")
    assert_status "PUT /api/profile (weights sum 1.5) → 400" "400" "$status"

    # Validation: adjustment factor out of range
    status=$(curl -s -o /dev/null -w "%{http_code}" -X PUT -b "session=$SESSION" \
        -H "Content-Type: application/json" \
        -d '{"personal_adjustment_factor":5.0}' \
        "$BASE_URL/api/profile")
    assert_status "PUT /api/profile (adj factor 5.0) → 400" "400" "$status"

    # Validation: negative adjustment factor
    status=$(curl -s -o /dev/null -w "%{http_code}" -X PUT -b "session=$SESSION" \
        -H "Content-Type: application/json" \
        -d '{"personal_adjustment_factor":-1.0}' \
        "$BASE_URL/api/profile")
    assert_status "PUT /api/profile (adj factor -1.0) → 400" "400" "$status"

    echo ""
}

suite_collectors() {
    bold "═══ Collectors Endpoints ═══"
    echo ""

    # Unauth
    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/collectors")
    assert_status "GET /api/collectors (no auth) → 401" "401" "$status"

    # List — valid JSON array
    local body
    body=$(curl -s -b "session=$SESSION" "$BASE_URL/api/collectors")
    local is_array
    is_array=$(echo "$body" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if isinstance(d, list) else 'no')" 2>/dev/null || echo "no")
    if [[ "$is_array" == "yes" ]]; then
        echo "  $(green ✓) GET /api/collectors → valid array"
        PASS=$((PASS + 1))
    else
        echo "  $(red ✗) GET /api/collectors → expected array, got: $(echo "$body" | head -c 100)"
        FAIL=$((FAIL + 1))
    fi

    # Create collector
    body=$(curl -s -X POST -b "session=$SESSION" \
        -H "Content-Type: application/json" \
        -d '{"type":"chrome_extension"}' \
        "$BASE_URL/api/collectors")
    assert_json_exists "POST /api/collectors → has id" "$body" "['id']"
    assert_json_exists "POST /api/collectors → has token" "$body" "['token']"
    assert_json_field "POST /api/collectors → type" "$body" "['type']" "chrome_extension"

    local collector_id
    collector_id=$(echo "$body" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo "")

    if [[ -n "$collector_id" ]]; then
        # Verify it shows in list
        body=$(curl -s -b "session=$SESSION" "$BASE_URL/api/collectors")
        local found
        found=$(echo "$body" | python3 -c "import sys,json; print('yes' if any(c['id']=='$collector_id' for c in json.load(sys.stdin)) else 'no')" 2>/dev/null || echo "no")
        if [[ "$found" == "yes" ]]; then
            echo "  $(green ✓) Collector appears in list"
            PASS=$((PASS + 1))
        else
            echo "  $(red ✗) Collector not in list after create"
            FAIL=$((FAIL + 1))
        fi

        # Delete
        status=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE -b "session=$SESSION" \
            "$BASE_URL/api/collectors/$collector_id")
        assert_status "DELETE /api/collectors/$collector_id → 200" "200" "$status"

        # Verify deleted
        body=$(curl -s -b "session=$SESSION" "$BASE_URL/api/collectors")
        found=$(echo "$body" | python3 -c "import sys,json; print('yes' if any(c['id']=='$collector_id' for c in json.load(sys.stdin)) else 'no')" 2>/dev/null || echo "yes")
        if [[ "$found" == "no" ]]; then
            echo "  $(green ✓) Collector gone after delete"
            PASS=$((PASS + 1))
        else
            echo "  $(red ✗) Collector still in list after delete"
            FAIL=$((FAIL + 1))
        fi
    fi

    # Delete non-existent → 404
    status=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE -b "session=$SESSION" \
        "$BASE_URL/api/collectors/00000000-0000-0000-0000-000000000000")
    assert_status "DELETE /api/collectors (non-existent) → 404" "404" "$status"

    echo ""
}

suite_trends() {
    bold "═══ Trends Endpoints ═══"
    echo ""

    local status

    # Unauth checks
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/trends/weekly")
    assert_status "GET /api/trends/weekly (no auth) → 401" "401" "$status"

    # Auth'd endpoints
    status=$(curl -s -o /dev/null -w "%{http_code}" -b "session=$SESSION" "$BASE_URL/api/trends/weekly")
    assert_status "GET /api/trends/weekly → 200" "200" "$status"

    status=$(curl -s -o /dev/null -w "%{http_code}" -b "session=$SESSION" "$BASE_URL/api/trends/monthly")
    assert_status "GET /api/trends/monthly → 200" "200" "$status"

    status=$(curl -s -o /dev/null -w "%{http_code}" -b "session=$SESSION" "$BASE_URL/api/trends/time-saved")
    assert_status "GET /api/trends/time-saved → 200" "200" "$status"

    status=$(curl -s -o /dev/null -w "%{http_code}" -b "session=$SESSION" "$BASE_URL/api/trends/activity-heatmap")
    assert_status "GET /api/trends/activity-heatmap → 200" "200" "$status"

    # With date range params
    status=$(curl -s -o /dev/null -w "%{http_code}" -b "session=$SESSION" \
        "$BASE_URL/api/trends/weekly?from=2025-01-01&to=2025-12-31")
    assert_status "GET /api/trends/weekly (date range) → 200" "200" "$status"

    status=$(curl -s -o /dev/null -w "%{http_code}" -b "session=$SESSION" \
        "$BASE_URL/api/trends/monthly?from=2025-01-01&to=2025-12-31")
    assert_status "GET /api/trends/monthly (date range) → 200" "200" "$status"

    echo ""
}

suite_insights() {
    bold "═══ Insights Endpoints ═══"
    echo ""

    local status

    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/insights/mirror")
    assert_status "GET /api/insights/mirror (no auth) → 401" "401" "$status"

    status=$(curl -s -o /dev/null -w "%{http_code}" -b "session=$SESSION" "$BASE_URL/api/insights/mirror")
    assert_status "GET /api/insights/mirror → 200" "200" "$status"

    echo ""
}

suite_account() {
    bold "═══ Account Endpoints ═══"
    echo ""

    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/account")
    assert_status "DELETE /api/account (no auth) → 401" "401" "$status"

    echo "  $(yellow ⊘) DELETE /api/account (auth'd) — skipped (destructive)"
    SKIP=$((SKIP + 1))

    echo ""
}

suite_schema() {
    bold "═══ DB Schema Checks ═══"
    echo ""

    local count

    # Table existence checks
    for table in users auth_tokens user_profiles scored_sessions collectors; do
        count=$(db_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '$table'")
        if [[ "$count" == "1" ]]; then
            echo "  $(green ✓) $table table exists"
            PASS=$((PASS + 1))
        else
            echo "  $(red ✗) $table table missing"
            FAIL=$((FAIL + 1))
        fi
    done

    # estimated_sessions should NOT exist (renamed to scored_sessions)
    count=$(db_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'estimated_sessions'")
    if [[ "$count" == "0" ]]; then
        echo "  $(green ✓) estimated_sessions removed (renamed to scored_sessions)"
        PASS=$((PASS + 1))
    else
        echo "  $(red ✗) estimated_sessions still exists"
        FAIL=$((FAIL + 1))
    fi

    # user_profiles columns
    for col in id user_id task_mix personal_adjustment_factor; do
        count=$(db_exec "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'user_profiles' AND column_name = '$col'")
        if [[ "$count" == "1" ]]; then
            echo "  $(green ✓) user_profiles.$col column exists"
            PASS=$((PASS + 1))
        else
            echo "  $(red ✗) user_profiles.$col column missing"
            FAIL=$((FAIL + 1))
        fi
    done

    echo ""
}

# ─── Cleanup ─────────────────────────────────────────────────────────────────

cleanup() {
    if [[ -n "${USER_ID:-}" ]] && $OWNS_TEST_USER; then
        echo "  Cleaning up test user $USER_ID ..."
        db_exec "DELETE FROM user_profiles WHERE user_id = '$USER_ID'" >/dev/null 2>&1 || true
        db_exec "DELETE FROM auth_tokens WHERE user_id = '$USER_ID'" >/dev/null 2>&1 || true
        db_exec "DELETE FROM extension_tokens WHERE user_id = '$USER_ID'" >/dev/null 2>&1 || true
        db_exec "DELETE FROM collectors WHERE user_id = '$USER_ID'" >/dev/null 2>&1 || true
        db_exec "DELETE FROM users WHERE id = '$USER_ID'" >/dev/null 2>&1 || true
    fi
    stop_backend
}

# ─── Main ────────────────────────────────────────────────────────────────────

SUITES=("${@:-all}")

echo ""
bold "╔══════════════════════════════════════╗"
bold "║   Rippl Backend E2E Test Runner      ║"
bold "╚══════════════════════════════════════╝"

preflight
trap cleanup EXIT
start_backend

NEEDS_AUTH=true
if [[ "${SUITES[0]}" == "schema" ]] && [[ ${#SUITES[@]} -eq 1 ]]; then
    NEEDS_AUTH=false
fi

if $NEEDS_AUTH; then
    authenticate
fi

for suite in "${SUITES[@]}"; do
    case "$suite" in
        all)
            suite_auth
            suite_profile
            suite_collectors
            suite_trends
            suite_insights
            suite_account
            suite_schema
            ;;
        auth)       suite_auth ;;
        profile)    suite_profile ;;
        collectors) suite_collectors ;;
        trends)     suite_trends ;;
        insights)   suite_insights ;;
        account)    suite_account ;;
        schema)     suite_schema ;;
        *)
            echo "  $(red ✗) Unknown suite: $suite"
            echo "  Available: auth profile collectors trends insights account schema"
            exit 1
            ;;
    esac
done

# ─── Summary ─────────────────────────────────────────────────────────────────

echo ""
bold "═══ Summary ═══"
echo ""
echo "  $(green "✓ $PASS passed")  $(red "✗ $FAIL failed")  $(yellow "⊘ $SKIP skipped")"
echo ""

if (( FAIL > 0 )); then
    exit 1
fi
