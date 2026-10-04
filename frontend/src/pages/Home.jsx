import React, { useEffect, useState } from 'react';
import api, { errorText, money } from '../api';
import { getSession, go } from '../session';

export default function Home() {
  const [products, setProducts] = useState([]);
  const [q, setQ] = useState('');
  const [error, setError] = useState('');
  const [flash, setFlash] = useState('');

  useEffect(() => {
    api.get('/products', { params: { q } })
      .then(r => { setProducts(r.data); setError(''); })
      .catch(e => { setProducts([]); setError(errorText(e)); });
  }, [q]);

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

  return (
    <>
      <header className="page-head">
        <h1>RbcTcsWorld E-Commerce</h1>
        <input data-testid="search-input" placeholder="Search products" value={q} onChange={e => setQ(e.target.value)} />
      </header>
      {error && <p data-testid="error-banner" className="error">{error}</p>}
      {flash && <p data-testid="flash" className="ok">{flash}</p>}
      <section className="grid" data-testid="product-grid">
        {products.map(p => <ProductCard key={p.id} p={p} onAdd={add} />)}
      </section>
    </>
  );
}

function ProductCard({ p, onAdd }) {
  const [qty, setQty] = useState(1);
  return (
    <article data-testid="product-card" data-sku={p.sku}>
      <h2 data-testid="product-name">{p.name}</h2>
      <p className="muted">{p.category}</p>
      <strong data-testid="product-price">{money(p.price)}</strong>
      <p className="muted" data-testid="product-stock">Stock: {p.stock}</p>
      <div className="row">
        <input type="number" min="1" max="10" value={qty} data-testid="qty-input"
               onChange={e => setQty(e.target.value)} className="qty" />
        <button data-testid="add-to-cart" disabled={p.stock < 1} onClick={() => onAdd(p.id, qty)}>
          {p.stock < 1 ? 'Out of stock' : 'Add to cart'}
        </button>
      </div>
    </article>
  );
}
