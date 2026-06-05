# Postman guide — Targeted campaigns + `campaignUid` scoped events

For **tenant platform developers** verifying LoyaltyOS before integration.

**Base URL:** `http://localhost:8081` (adjust if different)

**Import collections:**

- JWT setup: `docs/postman/LoyaltyOS-Campaign-Scoped-Events.postman_collection.json` + `LoyaltyOS-Campaign-Events-Environment.json`
- Integration (tenant app): `docs/postman/LoyaltyOS-Integration-Collection.json` → folder **Targeted Campaigns**
- Quick guide: `docs/postman/TARGETED_CAMPAIGNS_TESTING.md`

---

## 1. Postman environment

Create environment **LoyaltyOS Dev** with:

| Variable | Initial value | Notes |
|----------|---------------|--------|
| `baseUrl` | `http://localhost:8081` | |
| `accessToken` | *(empty)* | Set by Login test script |
| `programmeUid` | *(empty)* | Set by List Programmes |
| `campaignAllUid` | *(empty)* | ALL audience campaign |
| `campaignTargetedUid` | *(empty)* | TARGETED campaign |

**Collection auth:** Bearer Token → `{{accessToken}}`  
(Login request uses **No Auth**.)

---

## 2. Prerequisites

- Backend running (`gradlew bootRun` on port 8081)
- Tenant admin email + password
- CSV file for upload (see step 6)

---

## 3. Login

**POST** `{{baseUrl}}/api/v1/auth/login`  
**Authorization:** No Auth  
**Headers:** `Content-Type: application/json`  
**Body:**

```json
{
  "email": "your-admin@tenant.com",
  "password": "your-password"
}
```

**Tests tab:**

```javascript
const j = pm.response.json();
pm.environment.set("accessToken", j.accessToken);
pm.test("Login 200", () => pm.response.to.have.status(200));
```

**Expected:** `200`, `accessToken` in body.

---

## 4. Get programme UID (do not guess `"default"`)

**GET** `{{baseUrl}}/api/v2/programmes`  
**Authorization:** Bearer `{{accessToken}}`

**Tests tab:**

```javascript
const list = pm.response.json();
pm.environment.set("programmeUid", list[0].programmeUid);
console.log("programmeUid", list[0].programmeUid);
```

**Expected example:**

```json
[
  {
    "programmeUid": "aa2fd7aa-376d-4c06-bc29-9d73cddc3371",
    "name": "default"
  }
]
```

Use **`programmeUid` from this response** in every following request.

---

## 5. Create ALL-audience campaign

**POST** `{{baseUrl}}/api/v1/campaigns/admin/campaigns`  
**Body:**

```json
{
  "programmeUid": "{{programmeUid}}",
  "name": "Dev Test ALL Customers",
  "description": "Postman - all customers",
  "campaignType": "STANDARD",
  "customerScope": "ALL",
  "triggerEventType": "PURCHASE",
  "offerConfig": {
    "awardType": "POINTS_BONUS",
    "bonusPoints": 30,
    "stackableWithRules": true
  },
  "budgetTotal": 100000,
  "alertThresholdPct": 80,
  "priority": 10,
  "validFrom": "2026-01-01T00:00:00Z",
  "validUntil": "2027-12-31T23:59:59Z"
}
```

**Tests tab:**

```javascript
const j = pm.response.json();
pm.environment.set("campaignAllUid", j.campaignUid);
pm.test("ALL scope", () => pm.expect(j.customerScope).to.eql("ALL"));
pm.test("DRAFT", () => pm.expect(j.status).to.eql("DRAFT"));
```

**Expected:** `201`, `customerScope`: `"ALL"`, `status`: `"DRAFT"`.

---

## 6. Create TARGETED campaign

**POST** `{{baseUrl}}/api/v1/campaigns/admin/campaigns`  
**Body:**

```json
{
  "programmeUid": "{{programmeUid}}",
  "name": "Dev Test TARGETED VIP",
  "description": "Postman - specific customers",
  "campaignType": "STANDARD",
  "customerScope": "TARGETED",
  "triggerEventType": "PURCHASE",
  "offerConfig": {
    "awardType": "POINTS_BONUS",
    "bonusPoints": 60,
    "stackableWithRules": true
  },
  "budgetTotal": 100000,
  "alertThresholdPct": 80,
  "priority": 5,
  "validFrom": "2026-01-01T00:00:00Z",
  "validUntil": "2027-12-31T23:59:59Z"
}
```

**Tests tab:**

```javascript
const j = pm.response.json();
pm.environment.set("campaignTargetedUid", j.campaignUid);
pm.test("TARGETED scope", () => pm.expect(j.customerScope).to.eql("TARGETED"));
pm.test("customerCount 0", () => pm.expect(j.customerCount).to.eql(0));
```

---

## 7. Upload customer CSV

Create file `target-customers-dev.csv`:

```csv
customer_id
dev_target_002
```

**POST** `{{baseUrl}}/api/v1/campaigns/admin/campaigns/{{campaignTargetedUid}}/target-customers/upload`

- **Authorization:** Bearer `{{accessToken}}` *(required — 401 if missing)*
- **Body:** form-data
  - Key: `file` | Type: **File** | Value: select CSV

**Expected:**

```json
{
  "status": "COMPLETED",
  "importedCount": 1,
  "duplicateCount": 0
}
```

---

## 8. Verify target list

**GET** `{{baseUrl}}/api/v1/campaigns/admin/campaigns/{{campaignTargetedUid}}/target-customers?page=0&size=20&search=dev_target`

**Expected:** `dev_target_002` in content.

**GET** `{{baseUrl}}/api/v1/campaigns/admin/campaigns/{{campaignTargetedUid}}`

**Expected:** `customerCount`: `1`, `customerScope`: `"TARGETED"`.

---

## 9. Activate both campaigns

**POST** `{{baseUrl}}/api/v1/campaigns/admin/campaigns/{{campaignAllUid}}/activate`  
**POST** `{{baseUrl}}/api/v1/campaigns/admin/campaigns/{{campaignTargetedUid}}/activate`

**Expected each:** `status`: `"ACTIVE"`.

---

## 10. Test scenarios (event process)

**POST** `{{baseUrl}}/api/v1/loyalty/events/process`  
**Authorization:** Bearer `{{accessToken}}`  
**Headers:** `Content-Type: application/json`

Use a **new** `transactionId` for every send.

---

### Scenario A — Normal customer (no `campaignUid`)

**Purpose:** Normal customer must **not** earn on TARGETED campaign.

**Body:**

```json
{
  "programmeUid": "{{programmeUid}}",
  "evaluationScope": "CAMPAIGN",
  "customerId": "dev_normal_002",
  "eventType": "PURCHASE",
  "transactionId": "txn-postman-normal-001",
  "amount": 500,
  "metadata": {
    "timestamp": "2026-06-03T12:00:00Z",
    "channel": "WEB"
  }
}
```

**Pass if:**

- `success`: `true`
- `campaignsApplied` does **not** include `{{campaignTargetedUid}}`
- `campaignsDropped` contains TARGETED campaign with `dropReason`: `CUSTOMER_NOT_IN_TARGET_LIST`
- May include ALL campaign(s) in `campaignsApplied`

**Example snippet:**

```json
"campaignsApplied": [
  { "campaignName": "Dev Test ALL Customers", "pointsAwarded": 30 }
],
"campaignsDropped": [
  {
    "campaignUid": "<campaignTargetedUid>",
    "dropReason": "CUSTOMER_NOT_IN_TARGET_LIST"
  }
]
```

---

### Scenario B — Targeted customer + ALL campaign (`campaignUid` = ALL)

**Purpose:** VIP customer chose **general** promo → points **only** on ALL campaign.

**Body:**

```json
{
  "programmeUid": "{{programmeUid}}",
  "evaluationScope": "CAMPAIGN",
  "campaignUid": "{{campaignAllUid}}",
  "customerId": "dev_target_002",
  "eventType": "PURCHASE",
  "transactionId": "txn-postman-target-all-001",
  "amount": 500,
  "metadata": {
    "timestamp": "2026-06-03T12:00:00Z",
    "channel": "WEB"
  }
}
```

**Pass if:**

- `campaignsApplied.length` === **1**
- `campaignsApplied[0].campaignUid` === `{{campaignAllUid}}`
- `campaignPointsAwarded` === **30**
- TARGETED campaign **not** in `campaignsApplied`

**Tests tab:**

```javascript
const b = pm.response.json();
const allUid = pm.environment.get("campaignAllUid");
const targetedUid = pm.environment.get("campaignTargetedUid");
pm.test("One campaign applied", () => pm.expect(b.campaignsApplied.length).to.eql(1));
pm.test("ALL campaign only", () => pm.expect(b.campaignsApplied[0].campaignUid).to.eql(allUid));
pm.test("30 points", () => pm.expect(Number(b.campaignPointsAwarded)).to.eql(30));
const appliedUids = b.campaignsApplied.map((x) => x.campaignUid);
pm.test("TARGETED not applied", () => pm.expect(appliedUids).to.not.include(targetedUid));
```

---

### Scenario C — Targeted customer + TARGETED campaign (`campaignUid` = TARGETED)

**Purpose:** VIP customer chose **VIP** promo → points **only** on TARGETED campaign.

**Body:**

```json
{
  "programmeUid": "{{programmeUid}}",
  "evaluationScope": "CAMPAIGN",
  "campaignUid": "{{campaignTargetedUid}}",
  "customerId": "dev_target_002",
  "eventType": "PURCHASE",
  "transactionId": "txn-postman-target-vip-001",
  "amount": 500,
  "metadata": {
    "timestamp": "2026-06-03T12:00:00Z",
    "channel": "WEB"
  }
}
```

**Pass if:**

- `campaignsApplied.length` === **1**
- `campaignsApplied[0].campaignUid` === `{{campaignTargetedUid}}`
- `campaignPointsAwarded` === **60**
- ALL campaign **not** in `campaignsApplied`

**Tests tab:**

```javascript
const b = pm.response.json();
const allUid = pm.environment.get("campaignAllUid");
const targetedUid = pm.environment.get("campaignTargetedUid");
pm.test("One campaign applied", () => pm.expect(b.campaignsApplied.length).to.eql(1));
pm.test("TARGETED only", () => pm.expect(b.campaignsApplied[0].campaignUid).to.eql(targetedUid));
pm.test("60 points", () => pm.expect(Number(b.campaignPointsAwarded)).to.eql(60));
const appliedUids = b.campaignsApplied.map((x) => x.campaignUid);
pm.test("ALL not applied", () => pm.expect(appliedUids).to.not.include(allUid));
```

---

### Scenario D — Targeted customer without `campaignUid` (legacy / multi-campaign)

**Purpose:** Shows why integrators should send `campaignUid` when the action is campaign-specific.

**Body:** Same as Scenario C but **remove** `campaignUid`.

**Observe:** Multiple campaigns may appear in `campaignsApplied` (ALL + TARGETED + any other active PURCHASE campaigns on the programme).

**Production recommendation:** Always send `campaignUid` when the tenant UI knows which promo the customer used.

---

### Scenario E — Wrong `campaignUid` for customer

**Body:** Scenario C with `customerId`: `dev_normal_002` (not on CSV).

**Pass if:**

- `campaignsApplied` empty or no points
- `campaignsDropped` with `CUSTOMER_NOT_IN_TARGET_LIST` for TARGETED campaign

---

## 11. Error cases

| Test | Body change | Expected HTTP |
|------|-------------|---------------|
| Bad campaign UID | `campaignUid`: `00000000-0000-0000-0000-000000000000` | 404 |
| Wrong programme | `programmeUid` ≠ campaign’s programme | 400 |
| Inactive campaign | Activate only ALL, call event with TARGETED uid | 400 |
| Reused transaction | Same `transactionId` twice | Second may replay prior result |

---

## 12. Final checklist

| # | Check |
|---|--------|
| 1 | Login → token saved |
| 2 | `programmeUid` from GET programmes (exact UUID) |
| 3 | ALL + TARGETED campaigns ACTIVE |
| 4 | CSV uploaded, `dev_target_002` on list |
| 5 | Normal customer: no TARGETED in `campaignsApplied` |
| 6 | Targeted + `campaignAllUid`: only 30 pts, one campaign |
| 7 | Targeted + `campaignTargetedUid`: only 60 pts, one campaign |

---

## 13. Integration API (tenant applications)

Auth: **API key + HMAC** (same as vouchers/referrals). See `docs/INTEGRATION_API_GUIDE.md`.

### 13a. Upload target customers (public)

**GET** `{{baseUrl}}/api/v1/integration/{{tenantId}}/campaigns/target-customers/upload-spec`  
HMAC: sign **empty** body.

**POST** `{{baseUrl}}/api/v1/integration/{{tenantId}}/campaigns/{{campaignTargetedUid}}/target-customers/bulk`  
**Body:**

```json
{
  "customerIds": ["dev_target_002"]
}
```

**Or CSV upload:**  
**POST** `.../campaigns/{{campaignTargetedUid}}/target-customers/upload`  
form-data: `file` = your CSV (sign the **raw multipart** body for HMAC).

**GET** `.../campaigns/{{campaignTargetedUid}}/target-customers?page=0&size=20`  
**GET** `.../campaigns/{{campaignTargetedUid}}/audience` — `customerCount`, `customerScope`, `status`

### 13b. Process events (public)

**POST** `{{baseUrl}}/api/v1/integration/{{tenantId}}/events/process`

Include `campaignUid` when the checkout knows which promo applied. Example:

```json
{
  "programmeUid": "{{programmeUid}}",
  "evaluationScope": "CAMPAIGN",
  "campaignUid": "{{campaignTargetedUid}}",
  "customerId": "dev_target_002",
  "eventType": "PURCHASE",
  "eventId": "evt-int-target-001",
  "amount": 500,
  "channel": "WEB"
}
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| 401 on upload/event | Bearer token on request |
| `programmeUid` in response is `"default"` but you sent UUID | Confirm campaign rows use same programme UUID as GET programmes |
| Triple points without `campaignUid` | Send `campaignUid`; or set same `mutualExclGroup` + `BEST_OFFER` on campaigns |
| TARGETED not in dropped for normal user | TARGETED not ACTIVE or wrong programme |
| `CUSTOMER_NOT_IN_TARGET_LIST` for `dev_target_002` | CSV id must match `customerId` exactly |
