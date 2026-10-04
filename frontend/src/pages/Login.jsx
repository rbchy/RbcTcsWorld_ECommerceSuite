import React, { useState } from 'react';
import api, { errorText } from '../api';
import { go, saveSession } from '../session';

export default function Login({ redirect = '/' }) {
  const [mode, setMode] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const submit = async e => {
    e.preventDefault();
    try {
      const r = await api.post(mode === 'login' ? '/auth/login' : '/auth/register', { email, password });
      saveSession(r.data);
      go(redirect === '/login' ? '/' : redirect);
    } catch (err) {
      setError(errorText(err));
    }
  };

  return (
    <section className="card narrow">
      <h1 data-testid="auth-title">{mode === 'login' ? 'Log in' : 'Create account'}</h1>
      <form onSubmit={submit}>
        <label>Email<input data-testid="email-input" type="email" value={email} onChange={e => setEmail(e.target.value)} /></label>
        <label>Password<input data-testid="password-input" type="password" value={password} onChange={e => setPassword(e.target.value)} /></label>
        {error && <p data-testid="auth-error" className="error">{error}</p>}
        <button data-testid="auth-submit" type="submit">{mode === 'login' ? 'Log in' : 'Register'}</button>
      </form>
      <button className="link" data-testid="auth-toggle" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); }}>
        {mode === 'login' ? 'New customer? Create an account' : 'Already registered? Log in'}
      </button>
    </section>
  );
}
