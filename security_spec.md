# HELLO STAR VIBE — Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Authentication Invariant**: Every read and write operation across all collections requires `request.auth != null`.
2. **User Isolation & PII Protection**: `/users/{userId}`, `/bank_accounts/{accountId}`, `/wallet_transactions/{txId}`, `/withdrawals/{withdrawalId}`, `/reports/{reportId}`, `/support_tickets/{ticketId}`, and `/notifications/{notificationId}` enforce strict owner-or-admin access (`request.auth.uid == userId || isAdmin()`).
3. **Role-Based Admin Verification**: Admin privileges (`isAdmin()`) require either a server-managed `/admins/{uid}` document or a verified token email matching `abdulbarik80000@gmail.com` with `request.auth.token.email_verified == true`. Clients cannot self-assign admin status.
4. **Financial & Creator Payout Integrity**:
   - Diamond purchases created by clients MUST start with `verificationStatus == 'pending_backend_verification'`. Clients can never mark their own payment as `'verified'`.
   - Withdrawal requests require `userVipLevelAtRequest >= 30`, `amountInrPaise >= 1000000` (₹10,000 minimum), `scheduledPayoutDay == 18`, and `status == 'pending_review'`. Clients can never mark a withdrawal as `'paid'`.
5. **Anti-Fraud View Deduplication**: `/posts/{postId}/views/{viewerId}` enforces `viewerId == request.auth.uid` and is append-only (`allow update, delete: if false`), preventing duplicate view inflation per user.
6. **Terminal State Locking**: Live streams with `status == 'ended'` and withdrawals in terminal states (`'paid'`, `'rejected'`) cannot be mutated by non-admin users.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Scrape**: Unauthenticated `.get()` or `.where()` on `/posts` or `/users` -> Rejected by `isSignedIn()`.
2. **Cross-User Profile / Bank Account Read**: Alice reads `/users/bob_456` or `/bank_accounts/bob_bank` -> Rejected by ownership check.
3. **Ghost Field Injection (Shadow Update)**: User writes `{"isAdmin": true}` or `{"verified": true}` to `/users/{uid}` -> Rejected by `.keys().hasOnly(...)` and `.affectedKeys().hasOnly(...)`.
4. **Admin Email Spoofing**: Unverified token (`email_verified: false`) with `email: "abdulbarik80000@gmail.com"` attempts admin moderation -> Rejected by `request.auth.token.email_verified == true`.
5. **Client-Side Payment Verification Bypass**: User creates `/wallet_transactions/{txId}` with `txType: "diamond_purchase"` and `verificationStatus: "verified"` -> Rejected by `incoming().verificationStatus == 'pending_backend_verification'`.
6. **Below-Threshold or Under-Level Withdrawal**: User with `userVipLevelAtRequest: 15` or `amountInrPaise: 50000` creates `/withdrawals/{id}` -> Rejected by `>= 30` and `>= 1000000` rules checks.
7. **Self-Paid Withdrawal Attack**: User updates `/withdrawals/{id}` to `status: "paid"` -> Rejected because only `isAdmin()` can update withdrawal status.
8. **Duplicate / Spoofed View Inflation**: Alice writes `/posts/{postId}/views/bob_456` or updates her existing view record -> Rejected by `viewerId == request.auth.uid` and `allow update: if false`.
9. **Ended Stream Resurrection**: Host updates `/live_streams/{id}` after `status == 'ended'` -> Rejected by Terminal State Lock (`existing().status != 'ended'`).
10. **Unfiltered Collection List (Query Enforcer)**: Client executes unfiltered `db.collection("posts").get()` without `where("visibility", "==", "public")` -> Rejected by `resource.data.visibility == 'public'`.
11. **Oversized String / Resource Exhaustion**: Client submits a 5,000-character bio or 200-item hashtag array -> Rejected by `.size()` bounds in `isValid*()` helpers.
12. **Immutable Timestamp Tampering**: Client modifies `createdAt` or `authorId` during an `update` -> Rejected by `incoming().createdAt == existing().createdAt` and `incoming().authorId == existing().authorId`.
