import React, { useEffect, useState } from 'react';
import api, { errorText, money } from '../api';
import { getSession, go } from '../session';
import Stars from './Stars';

const PAGE_SIZE = 20;

export default function Home() {
  const [products, setProducts] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [error, setError] = useState('');
  const [flash, setFlash] = useState('');

  // The catalog is paginated (DEF-007): 20 products per request, "Load more" fetches the next page.
  const load = (pageNo, append) =>
    api.get('/products', { params: { q, page: pageNo, size: PAGE_SIZE } })
      .then(r => {
        setProducts(prev => (append ? [...prev, ...r.data] : r.data));
        setTotal(Number(r.headers['x-total-count'] ?? r.data.length));
        setPage(pageNo);
        setError('');
      })
      .catch(e => { if (!append) setProducts([]); setError(errorText(e)); });

  useEffect(() => { load(0, false); }, [q]);

  const add = async (productId, quantity) => {
    if (!getSession()) { go('/login'); return; }
    try {
      const r = await api.post('/cart/items', { productId, quantity: Number(quantity) });
      setFlash(`Added to cart (${r.data.totalQuantity} item(s) in cart)`);
      setError('');
    } catch (e) {
      setFlash('');
      setError(errorText(e));
    }
  };

  const wish = async productId => {
    if (!getSession()) { go('/login'); return; }
    try {
      const r = await api.post('/wishlist', { productId });
      setFlash(r.status === 201 ? `Saved to wishlist (${r.data.count})` : 'Already on your wishlist');
      setError('');
    } catch (e) {
      setFlash('');
      setError(errorText(e));
    }
  };

  return (
    <>
      <header className="page-head">
        <h1>RbcTcsWorld E-Commerce</h1>
        <input data-testid="search-input" placeholder="Search products" value={q} onChange={e => setQ(e.target.value)} />
      </header>
      {error && <p data-testid="error-banner" className="error">{error}</p>}
      {flash && <p data-testid="flash" className="ok">{flash}</p>}
      <p className="muted small" data-testid="catalog-total">Showing {products.length} of {total} products</p>
      <section className="grid" data-testid="product-grid">
        {products.map(p => <ProductCard key={p.id} p={p} onAdd={add} onWish={wish} />)}
      </section>
      {products.length < total && (
        <p className="center"><button className="ghost" data-testid="load-more" onClick={() => load(page + 1, true)}>Load more</button></p>
      )}
    </>
  );
}

function ProductCard({ p, onAdd, onWish }) {
  const [qty, setQty] = useState(1);
  return (
    <article data-testid="product-card" data-sku={p.sku}>
      <h2><a href={`#/products/${p.id}`} data-testid="product-name">{p.name}</a></h2>
      <p className="muted">{p.category}</p>
      <Stars average={p.ratingAverage} count={p.ratingCount} />
      <p><strong data-testid="product-price">{money(p.price)}</strong></p>
      <p className="muted" data-testid="product-stock">Stock: {p.stock}</p>
      <div className="row">
        <input type="number" min="1" max="10" value={qty} data-testid="qty-input"
               onChange={e => setQty(e.target.value)} className="qty" />
        <button data-testid="add-to-cart" disabled={p.stock < 1} onClick={() => onAdd(p.id, qty)}>
          {p.stock < 1 ? 'Out of stock' : 'Add to cart'}
        </button>
        <button className="ghost" data-testid="add-to-wishlist" title="Save to wishlist" onClick={() => onWish(p.id)}>♡</button>
      </div>
    </article>
  );
}
