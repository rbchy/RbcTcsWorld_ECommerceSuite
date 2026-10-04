import React from 'react';
import { createRoot } from 'react-dom/client';
import './style.css';
import { clearSession, go, useRoute, useSession } from './session';
import Home from './pages/Home';
import Login from './pages/Login';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import Orders from './pages/Orders';
import OrderDetail from './pages/OrderDetail';
import ProductDetail from './pages/ProductDetail';
import Wishlist from './pages/Wishlist';

// data-testid attributes are the stable hooks used by Selenium (automation/.../pages/*.java).
function App() {
  const route = useRoute();
  const session = useSession();
  const needsLogin = ['/cart', '/checkout', '/orders', '/wishlist'].some(p => route.startsWith(p));

  let page;
  if (needsLogin && !session) page = <Login redirect={route} />;
  else if (route === '/login') page = <Login redirect="/" />;
  else if (route === '/cart') page = <Cart />;
  else if (route === '/checkout') page = <Checkout />;
  else if (route === '/orders') page = <Orders />;
  else if (route.startsWith('/orders/')) page = <OrderDetail id={route.split('/')[2]} />;
  else if (route === '/wishlist') page = <Wishlist />;
  else if (route.startsWith('/products/')) page = <ProductDetail id={route.split('/')[2]} />;
  else page = <Home />;

  return (
    <>
      <nav className="topbar">
        <a href="#/" className="brand" data-testid="nav-home">RbcTcsWorld</a>
        <div className="links">
          <a href="#/wishlist" data-testid="nav-wishlist">Wishlist</a>
          <a href="#/cart" data-testid="nav-cart">Cart</a>
          <a href="#/orders" data-testid="nav-orders">Orders</a>
          {session ? (
            <>
              <span className="who" data-testid="nav-user">{session.email}</span>
              <button className="link" data-testid="nav-logout" onClick={() => { clearSession(); go('/'); }}>Log out</button>
            </>
          ) : (
            <a href="#/login" data-testid="nav-login">Log in</a>
          )}
        </div>
      </nav>
      <main>{page}</main>
    </>
  );
}

createRoot(document.getElementById('root')).render(<App />);
