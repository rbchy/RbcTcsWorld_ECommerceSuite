# ধাপ ৭: CI/CD পূর্ণ করা (Docker, JaCoCo, Jenkins)

## ১. পুরো app একটা command-এ (Docker)

```bash
docker compose --profile app up -d --build     # database + backend + দোকান (storefront)
open http://localhost:5173                     # দোকান
docker compose --profile app down              # বন্ধ (-v দিলে database-ও মুছে যাবে)
```

প্রতিদিনের development আগের মতোই চলবে। `docker compose up -d` (`--profile app` ছাড়া) শুধু PostgreSQL চালু করে। তারপর backend চালান Eclipse বা `mvn spring-boot:run` দিয়ে, আর UI চালান `npm run dev` দিয়ে।

**কেন profile ব্যবহার করলাম:** পুরো app সবসময় চালু থাকলে Docker-এর backend আর আপনার Eclipse-এর backend দুটোই 8081 port চাইত, ফলে সংঘর্ষ হতো। Profile দিয়ে দুটো কাজের ধরন আলাদা থাকে।

| অংশ | যা করা হয়েছে | কেন |
|---|---|---|
| `backend/Dockerfile` | **Multi-stage:** Maven দিয়ে build হয়, কিন্তু চূড়ান্ত image-এ থাকে শুধু Java runtime (JRE 21) | Image ছোট হয়, আর build-এর টুল production-এ যায় না |
| | **non-root user** (uid 10001) | কেউ container ভাঙলেও root অধিকার পাবে না। CI এটা যাচাই করে |
| | **Health check** (`/actuator/health`) | Backend সত্যিই তৈরি হওয়ার পরেই অন্য অংশ চালু হয় |
| | `-XX:MaxRAMPercentage=75` | Java তার memory ঠিক করে container-এর সীমা দেখে, পুরো কম্পিউটারের memory দেখে নয় |
| | আগে `pom.xml` copy, তারপর কোড | কোড বদলালে লাইব্রেরি আবার নামাতে হয় না, তাই build দ্রুত হয় (layer cache) |
| `frontend/Dockerfile` + `nginx.conf` | Vite দিয়ে build, তারপর nginx দেয় | nginx `/api` request backend-এ পাঠায় (reverse proxy)। ফলে ব্রাউজার একটাই ঠিকানা দেখে, CORS লাগে না |
| | Security header আর `server_tokens off` | Clickjacking ঠেকানো যায়, আর nginx-এর version ফাঁস হয় না |
| `docker-compose.yml` | `depends_on: condition: service_healthy` | চালু হওয়ার ক্রম: database সুস্থ → backend সুস্থ → দোকান |

**CI job "Whole app in Docker":**
- **Image build:** প্রতিটা push-এ image build হয়, আর সব container সুস্থ (healthy) হওয়া পর্যন্ত অপেক্ষা করে। Image-এর আকার annotation-এ দেখায়: backend 391 MB, frontend 48 MB।
- **curl দিয়ে যাচাই:** দোকান খোলে কি না, nginx দিয়ে API-তে যাওয়া যায় কি না, security header আছে কি না, nginx-এর version লুকানো কি না, আর backend root হিসেবে চলছে না কি না।
- **Smoke টেস্ট:** তারপর container-গুলোর বিরুদ্ধেই **৪৪টা smoke টেস্ট** চলে: API, BDD আর UI। এতে প্রমাণ হয় যে **image-ও কাজ করে**, শুধু কোড নয়।

## ২. JaCoCo: code coverage
- **কী:** `mvn verify` চালালে টেস্টের সময় একটা agent নজর রাখে, কোডের কোন লাইন আর কোন শাখা (if/else) আসলে চলেছে। শেষে HTML, XML আর CSV রিপোর্ট তৈরি হয়।
- **মাপা ফলাফল:** **লাইন 96.9%**, **শাখা (branch) 86.6%**। Mutation testing-এর টেস্ট যোগ হওয়ার আগে ছিল 96.3% আর 78.8%।
- **Gate:** লাইন ≥ 96% আর শাখা ≥ 85%। মাপা মানের ঠিক নিচে রাখা হয়েছে। আগে ছিল 95% আর 77%।
  - **নিয়ম:** নতুন টেস্ট যোগ হলে gate বাড়ানো যাবে, কিন্তু কখনো কমানো যাবে না (ratchet)।
  - coverage এর নিচে নামলে `mvn verify` fail করে, Mac-এ চালালেও।
- **অনলাইন রিপোর্ট:** https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/coverage/
- **সবচেয়ে কম coverage কোথায়:** run-এর পেজে "Least covered packages" নামে দেখায়, যেমন `exception` 85% আর `admin` 88%। পরের টেস্ট কোথায় লিখতে হবে, এটা সেটা দেখিয়ে দেয়।

**Mutation testing (PIT):** টেস্ট আসলে ভুল ধরে কি না, সেটা মাপে। দেখুন [MUTATION_BN.md](MUTATION_BN.md)। Score 65.5% থেকে বেড়ে 84.9% হয়েছে, gate 84%।

**Interview-এর জন্য জরুরি:** 96% coverage মানে কোডে 96% বাগ নেই, তা **নয়**। এর মানে শুধু কোডের 96% লাইন অন্তত একবার চলেছে। Coverage দেখায় কোথায় টেস্ট **নেই**। কিন্তু টেস্ট কতটা ভালো, সেটা বোঝায় assertion-এর মান আর mutation testing।

## ৩. Jenkinsfile: GitHub Actions-এর সমান gate
| Stage | কী করে |
|---|---|
| Traceability matrix | `check_rtm.py` চালায় |
| Backend | unit আর integration টেস্ট, সাথে JaCoCo gate; JUnit রিপোর্ট আর coverage archive করে |
| Mutation testing (PIT) | parameter `RUN_MUTATION`; ব্যবসার কোডে mutant তৈরি করে, score ≥ 84% |
| Start the whole app | `docker compose --profile app up --wait` |
| Automation | API, DB, BDD আর UI টেস্ট container-গুলোর বিরুদ্ধে; Allure আর Cucumber রিপোর্ট archive করে |
| Performance | k6 smoke, flash-sale আর catalog। মেশিনে k6 থাকলে সেটা, না থাকলে Docker image |
| Security | OSV-Scanner আর OWASP ZAP, দুটোই compose network-এর ভেতরে চলে (`backend:8081`), ফলে Mac আর Linux দুটোতেই কাজ করে |
| Extra k6 (parameter) | load, stress, spike বা soak বেছে নেওয়া যায় |
| post | ব্যর্থ হলে container-এর log দেখায়; সবশেষে `down -v` দিয়ে সব পরিষ্কার করে |

**Parameter:** `RUN_UI`, `RUN_MUTATION`, `RUN_SECURITY` আর `K6_EXTRA` দিয়ে কোন অংশ চলবে তা ঠিক করা যায়।
**Options:** প্রতিটা লাইনে timestamp, ৬০ মিনিটের timeout, শেষ ২০টা build রাখা, আর একসাথে দুটো build না চালানো, কারণ port নির্দিষ্ট।

**Jenkinsfile আসলেই ঠিক কি না, কীভাবে জানলাম:**
- CI-তে "Jenkinsfile lint" job একটা আসল Jenkins (2.580.1) চালু করে, pipeline plugin-সহ। তারপর Jenkins-এর নিজস্ব declarative validator দিয়ে ফাইলটা যাচাই করে।
- **উল্টো প্রমাণও নিয়েছি:** একটা temporary branch-এ ইচ্ছা করে `stages`-কে `stagez` লিখেছিলাম। Job fail করেছে এই বার্তা দিয়ে: "Undefined section "stagez" … Missing required section "stages"।
- অর্থাৎ gate-টা শুধু দেখানোর জন্য নয়, সত্যিই ভুল ধরে।

### আপনার Mac-এর Jenkins-এ চালানো (8080)
1. Jenkins চালু করুন (`brew services start jenkins-lts`), তারপর http://localhost:8080 খুলুন। Suggested plugins install করুন।
2. **New Item → Pipeline** বেছে নিন। Definition-এ দিন **Pipeline script from SCM**, তারপর SCM: Git, URL: `https://github.com/rbchy/RbcTcsWorld_ECommerceSuite.git`, Branch: `*/main`, Script Path: `Jenkinsfile`।
3. Docker Desktop চালু থাকতে হবে।
4. Eclipse-এর backend আর `npm run dev` বন্ধ রাখুন, কারণ Jenkins container-এ নিজের backend চালাবে, আর দুটো একই port চাইবে।
5. **Build with Parameters** চাপুন। প্রথমবার Docker image build হতে কয়েক মিনিট লাগবে।

### সব রিপোর্ট এক পাতায়: "QA Reports"
- **কী:** প্রতিটা build শেষে `ci/qa_dashboard.py` একটা পাতা বানায়।
  - পাতায় থাকে সব gate-এর মূল সংখ্যা: টেস্ট, coverage, mutation score, প্রতিটা k6 টেস্টের p95, ZAP আর OSV। সবুজ মানে পাস, লাল মানে ব্যর্থ, ধূসর মানে ওই stage চলেনি।
  - প্রতিটা বিস্তারিত রিপোর্টের link থাকে: Allure, Cucumber, JaCoCo, PIT, k6 আর ZAP।
- **কোথায় দেখবেন:** build পাতার বাম দিকে **QA Reports** link। একই পাতা `qa-reports/` নামে artifact হিসেবেও থাকে।
- **একবার করতে হবে:**
  1. **HTML Publisher plugin install করুন:** Manage Jenkins → Plugins → Available plugins → `HTML Publisher` → Install। Plugin না থাকলেও build ভাঙবে না, শুধু link আসবে না।
  2. **Allure আর Cucumber-এর জন্য JavaScript চালু করুন:** এ দুটো রিপোর্টের JavaScript লাগে, কিন্তু Jenkins-এর CSP সেটা আটকায়। Manage Jenkins → Script Console-এ চালান:
     `System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")`
     এটা Jenkins restart না হওয়া পর্যন্ত থাকে।
     - ⚠️ শুধু নিজের ব্যক্তিগত Jenkins-এ করবেন। শেয়ার করা Jenkins-এ Resource Root URL বা Allure Jenkins plugin ব্যবহার করুন।
- **Dashboard পাতাটা নিজে CSP বদলানো ছাড়াই দেখা যায়:** এতে কোনো JavaScript বা inline CSS নেই।

## ৪. একটা আসল ঘটনা: নতুন critical CVE (ধাপ ৭ চলাকালীন)
- **কী হলো:** OSV-Scanner হঠাৎ build থামিয়ে দিল। `spring-webmvc 6.2.19`-এ **CVE-2026-47884 (CVSS 9.8)** প্রকাশিত হয়েছে, আর 6.2 লাইনে এখনো কোনো fix নেই।
- **বিশ্লেষণ:**
  - এই দুর্বলতা শুধু তখনই ব্যবহার করা যায়, যখন app **XsltView** দিয়ে view render করে।
  - আমাদের backend পুরোটাই JSON API: ১৩টা controller-এর সবগুলো `@RestController`। কোনো view, ViewResolver বা XSLT নেই। কোড খুঁজে এটা নিশ্চিত হয়েছি।
- **সিদ্ধান্ত:** ঝুঁকিটা লিখিতভাবে মেনে নেওয়া (`backend/osv-scanner.toml`), তিনটা শর্তসহ:
  1. **কারণ** লেখা থাকবে।
  2. **Guard test** থাকবে: `SecurityIntegrationTest#noXsltViewRenderingIsConfigured_CVE_2026_47884`। কেউ কখনো XsltView বা view দেখানো controller যোগ করলে build fail করবে, অর্থাৎ ঝুঁকি মেনে নেওয়ার ভিত্তিটাই পরীক্ষা হতে থাকে।
  3. **মেয়াদ থাকবে, ২০২৬-১১-০৫ পর্যন্ত।** তারপর finding নিজে থেকে আবার ফিরে আসবে। তখন হয় fix-যুক্ত version-এ upgrade, নয়তো আবার মূল্যায়ন।
- **Gate কঠোর করা:**
  - আগে শুধু fix থাকা critical দুর্বলতায় build থামত।
  - এখন **যেকোনো** critical থামায়, fix থাকুক বা না থাকুক, যদি না সেটা কারণ, টেস্ট আর মেয়াদ দিয়ে লিখিতভাবে মেনে নেওয়া থাকে।
  - মেনে নেওয়া ঝুঁকিগুলো প্রতিটা run-এ "Accepted risk" নামে দেখায়, যাতে কেউ ভুলে না যায়।

**দ্বিতীয় ঘটনা (৭ অক্টোবর):** আবার একটা নতুন CVSS 9.8 advisory এল, GHSA-j9f9-w8pj-32f8। এটাও spring-webmvc 6.2.19-এ, এবার Server-Sent Events (SSE) নিয়ে, আর 6.2 লাইনে কোনো open-source fix নেই।
- **Gate কাজ করেছে:** build সঙ্গে সঙ্গে লাল হয়েছে।
- **বিশ্লেষণ:** দুর্বলতাটা শুধু তখনই কাজে লাগানো যায়, যখন app SSE stream পাঠায়। আমাদের app কোনো SSE পাঠায় না।
- **সিদ্ধান্ত:** একই পদ্ধতিতে লিখিতভাবে মেনে নেওয়া হয়েছে।
- **নতুন guard test:** `SecurityIntegrationTest#noServerSentEventsOrFunctionalEndpoints_GHSA_j9f9_w8pj_32f8`। কেউ SSE বা WebMvc.fn endpoint যোগ করলে build fail করবে।
- **আরেকটা উন্নতি:** এখন run-এর পেজে প্রতিটা critical advisory-র CVE id আর শিরোনামও দেখায়।

**Interview-এ:**
> "একটা CVSS 9.8 CVE এসেছিল যার কোনো fix ছিল না। আমি অন্ধভাবে suppress করিনি, আবার আতঙ্কিতও হইনি। কোথায় দুর্বলতাটা কাজ করে বিশ্লেষণ করে দেখলাম আমাদের app-এ সেটা ব্যবহারযোগ্য নয়। তারপর কারণ, guard test আর মেয়াদসহ ঝুঁকিটা লিখিতভাবে মেনে নিয়েছি। Guard test নিশ্চিত করে যে সেই ভিত্তি কখনো চুপচাপ বদলাতে পারবে না।"

## এখন CI-র gate-গুলো (প্রতিটা push-এ)
1. Backend টেস্ট (২৭৯টা) আর JaCoCo coverage gate
2. Traceability matrix check
3. API, DB, BDD আর UI টেস্ট (২৬৯টা), আর k6 smoke, flash-sale ও catalog
4. Mutation testing (PIT): score ≥ 84%
5. পুরো app Docker-এ চালিয়ে তার বিরুদ্ধে ৪৪টা smoke টেস্ট
6. Jenkinsfile lint
7. OWASP ZAP আর OSV-Scanner
8. সব ঠিক থাকলে Allure, coverage আর mutation রিপোর্ট GitHub Pages-এ প্রকাশ
