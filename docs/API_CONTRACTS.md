# API_CONTRACTS.md — authoritative v3 baseline
All routes `/api/v1`, JSON, authenticated unless indicated. IDs decimal strings, money decimal strings, dates YYYY-MM-DD, UTC timestamps. Strict server-side validation. `page=0&size=20` max 100 and allowlisted sort. Success uses direct resource objects; errors `{ "error": {"code":"VALIDATION_ERROR","message":"...","details":{}}, "requestId":"..." }`. 400/401/403/404/409/422/429/500 as appropriate. CSRF on every mutation.

## Authentication
`GET /auth/csrf` public, sets readable XSRF-TOKEN and returns `{csrfToken}`; `POST /auth/register` public with CSRF `{fullName,email,password}` -> 201 authenticated citizen; `POST /auth/login` public with CSRF `{email,password}` -> user + cookie; `POST /auth/logout` 204 clears cookie; `GET /auth/me` current user. Never return JWT or hash in JSON.

## Citizen
`GET /profile` -> `{profile:{dateOfBirth,stateCode,annualIncome,educationLevel,occupation},revision}`; `PUT /profile` -> full replacement plus `expectedRevision`, 409 conflict, increment once.
`GET /categories`; `GET /schemes?search=&stateCode=&categoryId=&page=0&size=20` published only; `GET /schemes/{id}` full detail/source/benefits/application guidance; `GET /schemes/{id}/questions` typed questions.
`PUT /schemes/{schemeId}/answers` `{expectedProfileRevision,answers:[{questionId,value}]}` scheme-scoped replacement: preserve omitted shared-question answers, submitted shared values update intentionally for all linked schemes; reject unrelated/duplicate/invalid answers; revision increments once.
`POST /eligibility/evaluate` `{schemeId,expectedProfileRevision}` -> `{evaluationId,schemeId,ruleVersionId,profileRevision,evaluationDate,verdict,stale,evaluatedAt,explanations:[{nodeId,fieldKey,operator,expectedValue,actualValue,result}]}`. 422 INPUT_REQUIRED with missing fields/questions and no history; 409 STALE_REVISION no history.
`GET /eligibility/history?page=0&size=20`; `GET /eligibility/history/{id}` owned immutable snapshots + stale reason.
`POST /suggestions` `{type:"SCHEME"|"CRITERION",schemeId?,schemeName?,description,proposedCriterion?,sourceUrl?}` -> 201 `{id,status:"PENDING",createdAt}`; `GET /suggestions/mine?page=0&size=20` own submissions. SCHEME requires schemeName; CRITERION requires schemeId.

## Admin
`POST /admin/schemes` and `PUT /admin/schemes/{id}` draft metadata, category, scope, description, benefits, guidance, verified official source; optimistic expectedRevision for update.
`POST /admin/questions`, `PUT /admin/questions/{id}` typed definitions; published references immutable in meaning.
`POST /admin/schemes/{id}/rules` create DRAFT JSON; `PUT /admin/schemes/{id}/rules/{versionId}` `{expectedDraftRevision,rule}` DRAFT only; `POST /admin/schemes/{id}/publish` `{draftVersionId,expectedDraftRevision}` -> atomic published pointer and audit, no unverified real scheme publication.
`GET /admin/audit-logs` paginated sanitized.
`GET /admin/suggestions`, `GET /admin/suggestions/{id}`, `POST /admin/suggestions/{id}/review` `{status:"UNDER_REVIEW"|"NEEDS_INFO"|"ACCEPTED"|"REJECTED",reviewNotes,expectedRevision}`. Acceptance never automatically publishes a scheme or rule.

## Rule JSON
Root `{schemaVersion:1,type:"GROUP",operator:"AND"|"OR",children:[...]}`. CONDITION `{type:"CONDITION",nodeId:"unique",field:"completedAgeYears"|"annualIncome"|"stateCode"|"educationLevel"|"occupation"|"question:<id>",operator:"EQ"|"NE"|"GT"|"GTE"|"LT"|"LTE"|"IN"|"NOT_IN"|"BETWEEN",value:<typed>}`. BETWEEN uses inclusive `[min,max]`; IN arrays homogeneous/nonempty. Max 3 GROUP levels. Reject empty groups, duplicate IDs, invalid field/operator/type, inactive or unlinked questions, MULTI_SELECT eligibility conditions. Return explanations for all leaves.

## Unspecified DTO details
Codex must document remaining exact fields and constraints consistently with this master, without introducing additional domain tables or dropping workflows. Do not treat illustrative examples as fully exhaustive OpenAPI.
