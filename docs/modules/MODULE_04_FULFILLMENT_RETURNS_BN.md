# Module 4: Shipping + Order Tracking + Return

## Order State Machine: পুরো প্রজেক্টের মেরুদণ্ড
আগে status-গুলো বিভিন্ন জায়গায় `if` দিয়ে যাচাই হতো। এখন **একটা টেবিল** (`OrderStatus.ALLOWED`) ঠিক করে কোন অবস্থা থেকে কোন অবস্থায় যাওয়া যাবে। Status বদলানোর **একমাত্র পথ** হলো `CustomerOrder.transitionTo()`। এটা টেবিল যাচাই করে (না মিললে **409**), আর প্রতিটা পরিবর্তন timeline-এ লিখে রাখে।

| থেকে \ যেতে পারে | PAID | SHIPPED | DELIVERED | RETURN_REQUESTED | RETURNED | CANCELLED |
|---|---|---|---|---|---|---|
| **PLACED** | ✅ pay | | | | | ✅ cancel |
| **PAID** | | ✅ ship | | | | ✅ cancel + refund |
| **SHIPPED** | | | ✅ deliver | | | ❌ |
| **DELIVERED** | | | | ✅ return চাওয়া | | ❌ |
| **RETURN_REQUESTED** | | | ✅ reject | | ✅ approve + refund | |
| **RETURNED / CANCELLED** | শেষ অবস্থা, আর কোথাও যাওয়া যায় না | | | | | |

ডাটাবেসেও `CHECK (status IN (...))` দেওয়া আছে, তাই ভুল status কোনোভাবেই সংরক্ষণ হবে না।

**State transition testing:** `OrderStateMachineTest` ৭টা status × ৭টা status = **৪৯টা জোড়ার প্রতিটা** পরীক্ষা করে। অনুমোদিত হলে সফল হতে হবে আর একটা নতুন event যোগ হতে হবে। অননুমোদিত হলে 409 আসতে হবে, আর কিছুই বদলানো যাবে না। এটা ISTQB-এর state transition টেকনিকের একদম পাঠ্যবইয়ের উদাহরণ।

## Shipping (শুধু ADMIN, অর্থাৎ warehouse)
- `POST /api/admin/orders/{id}/ship {"carrier":"UPS|FEDEX|USPS"}` → SHIPPED, tracking নম্বর যেমন `UPS-482910384756`
  - শুধু **PAID** অর্ডার ship হয়। unpaid বা cancelled অর্ডার কখনো warehouse ছাড়বে না।
- `POST /api/admin/orders/{id}/deliver` → DELIVERED (শুধু SHIPPED অর্ডার)
- Ship হওয়ার পর গ্রাহক আর cancel করতে পারে না (409)।

## Tracking
- `GET /api/orders/{id}/tracking`: গ্রাহক নিজের অর্ডারের পুরো timeline দেখে।
- `GET /api/tracking/{trackingNumber}`: **লগইন ছাড়াই** দেখা যায়, carrier-এর ওয়েবসাইটের মতো।
  - **নিরাপত্তা:** এই উত্তরে কোনো email, customer id, দাম বা item থাকে না। শুধু status আর timeline। টেস্টে এটা যাচাই করা আছে।
- নতুন `order_events` টেবিল। আপনার পুরনো অর্ডারগুলোর জন্য V6 migration নিজে একটা "Backfilled" event বসিয়ে দেয়।

## Return
**নিয়ম:**
1. শুধু **DELIVERED** অর্ডার return করা যায় (নাহলে 409)।
2. Delivery-র **৩০ দিনের মধ্যে** (`app.returns.window-days`)। ঠিক ৩০ দিন পর্যন্ত চলবে, ১ সেকেন্ড পরে হলে 400।
3. প্রতি অর্ডারে **একবারই** return চাওয়া যায় (ডাটাবেসে `UNIQUE(order_id)`)।
4. কারণ ৪টার একটা হতে হবে। নাহলে 400।

**Decision table (refund + restock):**

| কারণ | Refund | Stock-এ ফেরত |
|---|---|---|
| DAMAGED | পুরো total | **না**, নষ্ট মাল write-off |
| WRONG_ITEM | পুরো total | হ্যাঁ |
| NOT_AS_DESCRIBED | পুরো total | হ্যাঁ |
| NO_LONGER_NEEDED | total − shipping fee | হ্যাঁ |

- Approve হলে refund লেখা হয় (`payment_transactions` REFUND), দরকার হলে restock হয় (`stock_movements` RETURN_RESTOCK), আর order হয় RETURNED।
- Reject হলে order আবার DELIVERED-এ ফেরে, refund হয় না।
- একবার resolve হওয়া return আবার approve বা reject করা যায় না (409)।

**সময়ের উপর নির্ভরশীল টেস্ট কীভাবে করা হয়েছে:** "৩০ দিন পরে" টেস্ট করতে ৩০ দিন অপেক্ষা করা সম্ভব না। তাই কোডে `Clock` inject করা হয়েছে। Unit টেস্টে একটা নির্দিষ্ট সময় (fixed clock) দিয়ে ঠিক boundary-তে (৩০ দিন আর ৩০ দিন + ১ সেকেন্ড) যাচাই করা হয়। ইন্টারভিউতে "time-dependent logic কীভাবে টেস্ট করেন?" প্রশ্নের এটাই উত্তর।

## API সারসংক্ষেপ
| Endpoint | কে | সফল | ভুল |
|---|---|---|---|
| POST /api/admin/orders/{id}/ship | ADMIN | 200 | 400, 401, 403, 404, 409 |
| POST /api/admin/orders/{id}/deliver | ADMIN | 200 | 401, 403, 404, 409 |
| GET /api/orders/{id}/tracking | মালিক | 200 | 401, 404 |
| GET /api/tracking/{trackingNumber} | সবাই | 200 | 404 |
| POST /api/orders/{id}/return `{reason, comment?}` | মালিক | 201 | 400, 401, 404, 409 |
| GET /api/orders/{id}/return | মালিক | 200 | 401, 404 |
| GET /api/admin/returns?status= | ADMIN | 200 | 401, 403 |
| POST /api/admin/returns/{id}/approve | ADMIN | 200 | 401, 403, 404, 409 |
| POST /api/admin/returns/{id}/reject `{note?}` | ADMIN | 200 | 401, 403, 404, 409 |

## টেস্ট কভারেজ
| স্তর | ফাইল | সংখ্যা |
|---|---|---|
| Unit | `OrderStateMachineTest` (৪৯ জোড়া + ৪), `ReturnServiceTest` (১০), `FulfillmentServiceTest` (৯) | ৭২ নতুন |
| Integration (H2) | `FulfillmentReturnIntegrationTest` | ৭ |
| API | `FulfillmentApiTest` (৭), `ReturnApiTest` (৯) | ১৬ |
| DB | `LifecycleDatabaseTest`: ৬টা টেবিলের মধ্যে সামঞ্জস্য + "প্রতিটা অর্ডারের শেষ event = বর্তমান status" | ২ |
| BDD | `lifecycle.feature`: **flagship E2E** + decision table outline + reject + cancel-after-ship | ৭ run |

**Flagship scenario** (প্ল্যানের মূল E2E flow):
Register → Cart → Coupon দিয়ে Quote → Order → Stock যাচাই → Payment → Ship → Public tracking → Deliver → Timeline → Return → Approve → Refund → Restock → পুরো Timeline

## চালানোর নিয়ম
```bash
mvn -f backend/pom.xml test
mvn -f automation/pom.xml test -Dgroups=fulfillment
mvn -f automation/pom.xml test -Dgroups=returns
mvn -f automation/pom.xml test -Dcucumber.filter.tags="@flagship"
```

## ইন্টারভিউতে যা বলবেন
> "Order lifecycle-কে একটা explicit state machine বানিয়েছি। Status বদলানোর একটাই পথ, আর টেবিলের বাইরে যেকোনো পরিবর্তন 409 দেয়। ৪৯টা from/to জোড়ার প্রতিটার জন্য parameterized টেস্ট আছে। Return-এর refund আর restock নিয়ম decision table হিসেবে লেখা, আর API, unit ও BDD তিন স্তরেই একই টেবিল টেস্ট হয়। ৩০ দিনের return window injected Clock দিয়ে boundary-তে টেস্ট করি। Public tracking endpoint থেকে কোনো ব্যক্তিগত তথ্য ফাঁস হয় না, এটাও automation দিয়ে নিশ্চিত করা। আর একটা flagship BDD scenario রেজিস্ট্রেশন থেকে refund পর্যন্ত পুরো যাত্রা যাচাই করে।"

## শিক্ষা: যে বাগটা প্রথম রানে ধরা পড়েছিল
প্রথমবার চালানোর সময় ৩৪টা Cucumber scenario একসাথে `DuplicateStepDefinitionException` দিয়ে ব্যর্থ হয়েছিল। কারণ: `LifecycleSteps`-এ একই method-এ একই লেখা দিয়ে `@Given` আর `@When` দুটোই দেওয়া ছিল। Cucumber step মেলানোর সময় **Given/When/Then keyword দেখে না, শুধু লেখা দেখে**। তাই একই step দুবার রেজিস্টার হয়েছিল, আর তাতে পুরো suite থেমে গিয়েছিল। ঠিক করতে একটা annotation রাখা হয়েছে, কারণ সেটাই সব keyword-এর জন্য কাজ করে।

আরেকটা পর্যবেক্ষণ: root `pom.xml` থেকে `mvn test` চালালে automation-ও চলত, অথচ তখন backend চালু থাকত না, ফলে `Connection refused`। এখন root build শুধু backend টেস্ট চালায়। Automation চালাতে হয় আলাদাভাবে (`mvn -f automation/pom.xml test`), অথবা `mvn test -Pe2e` দিয়ে।
