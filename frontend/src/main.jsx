import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import axios from 'axios';
import './style.css';

// data-testid attributes are stable hooks for Selenium (automation/.../pages/HomePage.java).
function App() {
  const [products, setProducts] = useState([]);
  const [q, setQ] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    axios.get('/api/products', { params: { q } })
      .then(r => { setProducts(r.data); setError(''); })
      .catch(() => { setProducts([]); setError('Could not load products. Is the backend running on port 8081?'); });
  }, [q]);

  return (
    <main>
      <header>
        <h1>RbcTcsWorld E-Commerce</h1>
        <input data-testid="search-input" placeholder="Search products" value={q}
               onChange={e => setQ(e.target.value)} />
      </header>
      {error && <p data-testid="error-banner" className="error">{error}</p>}
      <section className="grid" data-testid="product-grid">
        {products.map(p => (
          <article key={p.id} data-testid="product-card">
            <h2 data-testid="product-name">{p.name}</h2>
            <p>{p.category}</p>
            <strong data-testid="product-price">${p.price}</strong>
            <p>Stock: {p.stock}</p>
          </article>
        ))}
      </section>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
