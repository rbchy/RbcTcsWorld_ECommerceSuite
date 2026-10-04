# Module 2: অর্ডার + ইনভেন্টরি (Orders + Inventory)

## কী হয়
গ্রাহক কার্ট থেকে **এক ক্লিকে অর্ডার** দেন। তখন একটা মাত্র database transaction-এ চারটা কাজ হয়:
1. কার্ট পড়া হয়। খালি হলে **400** "Cart is empty"।
2. প্রতিটা লাইনের stock **atomic ভাবে** কমানো হয়। কোনো লাইনে stock না থাকলে **409**।
3. `orders`, `order_items` আর `stock_movements` টেবিলে রেকর্ড লেখা হয়।
4. কার্ট খালি করা হয়।

কোনো একটা লাইন ব্যর্থ হলে **সবকিছু rollback** হয়: কোনো stock কমে না, অর্ডার তৈরি হয় না, কার্ট যেমন ছিল তেমন থাকে (all-or-nothing)।

## Overselling কীভাবে আটকানো হয়েছে (ইন্টারভিউয়ের সবচেয়ে গুরুত্বপূর্ণ অংশ)
সমস্যা: শেষ ৩টা পণ্য, আর ১০ জন একই মুহূর্তে কিনছে। সাধারণ "আগে পড়ো, তারপর লেখো" কোডে দুজনেই stock = 1 দেখবে, আর দুজনেই কিনে ফেলবে।

সমাধান: একটা **শর্তযুক্ত SQL** (`ProductRepository.decrementStock`):
```sql
UPDATE products SET stock = stock - :qty
WHERE id = :id AND active = true AND stock >= :qty
```
- Database এটাকে row lock দিয়ে একবারে চালায়। একজন জিতলে পরের জন আপডেট হওয়া stock দেখে।
- ফলাফল 1 row মানে পাওয়া গেছে, 0 row মানে stock নেই, তাই 409।
- দ্বিতীয় নিরাপত্তা: `CHECK (stock >= 0)` constraint, তাই database নিজেও কখনো ঋণাত্মক stock মানবে না।
- **Deadlock এড়াতে** লাইনগুলো সবসময় product id অনুযায়ী সাজিয়ে lock নেওয়া হয়।

PostgreSQL 16-এ যাচাই করা হয়েছে: stock 3, একসাথে ১০টা আপডেট, ফলাফল **ঠিক ৩ জন সফল, ০ error, শেষ stock 0**।

## ডাটাবেস (`V4__orders_inventory.sql`)
| টেবিল | কাজ |
|---|---|
| `orders` | অর্ডার নম্বর (`ORD-20261002-7KQ2MZ`), status, মোট পরিমাণ, subtotal, সময় |
| `order_items` | কেনার মুহূর্তের দাম, SKU আর নামের **snapshot**। পরে দাম বদলালেও পুরনো অর্ডার বদলায় না |
| `stock_movements` | প্রতিটা stock পরিবর্তনের audit trail: কত, কেন (`ORDER_PLACED` / `ORDER_CANCELLED`), পরে কত রইল |

GMP-র batch record-এর মতো: প্রতিটা stock পরিবর্তনের কারণসহ হিসাব থাকে, তাই যেকোনো সময় reconciliation করা যায়।

## Status (অবস্থা)
`PLACED` → `CANCELLED` (Module 2)। পরে আসবে: `PAID` → `SHIPPED` → `DELIVERED` → `RETURNED`।
- শুধু `PLACED` অর্ডার cancel করা যায়। দ্বিতীয়বার cancel করলে **409**, আর stock দ্বিতীয়বার ফেরত যায় না।

## API
| Endpoint | কে | সফল | ভুল |
|---|---|---|---|
| POST /api/orders | গ্রাহক | 201 | 400 খালি কার্ট, 401, 409 stock বা মুছে ফেলা পণ্য |
| GET /api/orders | গ্রাহক | 200 (নতুনটা আগে) | 401 |
| GET /api/orders/{id} | মালিক | 200 | 401, 404 (অন্যের অর্ডার) |
| POST /api/orders/{id}/cancel | মালিক | 200 | 401, 404, 409 |
| GET /api/admin/orders | ADMIN | 200 | 401, 403 |
| GET /api/admin/products/{id}/stock-movements | ADMIN | 200 | 401, 403 |

## কোড কোথায়
| ফাইল | কাজ |
|---|---|
| `inventory/InventoryService.java` | stock কমানো/ফেরত দেওয়ার একমাত্র জায়গা + movement লেখা |
| `product/ProductRepository.java` | `decrementStock` / `incrementStock` / `currentStock` query |
| `order/OrderService.java` | place / cancel / list, transaction সীমানা |
| `order/CustomerOrder.java`, `OrderItem.java` | Entity ("Order" নাম SQL-এর ORDER BY-এর সাথে মিলে যায়, তাই CustomerOrder) |
| `admin/AdminController.java` | Admin-এর জন্য সব অর্ডার আর stock history |

## টেস্ট কভারেজ
| স্তর | ফাইল | কী প্রমাণ করে |
|---|---|---|
| Unit | `InventoryServiceTest` (৪), `OrderServiceTest` (৭) | ব্যবসার নিয়ম, product-id ক্রমে lock, ব্যর্থ হলে কার্ট অক্ষত |
| Integration (H2) | `OrderIntegrationTest` (৮) | পুরো HTTP flow + JdbcTemplate দিয়ে DB যাচাই, rollback, IDOR, RBAC |
| Integration (H2) | `OrderConcurrencyIntegrationTest` | ১০টা thread: কখনো stock-এর বেশি বিক্রি হয় না |
| API automation | `OrderApiTest` (৮) | happy path, all-or-nothing, price snapshot, cancel একবার, IDOR, RBAC |
| **DB validation** | `OrderDatabaseTest` (৩), `@Tag("db")` | একটা API call-এর পর ৪টা টেবিলে ঠিক কী লেখা হলো |
| **Concurrency** | `InventoryConcurrencyTest`, `@Tag("concurrency")` | আসল PostgreSQL-এ ১০ জন বনাম stock ৫: ঠিক ৫টা 201, ৫টা 409, শেষ stock 0 |
| BDD | `order.feature` (৪টা scenario) | ব্যবসার ভাষায় E2E |

Automation-এর নিয়ম: টেস্ট **কখনো সরাসরি DB-তে লেখে না**। সব পরিবর্তন API দিয়ে হয়, আর DB শুধু যাচাইয়ের জন্য পড়া হয় (`DatabaseUtils` শুধু read-only)।

## চালানোর নিয়ম
```bash
mvn -f backend/pom.xml test                                    # unit + integration
mvn -f automation/pom.xml test -Dgroups=order                  # order API টেস্ট
mvn -f automation/pom.xml test -Dgroups=db                     # DB validation (docker postgres চালু থাকতে হবে)
mvn -f automation/pom.xml test -Dgroups=concurrency            # flash-sale race টেস্ট
mvn -f automation/pom.xml test -Dcucumber.filter.tags="@order"
```

## ইন্টারভিউতে যা বলবেন
> "Order placement-এ overselling আটকাতে conditional atomic UPDATE আর DB CHECK constraint ব্যবহার করেছি, আর deadlock এড়াতে product-id ক্রমে lock নিই। এটা প্রমাণ করতে একটা concurrency টেস্ট আছে: ১০ জন একসাথে ৫টা পণ্য কিনতে চায়, ঠিক ৫টা সফল হয়। প্রতিটা অর্ডারের পর JDBC দিয়ে চারটা টেবিল যাচাই করি। আর order_items-এ দামের snapshot রাখি, যাতে পরে দাম বদলালেও পুরনো invoice না বদলায়।"
