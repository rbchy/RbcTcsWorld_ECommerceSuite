# Module 3: Checkout + Coupon + Payment (mock)

## দামের হিসাব (`PricingService`)
```
merchandise = subtotal − coupon discount
shipping    = merchandise ≥ 50.00 হলে 0, নাহলে 5.99
tax         = merchandise × 6%       (shipping-এর উপর tax নেই)
total       = merchandise + shipping + tax       (সব টাকা HALF_UP, ২ দশমিক)
```
মানগুলো `application.yml`-এর `app.pricing`-এ রাখা, কোডে hard-code করা না।

**Boundary উদাহরণ (টেস্টে আছে):**

| Subtotal | Discount | Shipping | Tax | Total | কেন গুরুত্বপূর্ণ |
|---|---|---|---|---|---|
| 49.99 | 0 | 5.99 | 3.00 | 58.98 | threshold-এর ১ cent নিচে |
| 50.00 | 0 | 0.00 | 3.00 | 53.00 | ঠিক threshold-এ, তাই free shipping |
| 55.00 | 5.00 | 0.00 | 3.00 | 53.00 | discount-এর পরও 50, তাই free |
| 54.99 | 5.00 | 5.99 | 3.00 | 58.98 | discount-এর পর 49.99, তাই shipping লাগবে |
| 0.10 | 0 | 5.99 | 0.01 | 6.10 | 0.006 tax HALF_UP-এ 0.01 হয় |

## Coupon-এর নিয়ম (এই ক্রমে যাচাই হয়)
1. Code আছে আর active কিনা। না হলে **400** "Invalid coupon code"। ছোট হাতের অক্ষরেও চলে (`welcome10`)।
2. মেয়াদের ভেতরে কিনা। না হলে **400** "expired" বা "not active yet"।
3. ন্যূনতম অর্ডার পূরণ হয়েছে কিনা। না হলে **400** "Minimum order amount"।
4. এই গ্রাহক আগে ব্যবহার করেছেন কিনা। করে থাকলে **409** "already used"। ডাটাবেসেও `UNIQUE(coupon_id, user_id)` দিয়ে আটকানো।
5. মোট ব্যবহারের সীমা পার হয়েছে কিনা। হলে **409** "usage limit reached"। stock-এর মতোই atomic UPDATE দিয়ে আটকানো।

| Seed coupon | ধরন | নিয়ম |
|---|---|---|
| WELCOME10 | 10% | প্রতি গ্রাহক একবার |
| SAVE5 | $5 | ন্যূনতম $25 |
| EXPIRED20 | 20% | মেয়াদ শেষ (2020) |
| FUTURE15 | 15% | 2099 থেকে চালু |
| DISABLED | $10 | বন্ধ করা |

Discount কখনো subtotal-এর বেশি হয় না, তাই total কখনো ঋণাত্মক হয় না (ডাটাবেসেও `CHECK (total >= 0)`)।

## Quote: "Review your order"
`POST /api/checkout/quote` শুধু হিসাব দেখায়। **কিছুই সংরক্ষণ হয় না**: stock reserve হয় না, coupon ব্যবহৃত হিসেবে গণ্য হয় না। টেস্টে দুবার quote করার পরও coupon দিয়ে অর্ডার করা যায়, এটা প্রমাণ করা আছে।

## Payment (নকল gateway, Stripe-এর test card-এর মতো)
| Card | ফলাফল | HTTP |
|---|---|---|
| 4242 4242 4242 4242 | সফল, order হয় PAID | 200 |
| 4000 0000 0000 0002 | Card declined | **402** |
| 4000 0000 0000 9995 | Insufficient funds | **402** |
| 4242 4242 4242 4241 | Luhn checksum ভুল | 400 |
| মেয়াদ শেষ / মাস 13 / CVV 2 অক্ষর | Validation | 400 |

**নিরাপত্তা (PCI-DSS নীতি):**
- পুরো card নম্বর আর CVV **কখনো সংরক্ষণ হয় না**, শুধু শেষ ৪ সংখ্যা (`card_last4`)।
- `PayRequest.toString()` card data লুকিয়ে রাখে, তাই ভুল করে log হলেও নম্বর ফাঁস হবে না।
- DB টেস্ট যাচাই করে যে কোনো টেবিলে `card_number` বা `cvv` কলামই নেই।

**সূক্ষ্ম কিন্তু গুরুত্বপূর্ণ বিষয় (ইন্টারভিউয়ের জন্য দারুণ):**
Card declined হলে exception ছোড়া হয় (402)। Spring-এর নিয়মে exception হলে পুরো transaction rollback হয়, ফলে FAILED payment-এর রেকর্ডও মুছে যেত। অথচ fraud বিশ্লেষণের জন্য ব্যর্থ চেষ্টার রেকর্ড রাখা জরুরি। তাই `@Transactional(noRollbackFor = PaymentDeclinedException.class)` দুই স্তরেই (OrderService আর PaymentService) দেওয়া হয়েছে। `declinedPaymentIs402ButTheFailedAttemptIsKept` টেস্ট এটা প্রমাণ করে।

## Order-এর অবস্থা (state machine)
```
PLACED ──pay──> PAID
PLACED ──cancel──> CANCELLED              (stock ফেরত)
PAID   ──cancel──> CANCELLED              (stock ফেরত + পুরো টাকা refund)
PAID   ──pay──> 409 (দুবার charge হবে না)
CANCELLED ──pay/cancel──> 409
```

## API
| Endpoint | সফল | ভুল |
|---|---|---|
| POST /api/checkout/quote `{couponCode?}` | 200 | 400, 401, 409 |
| POST /api/orders `{couponCode?}` | 201 | 400, 401, 409 |
| POST /api/orders/{id}/pay `{cardNumber, expiryMonth, expiryYear, cvv}` | 200 | 400, 402, 404, 409 |
| GET /api/orders/{id}/payments | 200 | 401, 404 |
| POST /api/admin/coupons (ADMIN) | 201 | 400, 401, 403, 409 |
| GET /api/admin/coupons (ADMIN) | 200 | 401, 403 |

## ডাটাবেস (`V5__checkout_coupons_payments.sql`)
- `orders`-এ নতুন কলাম: `discount`, `shipping_fee`, `tax`, `total`, `coupon_code`, `paid_at`। আগের অর্ডারে `total = subtotal` বসানো হয়, তাই আপনার পুরনো ডেটাও ঠিক থাকে।
- নতুন টেবিল: `coupons`, `coupon_redemptions`, `payment_transactions` (CHARGE/REFUND × SUCCEEDED/FAILED)।

## টেস্ট কভারেজ
| স্তর | ফাইল | সংখ্যা |
|---|---|---|
| Unit | `PricingServiceTest`, `CouponServiceTest`, `PaymentServiceTest`, `OrderServiceTest` (নতুন ৩টা) | ৫৭টা unit test মোট |
| Integration (H2) | `CheckoutPaymentIntegrationTest` | ১১ |
| API | `CheckoutApiTest`, `PaymentApiTest` | ১৪ + ৭ (parameterized মিলিয়ে) |
| DB + Security | `PaymentDatabaseTest` | ৪ |
| BDD | `checkout.feature` (৩টা Scenario Outline + ১টা scenario = ১২টা run) | ১২ |

ব্যবহৃত টেস্ট কৌশল: boundary value, equivalence partitioning (card-এর ধরন), state transition (order status), decision table (coupon-এর নিয়ম), security (IDOR, RBAC, PCI), আর data integrity (total = subtotal − discount + shipping + tax)।

## চালানোর নিয়ম
```bash
mvn -f backend/pom.xml test
mvn -f automation/pom.xml test -Dgroups=checkout
mvn -f automation/pom.xml test -Dgroups=payment
mvn -f automation/pom.xml test -Dgroups=security
mvn -f automation/pom.xml test -Dcucumber.filter.tags="@checkout"
```

## ইন্টারভিউতে যা বলবেন
> "Checkout-এ boundary-value টেস্ট দিয়ে free-shipping threshold আর tax rounding যাচাই করি। Coupon-এর নিয়ম decision table হিসেবে সাজানো, আর ব্যবহারের সীমা atomic UPDATE দিয়ে আটকানো। Payment-এ Stripe-স্টাইলের test card দিয়ে প্রতিটা ফলাফল নিশ্চিতভাবে তৈরি করতে পারি। একটা সূক্ষ্ম বাগও ধরেছি: declined payment-এর exception transaction rollback করে failed attempt-এর রেকর্ড মুছে দিত। noRollbackFor দিয়ে ঠিক করেছি, আর একটা টেস্ট দিয়ে প্রমাণ রেখেছি। DB টেস্ট নিশ্চিত করে যে পুরো card নম্বর বা CVV কোথাও সংরক্ষণ হয় না।"
