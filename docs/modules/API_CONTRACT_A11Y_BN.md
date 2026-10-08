# API contract টেস্ট আর accessibility (WCAG 2.1 AA) টেস্ট

দুটো নতুন test layer যোগ হয়েছে। প্রথমটা নিশ্চিত করে API-র "চুক্তি" ভাঙে না। দ্বিতীয়টা নিশ্চিত করে প্রতিবন্ধী ব্যবহারকারীরাও দোকান ব্যবহার করতে পারেন।

## ১. API contract: দুই দিক থেকে

| দিক | কী যাচাই করে | কোথায় |
|---|---|---|
| **Consumer (ব্যবহারকারী) দিক** | Storefront আর অন্য client যেসব response-এর উপর নির্ভর করে, তার আকার: দরকারি field, type, format (তারিখ, order number, JWT), enum (order status, role) | `ApiContractTest` + `automation/src/test/resources/contracts/*.json` (১২টা JSON Schema) |
| **Provider (backend) দিক** | চলমান backend-এর OpenAPI বিবরণ অনুমোদিত baseline (`docs/api/openapi.json`)-এর তুলনায় **ভাঙনকারী (breaking)** কিছু বদলেছে কি না | `.github/scripts/openapi-breaking-changes.sh` (openapi-diff), GitHub CI আর Jenkins-এর security stage |

**কঠোর schema (`additionalProperties: false`):**
- Response-এ নতুন, বদলানো বা "ফাঁস হওয়া" field এলেই টেস্ট fail করে।
- তাই contract বদলাতে হলে একই PR-এ schema-ও বদলাতে হয়। মানে চুক্তির পরিবর্তন সবসময় সচেতন সিদ্ধান্ত, দুর্ঘটনা নয়।

**গোপনীয়তার নিয়মও চুক্তির অংশ:**
- Public tracking response-এ customer id, email, দাম বা item থাকতে পারবে না।
- Review-এ reviewer-এর নাম masked থাকবে (`ja***`, `@` চিহ্ন নেই)।
- Payment-এ শুধু কার্ডের শেষ ৪ সংখ্যা থাকবে, পুরো নম্বর বা CVV কখনো না।
- কেউ ভুল করে এসব ফাঁস করলে contract টেস্টই সেটা ধরবে।

**কোন response-গুলো যাচাই হয়:**
- auth, product, product page (সাথে paging header `X-Total-Count`, `Link` ইত্যাদি) আর cart।
- checkout quote, order (place → pay → get), order list আর payment history।
- tracking (logged-in আর public দুটোই), product reviews আর wishlist।
- সব error (400, 401, 403, 404, 409) একই JSON আকারে।

**Breaking-change gate কীভাবে কাজ করে:**
1. CI backend চালিয়ে `/v3/api-docs` নেয়, আর baseline-এর সাথে openapi-diff দিয়ে তুলনা করে।
2. ফল অনুযায়ী আচরণ:
   - Endpoint মুছে ফেলা, response field সরানো, নতুন required input বা type বদলানো → **build লাল**।
   - শুধু নতুন endpoint বা নতুন optional field → সবুজ, তবে run-এর পেজে notice দেখায়।
3. **Self-test:** প্রতিবার gate নিজেকেও পরীক্ষা করে। Baseline থেকে `/api/products` মুছে দেওয়া একটা কপি দিলে gate-কে অবশ্যই "breaking" বলতে হবে, না বললে build লাল হয়।
4. **চুক্তি ইচ্ছাকৃতভাবে বদলালে:** GitHub → Actions → **API baseline** → **Run workflow** চালান। Workflow নতুন baseline commit করে দেয়, এটাই "অনুমোদন"।

```bash
mvn -f automation/pom.xml test -Dgroups=contract      # consumer-side contract টেস্ট
```

## ২. Accessibility: WCAG 2.1 level A আর AA
- **টুল:** Deque **axe-core** আসল browser-এ (Selenium) storefront-এর **৮টা পাতা** পরীক্ষা করে:
  - login, catalog, product page আর wishlist।
  - cart, checkout, order history আর order-ও-payment পাতা।
- **Gate:** "serious" বা "critical" ভুল থাকলে টেস্ট fail করে। "moderate" আর "minor" ভুল শুধু Allure-এ রিপোর্ট হয়।
- **কেন জরুরি (US-এর প্রেক্ষাপটে):** online দোকানের accessibility ADA-সংক্রান্ত মামলার একটা বড় বিষয়, তাই WCAG 2.1 AA হলো প্রচলিত লক্ষ্য। আর এটা শুধু আইনের ব্যাপার না, অনেক ক্রেতার সত্যিকার প্রয়োজন।

```bash
mvn -f automation/pom.xml test -Dgroups=a11y -Dheadless=false   # backend আর frontend চালু থাকতে হবে
```

### Test-first: টেস্ট আগে লাল, তারপর সংশোধন (DEF-018)
প্রথম CI run-এ **৮টা পাতার ৮টাই fail** করেছিল। যা পাওয়া গিয়েছিল:

| সমস্যা | মাত্রা | সংশোধন |
|---|---|---|
| `<html>`-এ ভাষা (`lang`) নেই, তাই screen reader ভুল উচ্চারণে পড়ে | serious | `<html lang="en">` |
| কমলা বোতামে সাদা লেখার contrast 2.9:1 (লাগে ≥ 4.5:1) | serious | Accent রঙ `#e07a1f` থেকে `#b45309`; ছোট ধূসর লেখা আর তারকাও গাঢ় করা হয়েছে |
| পরিমাণের ঘরে (catalog, cart) কোনো label নেই | critical | `aria-label="Quantity of <product>"` |
| Review sort আর return reason-এর select-এর নাম নেই | critical | `aria-label` |
| Table-এর "action" কলামের header খালি | minor | Screen reader-এর জন্য লুকানো লেখা (`.sr-only`) |
| Keyboard focus ভালো করে দেখা যায় না | (ম্যানুয়াল চেক) | `:focus-visible`-এ স্পষ্ট নীল outline |

- **পরের run:** ৮টা পাতাতেই **০টা** serious বা critical ভুল।
- **Local-এ আগেই যাচাই:** একই axe-core ব্যবহার করে mock backend-এর বিরুদ্ধে সব পাতা পরীক্ষা করা হয়েছিল, best-practice নিয়মসহ, আর সেখানেও ০টা ভুল।

### সীমা (সৎভাবে বলা)
- **Automated নিয়মের সীমা:** axe-core-এর মতো automated নিয়ম accessibility সমস্যার মোটামুটি এক-তৃতীয়াংশ থেকে অর্ধেক ধরতে পারে।
- **মানুষের পরীক্ষা এখনো দরকার:**
  - শুধু keyboard দিয়ে পুরো কেনাকাটা করা যায় কি না।
  - Screen reader (VoiceOver বা NVDA) দিয়ে পাতা বোঝা যায় কি না।
  - 200% zoom-এ layout ঠিক থাকে কি না।
- এগুলো release checklist-এ ম্যানুয়াল টেস্ট হিসেবে রাখা উচিত।

## ৩. Interview-এ
> "আমি API contract দুই দিক থেকে টেস্ট করি। Consumer দিকে কঠোর JSON Schema আছে, যাতে গোপনীয়তার নিয়মও আছে, যেমন public tracking-এ কোনো customer data থাকবে না। Provider দিকে OpenAPI breaking-change gate আছে, যার নিজের একটা self-test আছে।
>
> Accessibility-তে টেস্ট আগে লিখেছি। axe-core আটটা পাতাতেই WCAG 2.1 AA-র ভুল ধরল: contrast, label আর ভাষা। সংশোধনের পর সব শূন্য। তবে আমি জানি automated টেস্ট সব ধরে না, তাই keyboard আর screen reader দিয়ে ম্যানুয়াল পরীক্ষাও রাখি।"
