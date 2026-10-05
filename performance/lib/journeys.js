import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import * as api from './api.js';
import { think } from './config.js';

// Business metrics (show up in the summary next to the HTTP metrics)
export const journeyTime = new Trend('purchase_journey_ms', true);   // cart -> quote -> order -> pay (server time, no think time)
export const ordersPaid = new Counter('orders_paid');
export const businessErrors = new Rate('business_errors');             // a 2xx answer with wrong content

const TERMS = ['mouse', 'keyboard', 'lamp', 'monitor', 'usb', 'chair'];
const pick = (arr) => arr[Math.floor(Math.random() * arr.length)];

/**
 * Anonymous shopper (most traffic on a real shop): catalog, search, product page, reviews.
 * data.catalogIds = ids of active products (from setup).
 */
export function browse(data) {
  group('browse', () => {
    const list = api.listProducts();
    check(list, { 'catalog 200': (r) => r.status === 200, 'catalog not empty': (r) => r.status === 200 && r.json().length > 0 });
    sleep(think(0.5, 1.5));

    const term = pick(TERMS);
    const found = api.search(term);
    check(found, {
      'search 200': (r) => r.status === 200,
      'search results match term': (r) => r.status === 200 && r.json().every((p) => p.name.toLowerCase().includes(term)),
    });
    sleep(think(0.5, 1.5));

    const id = pick(data.catalogIds);
    check(api.product(id), { 'product 200': (r) => r.status === 200 });
    const rv = api.reviews(id, pick(['newest', 'highest', 'lowest']));
    check(rv, {
      'reviews 200': (r) => r.status === 200,
      'review count matches list': (r) => r.status === 200 && r.json('reviewCount') === r.json('reviews').length,
    });
  });
  sleep(think());
}

// One customer per virtual user, created on its first purchase (signup is part of the load).
let me = null;

/**
 * Logged-in buyer: add to cart, ask for a quote, place the order and pay.
 * Verifies the money: the order total must equal the quote total.
 */
export function buy(data) {
  if (!me) me = api.newCustomer();
  if (!me) { businessErrors.add(1); return; }

  group('purchase', () => {
    const started = Date.now();
    let paused = 0;                                   // think time is excluded from the journey metric
    const nap = (sec) => { sleep(sec); paused += sec * 1000; };
    const productId = pick(data.shopIds);
    const qty = 1 + Math.floor(Math.random() * 2);

    const cart = api.addToCart(me.token, productId, qty);
    if (cart.status === 401) { me = null; return; }               // token expired (long soak test) -> new user next time
    check(cart, { 'add to cart 201': (r) => r.status === 201 });
    nap(think(0.5, 1));

    const q = api.quote(me.token);
    check(q, { 'quote 200': (r) => r.status === 200 });

    const order = api.placeOrder(me.token);
    const placed = check(order, { 'order 201': (r) => r.status === 201 });
    if (!placed) { api.clearCart(me.token); businessErrors.add(1); return; }

    const totalOk = q.status === 200 && order.json('total') === q.json('total');
    businessErrors.add(!totalOk);
    check(order, { 'order total equals quote total': () => totalOk });
    nap(think(0.5, 1));

    const paid = api.pay(me.token, order.json('id'));
    const ok = check(paid, {
      'pay 200': (r) => r.status === 200,
      'order is PAID': (r) => r.status === 200 && r.json('status') === 'PAID',
    });
    if (ok) {
      ordersPaid.add(1);
      journeyTime.add(Date.now() - started - paused);
    }
  });
  sleep(think(2, 5));
}

/** Creates the products every load test buys from, and reads the seeded catalog ids for browsing. */
export function prepare() {
  const admin = api.adminToken();
  const shopIds = [];
  for (let i = 0; i < 5; i++) {
    // huge stock so a long test never runs out (stock is not what these tests measure)
    shopIds.push(api.createProduct(admin, `Perf Item ${i}`, 10 + i * 7.5, 1000000));
  }
  const catalogIds = api.listProducts().json().map((p) => p.id);
  return { shopIds, catalogIds };
}
