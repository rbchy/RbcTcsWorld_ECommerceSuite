import React from 'react';

/** Read-only star line: "★★★★☆ 4.3 (12)". count = null shows only the stars (a single review). */
export default function Stars({ average, count, testId = 'product-rating' }) {
  const full = Math.round(Number(average ?? 0));
  return (
    <span className="stars" data-testid={testId} data-average={Number(average ?? 0).toFixed(1)} data-count={count ?? 0}>
      <span aria-hidden="true">{'★'.repeat(full)}{'☆'.repeat(5 - full)}</span>{' '}
      {count === null ? null : count ? <>{Number(average).toFixed(1)} <span className="muted">({count})</span></> : <span className="muted">No reviews yet</span>}
    </span>
  );
}
