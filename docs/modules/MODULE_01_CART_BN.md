# Module 1: শপিং কার্ট (Shopping Cart)

## ব্যবসার নিয়ম (Business Rules)
1. শুধু **active** প্রোডাক্ট কার্টে যোগ করা যায়। মুছে ফেলা বা অজানা প্রোডাক্ট হলে **404**।
2. একই প্রোডাক্ট আবার যোগ করলে নতুন লাইন হয় না, **পরিমাণ যোগ হয়** (2 + 3 = 5)।
3. প্রতি লাইনে পরিমাণ **১ থেকে ১০**। এর বাইরে হলে **400**।
4. পরিমাণ **stock-এর বেশি হতে পারবে না**। বেশি হলে **409** "Insufficient stock... available 3"।
5. প্রত্যেক গ্রাহক **শুধু নিজের কার্ট** দেখতে বা বদলাতে পারে। অন্যের item id দিলে **404** আসে, 403 না। এতে কেউ জানতেও পারে না যে item-টা আছে (**IDOR** সুরক্ষা)।
6. কার্টে দাম সংরক্ষণ হয় না, সবসময় **বর্তমান দাম** দেখায়।
7. কার্টে রাখার পর প্রোডাক্ট মুছে গেলে বা stock কমে গেলে লাইনে `available: false` দেখায়। পরের মডিউলে checkout এই লাইন গ্রহণ করবে না।

## ডাটাবেস (`V3__cart.sql`)
```
cart_items
 ├─ id           (PK)
 ├─ user_id      → users.id  (user মুছলে কার্টও মুছে যায়)
 ├─ product_id   → products.id
 ├─ quantity     CHECK (quantity > 0)
 ├─ added_at, updated_at
 └─ UNIQUE (user_id, product_id)   ← এক প্রোডাক্টে এক লাইন, ডাটাবেস লেভেলেও নিশ্চিত
```

## API
| Endpoint | কাজ | সফল | ভুল হলে |
|---|---|---|---|
| GET /api/cart | কার্ট দেখা | 200 | 401 |
| POST /api/cart/items `{productId, quantity}` | যোগ বা merge | 201 | 400, 401, 404, 409 |
| PUT /api/cart/items/{itemId} `{quantity}` | পরিমাণ বদলানো | 200 | 400, 401, 404, 409 |
| DELETE /api/cart/items/{itemId} | একটা লাইন মোছা | 200 | 401, 404 |
| DELETE /api/cart | পুরো কার্ট খালি করা | 204 | 401 |

উত্তরের নমুনা:
```json
{
  "items": [
    {"itemId": 4, "productId": 1, "sku": "ELEC-MOUSE-001", "name": "Wireless Mouse",
     "unitPrice": 24.99, "quantity": 2, "lineTotal": 49.98, "available": true}
  ],
  "totalQuantity": 2,
  "subtotal": 49.98
}
```

## কোড কোথায়
| ফাইল | কাজ |
|---|---|
| `cart/CartItem.java` | JPA entity (টেবিলের এক সারি) |
| `cart/CartItemRepository.java` | ডাটাবেস query। `findByIdAndUserId` দিয়েই IDOR সুরক্ষা হয় |
| `cart/CartService.java` | সব business rule এখানে |
| `cart/CartDtos.java` | Request/Response-এর আকার, validation (`@Min(1) @Max(10)`) |
| `cart/CartController.java` | HTTP endpoint |

## টেস্ট কভারেজ (Senior QA দৃষ্টিভঙ্গি)
| কৌশল | উদাহরণ টেস্ট |
|---|---|
| Boundary value | quantity 0, -1, 11 → 400; stock 3 হলে 3 চলবে, 4 → 409 |
| State transition | add → merge → update → remove, প্রতিটা ধাপে total যাচাই |
| Negative | অজানা প্রোডাক্ট, মুছে ফেলা প্রোডাক্ট, productId/quantity ছাড়া রিকোয়েস্ট |
| Security | টোকেন ছাড়া 401, ভুয়া টোকেন 401, অন্যের item বদলানো 404 (IDOR) |
| Data integrity | stock ছাড়ানো merge প্রত্যাখ্যাত হলে আগের পরিমাণ অপরিবর্তিত থাকে |

টেস্ট ফাইল:
- Backend unit: `CartServiceTest` (৭টা টেস্ট)
- Backend integration: `ApiIntegrationTest`-এর cart অংশ
- API automation: `automation/.../tests/api/CartApiTest.java` (১২টা টেস্ট মেথড, parameterized মিলিয়ে ১৪টা কেস)
- BDD: `automation/src/test/resources/features/cart.feature` (৭টা scenario, Outline মিলিয়ে ৮টা run)

## চালানোর নিয়ম
```bash
mvn -f backend/pom.xml test                                   # unit + integration
mvn -f automation/pom.xml test -Dgroups=cart                  # cart API টেস্ট (backend চালু থাকতে হবে)
mvn -f automation/pom.xml test -Dcucumber.filter.tags="@cart" # cart BDD
```

## ইন্টারভিউতে যা বলবেন
> "Cart মডিউলে boundary, state transition আর IDOR টেস্ট আছে। অন্যের cart item-এ 403 না দিয়ে 404 দিই, যাতে resource আছে কিনা সেটাও ফাঁস না হয়। এক প্রোডাক্টে এক লাইন নিয়মটা কোডের পাশাপাশি ডাটাবেসের UNIQUE constraint দিয়েও নিশ্চিত করেছি।"
