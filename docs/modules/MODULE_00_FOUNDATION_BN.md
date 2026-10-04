# Module 0: ভিত্তি মেরামত (Foundation)

## কেন দরকার ছিল
আগের কোডে পাঁচটা সমস্যা ছিল। এগুলো না সারালে কোনো নতুন মডিউলের টেস্ট বিশ্বাসযোগ্য হতো না।

| সমস্যা | কী করা হয়েছে | কোন ফাইল |
|---|---|---|
| পোর্ট মিলত না (8081 বনাম 8080) | সব জায়গায় **8081** করা হয়েছে, কারণ আপনার Mac-এ Jenkins 8080 ব্যবহার করে | `application.yml`, `TestConfig.java`, `vite.config.js`, README |
| ফ্রন্টএন্ডে proxy ছিল না | `/api` রিকোয়েস্ট 8081-এ পাঠানোর proxy যোগ হয়েছে | `frontend/vite.config.js` |
| লগইন ছাড়াই প্রোডাক্ট মোছা যেত | GET সবার জন্য খোলা, POST/PUT/DELETE শুধু **ADMIN** পারবে | `SecurityConfig.java`, `JwtFilter.java` |
| Update শুধু stock বদলাত | এখন নাম, SKU, ক্যাটাগরি, দাম, stock সব বদলায়। SKU ডুপ্লিকেট হলে 409 | `Product.update()`, `ProductService.java` |
| সব ভুলে 500 আসত | `GlobalExceptionHandler` এখন সঠিক কোড দেয়: 400 / 401 / 403 / 404 / 409 | `common/exception/*` |

## নতুন যা যোগ হয়েছে
- **Role**: নতুন রেজিস্ট্রেশন সবসময় `CUSTOMER`। `ADMIN` অ্যাকাউন্ট অ্যাপ চালু হলে `DataSeeder` নিজে তৈরি করে (`admin@rbctcsworld.com` / `Admin@12345`, শুধু ডেভেলপমেন্টের জন্য)।
- **এক রকম JSON error**: প্রতিটা ভুলের উত্তর একই আকারে আসে, তাই টেস্টে সহজে assert করা যায়:
  ```json
  {"timestamp":"...","status":404,"error":"Not Found","message":"Product not found: 99","path":"/api/products/99","fieldErrors":{}}
  ```
- **401 আর 403 আলাদা**: টোকেন নেই বা ভুয়া হলে 401, টোকেন ঠিক কিন্তু role ভুল হলে 403 (`RestSecurityHandlers.java`)।
- **লগইনে নিরাপত্তা**: ভুল পাসওয়ার্ড আর অজানা email-এ একই বার্তা আসে, যাতে কেউ বুঝতে না পারে কোন email রেজিস্টার্ড (account enumeration প্রতিরোধ)।
- **Email normalize**: `New@Test.com` আর `new@test.com` একই ধরা হয়।
- **ডেমো ডেটা**: Flyway `V2__seed_products.sql` দিয়ে ৮টা প্রোডাক্ট তৈরি হয়। `ELEC-HEAD-005`-এর stock মাত্র 3, যাতে stock-এর সীমা টেস্ট করা যায়।
- **Soft delete**: প্রোডাক্ট মুছলে টেবিল থেকে যায় না, শুধু `active=false` হয়। পরে order history-র জন্য এটা দরকার।

## API চুক্তি (Contract)
| Endpoint | কে পারবে | সফল | ভুল হলে |
|---|---|---|---|
| POST /api/auth/register | সবাই | 201 | 400, 409 |
| POST /api/auth/login | সবাই | 200 | 400, 401 |
| GET /api/products, /api/products/{id} | সবাই | 200 | 400, 404 |
| POST /api/products | ADMIN | 201 | 400, 401, 403, 409 |
| PUT /api/products/{id} | ADMIN | 200 | 400, 401, 403, 404, 409 |
| DELETE /api/products/{id} | ADMIN | 204 | 401, 403, 404 |

## টেস্ট
- **Unit (Mockito)**: `AuthServiceTest`, `ProductServiceTest`। `updateChangesEveryField_regressionForStockOnlyBug` টেস্টটা আগের বাগ যেন আর ফিরে না আসে, সেটা নিশ্চিত করে (regression test)।
- **Integration (H2 + MockMvc)**: `ApiIntegrationTest` পুরো Spring অ্যাপ চালায়, Flyway migration চালায়, আর আসল HTTP status কোড যাচাই করে। এর জন্য Docker লাগে না।
- **Automation**: `AuthApiTest`, `ProductApiTest`, `catalog.feature`।

## ইন্টারভিউতে যা বলবেন
> "প্রথমে টেস্ট করার মতো ভিত্তি বানিয়েছি: এক রকম error contract, role-based access, আর প্রতিটা বাগের জন্য regression test। আগে টেস্টগুলো 500-কেও pass ধরত, যা আসলে বাগ লুকিয়ে রাখত।"
