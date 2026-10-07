# Spring Boot 3.5 থেকে 4.1-এ upgrade

## ১. কেন, আর কেন এখনই
- **CVE-র মেয়াদ:** Spring MVC-র দুটো CVSS 9.8 দুর্বলতা (CVE-2026-47884 আর GHSA-j9f9-w8pj-32f8) Boot 3.5-এ "ঝুঁকি মেনে নেওয়া" হিসেবে ছিল। সেই exception-এর মেয়াদ ছিল **২০২৬-১১-০৫**।
  - Open-source fix আছে শুধু Spring Framework 7.0.9-এ, যেটা Spring Boot 4.1.1 আনে।
- **Support:** Spring Boot 3.5 open-source support-এর বাইরে। তাই Tomcat, Jackson আর Log4j-এর নিরাপত্তা patch হাতে pin করতে হচ্ছিল।
- **কাজের পদ্ধতি:** আলাদা branch (`upgrade/spring-boot-4`)-এ কাজ, আর সব CI gate সবুজ হলে তবেই `main`-এ merge।

## ২. কী বদলেছে

| বিষয় | আগে (Boot 3.5.16) | এখন (Boot 4.1.1) |
|---|---|---|
| Spring Framework | 6.2.19 | **7.0.9** |
| Tomcat | 10.1.60 (pin) | **11.0.26** |
| JSON | Jackson 2 (`com.fasterxml.jackson`) | **Jackson 3** (`tools.jackson`); API-র JSON আগের মতোই রাখতে `spring.jackson.use-jackson2-defaults: true` |
| Web starter | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| Flyway | শুধু `flyway-core` | `spring-boot-starter-flyway` (Boot 4-এ auto-configuration আলাদা module-এ) |
| MockMvc টেস্ট | `spring-boot-starter-test`-এর ভেতরে | আলাদা `spring-boot-starter-webmvc-test`; `@AutoConfigureMockMvc`-এর নতুন package `org.springframework.boot.webmvc.test.autoconfigure` |
| OpenAPI (ZAP-এর জন্য) | springdoc 2.8.9 | springdoc **3.0.3** |
| "generated security password" বন্ধ করা | `UserDetailsServiceAutoConfiguration` exclude | নিজস্ব `UserDetailsService` bean (`SecurityConfig.noFormLogin`), যাতে auto-config-এর package নামের উপর নির্ভর করতে না হয় |
| CVE exception | ২টা (মেয়াদসহ) | **০টা**; guard test দুটো এখন architecture rule হিসেবে রাখা |

## ৩. Upgrade চালানোর সময় টেস্ট যা ধরেছে

| কী ধরা পড়ল | কে ধরল | সমাধান |
|---|---|---|
| Boot 4.1.1-এর Tomcat 11.0.24-এ CVSS 9.8 (৩টা advisory), Jackson 3.1.5 আর 2.21.5-এ CVSS 7.5 | OSV-Scanner gate | একই release line-এর patch pin: Tomcat 11.0.26, Jackson 3.1.7 / 2.21.7, PostgreSQL 42.7.14। Boot পরের patch-এ নিজেই এগুলো আনলে pin তুলে দেওয়া হবে |
| **DEF-016:** search box-এ দ্রুত টাইপ করলে পুরনো উত্তর ("mo") নতুন ফলাফলের ("mouse") উপর বসে যেত | Selenium UI টেস্ট `searchFilters` | Storefront পুরনো উত্তর বাদ দেয় (`isCurrent` guard), সাথে 200 ms debounce |
| **DEF-017:** নামহীন query parameter (`/api/products?=x`) পাঠালে **500** আসত | OWASP ZAP (rule 100000), তারপর real server-এর বিরুদ্ধে API টেস্ট | Tomcat 11 এখন `getParameter()` থেকে `InvalidParameterException` ছোড়ে। Handler এখন একে **400** দেয়, সাথে unit test |

**DEF-017 থেকে শিক্ষা:**
- **MockMvc এটা ধরতে পারেনি:** আমাদের নতুন MockMvc টেস্ট পাস করেছিল, কারণ MockMvc-তে আসল Tomcat নেই।
- **কে ধরল:** শুধু real server-এর বিরুদ্ধে চলা টেস্ট (ZAP আর JDK HTTP client) এটা ধরেছে। একই কারণে automation suite চলে আসল backend-এর বিরুদ্ধে।
- **REST Assured-এর সীমা:** REST Assured নামহীন parameter পাঠাতেই পারে না। তাই এই টেস্ট লেখা হয়েছে JDK HTTP client দিয়ে।
- **CI এখন নিজেই কারণ দেখায়:** নতুন CI step `.github/scripts/backend-errors.sh` প্রতিটা 500-এর request, exception আর stack frame run-এর পেজে দেখায়। কারণটা এভাবেই পাওয়া গেছে।

## ৪. যা বদলায়নি (আর সেটাই প্রমাণ)
- **API-র JSON contract অপরিবর্তিত:** automation suite-এর একটা টেস্টও বদলাতে হয়নি, শুধু নতুন টেস্ট যোগ হয়েছে।
- **Database একই:** Flyway migration V1–V7 একই। Hibernate 7 schema `validate` পাস করেছে।
- **Mutation score অপরিবর্তিত:** 84.9%। k6-এর সব threshold পাস।

## ৫. Interview-এ
> "Spring Boot 3.5 থেকে 4.1-এ upgrade করেছি, যাতে Jackson 2 থেকে 3, Tomcat 10 থেকে 11 আর Spring 6 থেকে 7-এর বড় পরিবর্তন ছিল। কাজটা আলাদা branch-এ করেছি, আর প্রতিটা gate সবুজ না হওয়া পর্যন্ত merge করিনি। Gate-গুলো তিনটা জিনিস ধরেছে: নতুন Tomcat-এ critical CVE, storefront-এ একটা race condition, আর একটা regression যেখানে Tomcat 11-এর কারণে malformed request-এ 500 আসছিল। শেষেরটা unit টেস্ট বা MockMvc ধরতে পারেনি, শুধু real server-এর বিরুদ্ধে চলা টেস্ট ধরেছে।"
