import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api } from './api'
import type { Cart, Product, User } from './api'
import './App.css'

type Route = { page: 'catalog' | 'product' | 'cart' | 'login' | 'account'; id?: string }

function route(): Route {
  const [path, id] = window.location.hash.replace(/^#\/?/, '').split('/')
  if (path === 'product' && id) return { page: 'product', id }
  if (path === 'cart') return { page: 'cart' }
  if (path === 'login') return { page: 'login' }
  if (path === 'account') return { page: 'account' }
  return { page: 'catalog' }
}

function money(value: number) {
  return new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' }).format(value)
}

function Header({ user, cartCount, onLogout }: { user: User | null; cartCount: number; onLogout: () => void }) {
  return <header className="site-header">
    <a className="brand" href="#/">NOCTURNE <span>FRAGRANCE</span></a>
    <nav aria-label="Primary navigation">
      <a href="#/">Shop</a>
      {user ? <a href="#/account">Account</a> : <a href="#/login">Sign in</a>}
      <a className="cart-link" href="#/cart" aria-label={`Cart with ${cartCount} items`}>Cart <b>{cartCount}</b></a>
      {user && <button className="text-button" onClick={onLogout}>Sign out</button>}
    </nav>
  </header>
}

function ProductVisual({ product, active = 0, onChange }: { product: Product; active?: number; onChange?: (index: number) => void }) {
  const slides = ['botanical', 'amber', 'stone']
  return <div className={`product-visual ${slides[active]}`} role="region" aria-label={`${product.name} image carousel`}>
    <div className="visual-bottle" aria-hidden="true"><span>NOCTURNE</span></div>
    {onChange && <div className="carousel-controls">
      {slides.map((slide, index) => <button key={slide} className={index === active ? 'active' : ''} onClick={() => onChange(index)} aria-label={`Show product image ${index + 1}`} />)}
    </div>}
  </div>
}

function Catalog({ onOpen }: { onOpen: (id: string) => void }) {
  const [products, setProducts] = useState<Product[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    setLoading(true)
    api.products(search).then((page) => setProducts(page.items)).catch((reason: Error) => setError(reason.message)).finally(() => setLoading(false))
  }, [search])
  return <main>
    <section className="hero-section">
      <p className="eyebrow">THE SCENT EDIT · 2026</p>
      <h1>Quiet luxury,<br /><em>distilled.</em></h1>
      <p className="hero-copy">Small-batch fragrances for the spaces between moments. Explore our considered collection of botanical and mineral compositions.</p>
      <a className="button" href="#collection">Explore collection <span>↓</span></a>
    </section>
    <section id="collection" className="collection-section">
      <div className="section-heading"><div><p className="eyebrow">THE COLLECTION</p><h2>Find your signature</h2></div>
        <label className="search"><span className="sr-only">Search fragrances</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search scents" /></label>
      </div>
      {loading && <p className="state">Loading the collection<span className="pulse">...</span></p>}
      {error && <div className="state error-state"><strong>We could not load the collection.</strong><span>{error}</span><button className="button secondary" onClick={() => setSearch(search)}>Try again</button></div>}
      {!loading && !error && products.length === 0 && <p className="state">No fragrances match that search.</p>}
      <div className="product-grid">{products.map((product) => <article className="product-card" key={product.id}>
        <button className="card-link" onClick={() => onOpen(product.id)}><ProductVisual product={product} /><div className="card-copy"><p className="eyebrow">{product.brandName} · {product.categoryName}</p><h3>{product.name}</h3><span>{money(product.price)}</span></div></button>
      </article>)}</div>
    </section>
  </main>
}

function ProductDetails({ id, onAdded }: { id: string; onAdded: () => void }) {
  const [product, setProduct] = useState<Product | null>(null)
  const [active, setActive] = useState(0)
  const [message, setMessage] = useState('')
  useEffect(() => { api.product(id).then(setProduct).catch((reason: Error) => setMessage(reason.message)) }, [id])
  if (message) return <div className="state error-state"><strong>Product unavailable</strong><span>{message}</span></div>
  if (!product) return <p className="state">Loading product<span className="pulse">...</span></p>
  return <main className="detail-layout"><ProductVisual product={product} active={active} onChange={setActive} /><section className="detail-copy"><p className="eyebrow">{product.brandName} · {product.categoryName}</p><h1>{product.name}</h1><p className="detail-price">{money(product.price)}</p><p>{product.description}</p><p className="stock">{product.stockQuantity > 0 ? `${product.stockQuantity} available` : 'Currently out of stock'}</p><button className="button" disabled={!product.stockQuantity} onClick={() => api.addToCart(product.id, 1).then(() => { setMessage('Added to your cart'); onAdded() }).catch((reason: Error) => setMessage(reason.message))}>Add to cart <span>↗</span></button>{message && <p className="success-message" role="status">{message}</p>}<a className="back-link" href="#/">← Back to collection</a></section></main>
}

function CartPage({ cart, onChange, user }: { cart: Cart | null; onChange: () => void; user: User | null }) {
  if (!user) return <div className="state error-state"><strong>Sign in to view your cart.</strong><a className="button" href="#/login">Sign in</a></div>
  if (!cart) return <p className="state">Loading your cart<span className="pulse">...</span></p>
  return <main className="cart-layout"><section><p className="eyebrow">YOUR SELECTION</p><h1>Shopping cart</h1>{cart.items.length === 0 ? <p className="state">Your cart is waiting for a fragrance.</p> : cart.items.map((item) => <div className="cart-item" key={item.productId}><ProductVisual product={{ id: item.productId, brandName: '', categoryName: '', name: item.productName, description: '', price: item.unitPrice, stockQuantity: 1 }} /><div><h3>{item.productName}</h3><p>{money(item.unitPrice)}</p><label>Quantity <input type="number" min="1" value={item.quantity} onChange={(event) => api.updateCart(item.productId, Number(event.target.value)).then(onChange)} /></label><button className="text-button" onClick={() => api.removeFromCart(item.productId).then(onChange)}>Remove</button></div><strong>{money(item.lineTotal)}</strong></div>)}</section>{cart.items.length > 0 && <aside className="summary"><p className="eyebrow">ORDER SUMMARY</p><div><span>Total</span><strong>{money(cart.total)}</strong></div><p>Payment is completed securely in Stripe test mode.</p><button className="button" disabled>Proceed to checkout <span>↗</span></button></aside>}</main>
}

function Login({ onLogin }: { onLogin: (user: User) => void }) {
  const [registering, setRegistering] = useState(false)
  const [form, setForm] = useState({ email: '', password: '', displayName: '' })
  const [error, setError] = useState('')
  const submit = (event: FormEvent) => { event.preventDefault(); const action = registering ? api.register(form.email, form.password, form.displayName) : api.login(form.email, form.password); action.then(onLogin).catch((reason: Error) => setError(reason.message)) }
  return <main className="auth-layout"><div><p className="eyebrow">{registering ? 'JOIN THE EDIT' : 'WELCOME BACK'}</p><h1>{registering ? 'Create your account.' : 'Enter quietly.'}</h1><p>{registering ? 'Save your selections and follow your orders.' : 'Sign in to continue your fragrance journey.'}</p></div><form className="auth-form" onSubmit={submit}>{registering && <label>Display name<input required value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label>}<label>Email<input type="email" required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label><label>Password<input type="password" required value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>{error && <p className="error-text" role="alert">{error}</p>}<button className="button">{registering ? 'Create account' : 'Sign in'} <span>↗</span></button><button type="button" className="text-button" onClick={() => setRegistering(!registering)}>{registering ? 'Already have an account? Sign in' : 'New here? Create an account'}</button></form></main>
}

function App() {
  const [current, setCurrent] = useState(route())
  const [user, setUser] = useState<User | null>(null)
  const [cart, setCart] = useState<Cart | null>(null)
  useEffect(() => { const changed = () => setCurrent(route()); window.addEventListener('hashchange', changed); api.me().then(setUser).catch(() => setUser(null)); return () => window.removeEventListener('hashchange', changed) }, [])
  useEffect(() => { if (user) api.cart().then(setCart).catch(() => setCart(null)); else setCart(null) }, [user])
  const refreshCart = () => { if (user) api.cart().then(setCart) }
  const logout = () => api.logout().then(() => setUser(null)).then(() => { window.location.hash = '#/' })
  return <><Header user={user} cartCount={cart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0} onLogout={logout} /><div className="page">{current.page === 'catalog' && <Catalog onOpen={(id) => { window.location.hash = `#/product/${id}` }} />}{current.page === 'product' && current.id && <ProductDetails id={current.id} onAdded={refreshCart} />}{current.page === 'cart' && <CartPage cart={cart} onChange={refreshCart} user={user} />}{current.page === 'login' && <Login onLogin={(next) => { setUser(next); window.location.hash = '#/' }} />}{current.page === 'account' && <main className="state"><p className="eyebrow">YOUR ACCOUNT</p><h1>{user ? `Hello, ${user.displayName}.` : 'Sign in to view your account.'}</h1>{!user && <a className="button" href="#/login">Sign in</a>}</main>}</div><footer><span>NOCTURNE FRAGRANCE</span><span>Educational shop · Stripe test mode only</span></footer></>
}

export default App
