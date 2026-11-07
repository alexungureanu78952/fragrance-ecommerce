import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import HCaptcha from '@hcaptcha/react-hcaptcha'
import { api } from './api'
import type { AdminOrder, Cart, Order, Product, Reference, User } from './api'
import './App.css'

type Route = { page: 'catalog' | 'product' | 'cart' | 'login' | 'account' | 'admin' | 'payment-success' | 'payment-cancel'; id?: string }

function route(): Route {
  const [path, id] = window.location.hash.replace(/^#\/?/, '').split('/')
  if (path === 'product' && id) return { page: 'product', id }
  if (path === 'cart') return { page: 'cart' }
  if (path === 'login') return { page: 'login' }
  if (path === 'account') return { page: 'account' }
  if (path === 'admin') return { page: 'admin' }
  if (path === 'checkout' && id === 'success') return { page: 'payment-success' }
  if (path === 'checkout' && id === 'cancel') return { page: 'payment-cancel' }
  return { page: 'catalog' }
}

function money(value: number) {
  return new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' }).format(value)
}

const SITE_NAME = 'Nocturne Fragrance'
const DEFAULT_DESCRIPTION = 'Nocturne Fragrance — considered botanical and mineral compositions for quiet moments.'

function setMeta(selector: string, attribute: string, value: string) {
  const element = document.head.querySelector(selector)
  if (element) element.setAttribute(attribute, value)
}

// Keeps the document title, description, and social metadata in step with the current view.
function usePageMeta(title: string, description = DEFAULT_DESCRIPTION) {
  useEffect(() => {
    const fullTitle = title ? `${title} | ${SITE_NAME}` : `${SITE_NAME} | The scent edit`
    document.title = fullTitle
    setMeta('meta[name="description"]', 'content', description)
    setMeta('meta[property="og:title"]', 'content', fullTitle)
    setMeta('meta[property="og:description"]', 'content', description)
    setMeta('meta[name="twitter:title"]', 'content', fullTitle)
    setMeta('meta[name="twitter:description"]', 'content', description)
  }, [title, description])
}

// Publishes schema.org Product data for the product being viewed; textContent keeps it inert.
function useProductStructuredData(product: Product | null) {
  useEffect(() => {
    if (!product) return
    const script = document.createElement('script')
    script.type = 'application/ld+json'
    script.dataset.productId = product.id
    script.textContent = JSON.stringify({
      '@context': 'https://schema.org',
      '@type': 'Product',
      name: product.name,
      description: product.description,
      brand: { '@type': 'Brand', name: product.brandName },
      category: product.categoryName,
      offers: {
        '@type': 'Offer',
        price: product.price.toFixed(2),
        priceCurrency: 'EUR',
        availability: product.stockQuantity > 0 ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock',
      },
    })
    document.head.appendChild(script)
    return () => script.remove()
  }, [product])
}

function Header({ user, cartCount, onLogout }: { user: User | null; cartCount: number; onLogout: () => void }) {
  return <header className="site-header">
    <a className="brand" href="#/">NOCTURNE <span>FRAGRANCE</span></a>
    <nav aria-label="Primary navigation">
      <a href="#/">Shop</a>
      {user ? <a href="#/account">Account</a> : <a href="#/login">Sign in</a>}
      {user?.roles.includes('ADMINISTRATOR') && <a href="#/admin">Admin</a>}
      <a className="cart-link" href="#/cart" aria-label={`Cart with ${cartCount} items`}>Cart <b>{cartCount}</b></a>
      {user && <button className="text-button" onClick={onLogout}>Sign out</button>}
    </nav>
  </header>
}

type ProductForm = { brandId: string; categoryId: string; name: string; description: string; price: string; stockQuantity: string }

function Admin({ user }: { user: User | null }) {
  usePageMeta('Administration')
  const [products, setProducts] = useState<Product[]>([])
  const [brands, setBrands] = useState<Reference[]>([])
  const [categories, setCategories] = useState<Reference[]>([])
  const [form, setForm] = useState<ProductForm>({ brandId: '', categoryId: '', name: '', description: '', price: '', stockQuantity: '0' })
  const [editing, setEditing] = useState<string | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const load = () => api.products().then((page) => setProducts(page.items)).catch((reason: Error) => setError(reason.message))
  useEffect(() => { if (user?.roles.includes('ADMINISTRATOR')) { load(); api.brands().then(setBrands).catch((reason: Error) => setError(reason.message)); api.categories().then(setCategories).catch((reason: Error) => setError(reason.message)) } }, [user])
  if (!user || !user.roles.includes('ADMINISTRATOR')) return <main className="state error-state"><strong>Administrator access required.</strong><span>Your account does not have permission to manage products.</span><a className="button" href="#/">Return to shop</a></main>
  const update = (key: keyof ProductForm, value: string) => setForm({ ...form, [key]: value })
  const submit = (event: FormEvent) => { event.preventDefault(); setError(''); setMessage(''); const payload = { ...form, price: Number(form.price), stockQuantity: Number(form.stockQuantity) }; const action = editing ? api.updateProduct(editing, payload) : api.createProduct(payload); action.then(() => { setMessage(editing ? 'Product updated.' : 'Product created.'); setEditing(null); setForm({ brandId: '', categoryId: '', name: '', description: '', price: '', stockQuantity: '0' }); load() }).catch((reason: Error) => setError(reason.message)) }
  const edit = (product: Product) => { setEditing(product.id); setForm({ brandId: product.brandId, categoryId: product.categoryId, name: product.name, description: product.description, price: String(product.price), stockQuantity: String(product.stockQuantity) }) }
  return <main className="admin-layout"><div><p className="eyebrow">ADMINISTRATION</p><h1>Shape the collection.</h1><p className="muted">Manage products and fulfil customer orders. Server-side authorization remains authoritative.</p></div><section className="admin-grid"><form className="admin-form" onSubmit={submit}><p className="eyebrow">{editing ? 'EDIT PRODUCT' : 'NEW PRODUCT'}</p><label>Brand<select required value={form.brandId} onChange={(event) => update('brandId', event.target.value)}><option value="">Select a brand</option>{brands.map((brand) => <option key={brand.id} value={brand.id}>{brand.name}</option>)}</select></label><label>Category<select required value={form.categoryId} onChange={(event) => update('categoryId', event.target.value)}><option value="">Select a category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label><label>Name<input required maxLength={180} value={form.name} onChange={(event) => update('name', event.target.value)} /></label><label>Description<textarea required maxLength={4000} value={form.description} onChange={(event) => update('description', event.target.value)} /></label><div className="form-row"><label>Price<input required min="0.01" step="0.01" type="number" value={form.price} onChange={(event) => update('price', event.target.value)} /></label><label>Stock<input required min="0" type="number" value={form.stockQuantity} onChange={(event) => update('stockQuantity', event.target.value)} /></label></div>{error && <p className="error-text" role="alert">{error}</p>}{message && <p className="success-message" role="status">{message}</p>}<button className="button">{editing ? 'Save changes' : 'Create product'} <span>↗</span></button>{editing && <button type="button" className="text-button" onClick={() => setEditing(null)}>Cancel editing</button>}</form><div className="admin-products"><p className="eyebrow">ACTIVE PRODUCTS</p>{products.length === 0 && <p className="state">No active products found.</p>}{products.map((product) => <article className="admin-product" key={product.id}><div><strong>{product.name}</strong><span>{product.brandName} · {money(product.price)} · {product.stockQuantity} in stock</span></div><div><button className="text-button" onClick={() => edit(product)}>Edit</button><button className="text-button danger" onClick={() => api.deactivateProduct(product.id).then(load).catch((reason: Error) => setError(reason.message))}>Deactivate</button></div></article>)}</div></section><AdminOrders /></main>
}

const ORDER_STATUSES = ['PENDING_PAYMENT', 'PAID', 'SHIPPED', 'DELIVERED', 'PAYMENT_FAILED', 'CANCELED']
const NEXT_STATUSES: Record<string, string[]> = { PENDING_PAYMENT: ['CANCELED'], PAID: ['SHIPPED', 'CANCELED'], SHIPPED: ['DELIVERED'] }
const ACTION_LABELS: Record<string, string> = { SHIPPED: 'Mark shipped', DELIVERED: 'Mark delivered', CANCELED: 'Cancel' }

function statusLabel(status: string) {
  return status.replaceAll('_', ' ').toLowerCase()
}

function AdminOrders() {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [loaded, setLoaded] = useState<{ key: string; items: AdminOrder[]; totalPages: number; error: string } | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [actionError, setActionError] = useState('')
  const [message, setMessage] = useState('')
  const key = `${status}|${page}|${reload}`
  useEffect(() => {
    let active = true
    api.adminOrders(status, page)
      .then((result) => { if (active) setLoaded({ key, items: result.items, totalPages: result.totalPages, error: '' }) })
      .catch((reason: Error) => { if (active) setLoaded({ key, items: [], totalPages: 0, error: reason.message }) })
    return () => { active = false }
  }, [key, status, page])
  const loading = loaded?.key !== key
  const orders = loaded?.items ?? []
  const totalPages = loaded?.totalPages ?? 0
  const error = actionError || (loading ? '' : loaded?.error ?? '')
  const change = (order: AdminOrder, next: string) => {
    setBusy(order.id)
    setActionError('')
    setMessage('')
    api.updateOrderStatus(order.id, next).then(() => { setMessage(`Order ${order.id.slice(0, 8)} is now ${statusLabel(next)}.`); setReload(reload + 1) }).catch((reason: Error) => setActionError(reason.message)).finally(() => setBusy(null))
  }
  return <section className="admin-orders" aria-labelledby="admin-orders-heading">
    <div className="section-heading"><div><p className="eyebrow">ORDER MANAGEMENT</p><h2 id="admin-orders-heading">Customer orders</h2></div>
      <label className="filter">Status<select value={status} onChange={(event) => { setPage(0); setActionError(''); setMessage(''); setStatus(event.target.value) }}><option value="">All statuses</option>{ORDER_STATUSES.map((value) => <option key={value} value={value}>{statusLabel(value)}</option>)}</select></label>
    </div>
    {error && <p className="error-text" role="alert">{error}</p>}
    {message && <p className="success-message" role="status">{message}</p>}
    {loading && <p className="state">Loading orders<span className="pulse">...</span></p>}
    {!loading && !error && orders.length === 0 && <p className="state">No orders match this filter.</p>}
    {!loading && orders.length > 0 && <div className="table-scroll"><table className="orders-table">
      <caption className="sr-only">Customer orders, newest first</caption>
      <thead><tr><th scope="col">Order</th><th scope="col">Placed</th><th scope="col">Customer</th><th scope="col">Items</th><th scope="col">Total</th><th scope="col">Status</th><th scope="col">Actions</th></tr></thead>
      <tbody>{orders.map((order) => <tr key={order.id}>
        <td>{order.id.slice(0, 8)}</td>
        <td>{new Date(order.createdAt).toLocaleDateString('en-IE')}</td>
        <td><span className="cell-main">{order.customerName}</span><span className="cell-sub">{order.customerEmail}</span></td>
        <td>{order.items.map((item) => `${item.quantity} × ${item.productName}`).join(', ')}</td>
        <td>{money(order.total)}</td>
        <td><span className="status-tag">{statusLabel(order.status)}</span></td>
        <td>{(NEXT_STATUSES[order.status] ?? []).map((next) => <button key={next} className={`text-button${next === 'CANCELED' ? ' danger' : ''}`} disabled={busy === order.id} onClick={() => change(order, next)}>{ACTION_LABELS[next]}</button>)}</td>
      </tr>)}</tbody>
    </table></div>}
    {totalPages > 1 && <div className="pager"><button className="text-button" disabled={page === 0} onClick={() => setPage(page - 1)}>← Previous</button><span>Page {page + 1} of {totalPages}</span><button className="text-button" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Next →</button></div>}
  </section>
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
  usePageMeta('', 'Shop small-batch botanical and mineral fragrances from Nocturne. Explore woody, floral, and amber compositions.')
  const [search, setSearch] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<{ key: string; products: Product[]; error: string } | null>(null)
  const key = `${search}|${attempt}`
  useEffect(() => {
    let active = true
    api.products(search)
      .then((page) => { if (active) setResult({ key, products: page.items, error: '' }) })
      .catch((reason: Error) => { if (active) setResult({ key, products: [], error: reason.message }) })
    return () => { active = false }
  }, [key, search])
  const loading = result?.key !== key
  const products = loading ? [] : result.products
  const error = loading ? '' : result.error
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
      {error && <div className="state error-state"><strong>We could not load the collection.</strong><span>{error}</span><button className="button secondary" onClick={() => setAttempt(attempt + 1)}>Try again</button></div>}
      {!loading && !error && products.length === 0 && <p className="state">No fragrances match that search.</p>}
      <div className="product-grid">{products.map((product) => <article className="product-card" key={product.id}>
        <button className="card-link" onClick={() => onOpen(product.id)}><ProductVisual product={product} /><div className="card-copy"><p className="eyebrow">{product.brandName} · {product.categoryName}</p><h3>{product.name}</h3><span>{money(product.price)}</span></div></button>
      </article>)}</div>
    </section>
  </main>
}

function ProductDetails({ id, onAdded }: { id: string; onAdded: () => void }) {
  const [product, setProduct] = useState<Product | null>(null)
  usePageMeta(product?.name ?? 'Fragrance', product ? `${product.name} by ${product.brandName}: ${product.description}`.slice(0, 160) : undefined)
  useProductStructuredData(product)
  const [active, setActive] = useState(0)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  // The parent keys this component by product id, so each product starts with fresh state.
  useEffect(() => {
    let active = true
    api.product(id).then((next) => { if (active) setProduct(next) }).catch((reason: Error) => { if (active) setError(reason.message) })
    return () => { active = false }
  }, [id])
  if (error) return <div className="state error-state"><strong>Product unavailable</strong><span>{error}</span></div>
  if (!product) return <p className="state">Loading product<span className="pulse">...</span></p>
  return <main className="detail-layout"><ProductVisual product={product} active={active} onChange={setActive} /><section className="detail-copy"><p className="eyebrow">{product.brandName} · {product.categoryName}</p><h1>{product.name}</h1><p className="detail-price">{money(product.price)}</p><p>{product.description}</p><p className="stock">{product.stockQuantity > 0 ? `${product.stockQuantity} available` : 'Currently out of stock'}</p><button className="button" disabled={!product.stockQuantity} onClick={() => api.addToCart(product.id, 1).then(() => { setMessage('Added to your cart'); onAdded() }).catch((reason: Error) => setError(reason.message))}>Add to cart <span>↗</span></button>{message && <p className="success-message" role="status">{message}</p>}{error && <p className="error-text" role="alert">{error}</p>}<a className="back-link" href="#/">← Back to collection</a></section></main>
}

function CartPage({ cart, onChange, user }: { cart: Cart | null; onChange: () => void; user: User | null }) {
  usePageMeta('Shopping cart')
  const [error, setError] = useState('')
  const [checkingOut, setCheckingOut] = useState(false)
  if (!user) return <div className="state error-state"><strong>Sign in to view your cart.</strong><a className="button" href="#/login">Sign in</a></div>
  if (!cart) return <p className="state">Loading your cart<span className="pulse">...</span></p>
  const checkout = () => {
    setCheckingOut(true)
    setError('')
    api.createOrder(crypto.randomUUID()).then((order) => api.checkout(order.id)).then((session) => { window.location.href = session.checkoutUrl }).catch((reason: Error) => { setError(reason.message); onChange() }).finally(() => setCheckingOut(false))
  }
  return <main className="cart-layout"><section><p className="eyebrow">YOUR SELECTION</p><h1>Shopping cart</h1>{cart.items.length === 0 ? <p className="state">Your cart is waiting for a fragrance.</p> : cart.items.map((item) => <div className="cart-item" key={item.productId}><ProductVisual product={{ id: item.productId, brandId: '', brandName: '', categoryId: '', categoryName: '', name: item.productName, description: '', price: item.unitPrice, stockQuantity: 1 }} /><div><h3>{item.productName}</h3><p>{money(item.unitPrice)}</p><label>Quantity <input type="number" min="1" value={item.quantity} onChange={(event) => api.updateCart(item.productId, Number(event.target.value)).then(onChange)} /></label><button className="text-button" onClick={() => api.removeFromCart(item.productId).then(onChange)}>Remove</button></div><strong>{money(item.lineTotal)}</strong></div>)}</section>{cart.items.length > 0 && <aside className="summary"><p className="eyebrow">ORDER SUMMARY</p><div><span>Total</span><strong>{money(cart.total)}</strong></div><p>Payment is completed securely in Stripe test mode.</p>{error && <p className="error-text" role="alert">{error}</p>}<button className="button" disabled={checkingOut} onClick={checkout}>{checkingOut ? 'Preparing checkout...' : 'Proceed to checkout'} <span>↗</span></button></aside>}</main>
}

function PaymentResult({ canceled = false }: { canceled?: boolean }) {
  usePageMeta(canceled ? 'Checkout canceled' : 'Order confirmed')
  return <main className="state payment-result"><p className="eyebrow">{canceled ? 'CHECKOUT CANCELED' : 'PAYMENT RECEIVED'}</p><h1>{canceled ? 'Your selection is still here.' : 'Thank you for your order.'}</h1><p>{canceled ? 'No payment was taken. You can return to your cart whenever you are ready.' : 'Stripe will confirm the payment and your order status will update shortly.'}</p><div><a className="button" href={canceled ? '#/cart' : '#/account'}>{canceled ? 'Return to cart' : 'View order history'} <span>↗</span></a><a className="back-link" href="#/">Continue browsing</a></div></main>
}

function Account({ user }: { user: User | null }) {
  usePageMeta('Your account')
  const [orders, setOrders] = useState<Order[]>([])
  const [error, setError] = useState('')
  useEffect(() => { if (user) api.orders().then((page) => setOrders(page.items)).catch((reason: Error) => setError(reason.message)) }, [user])
  if (!user) return <main className="state error-state"><strong>Sign in to view your account.</strong><a className="button" href="#/login">Sign in</a></main>
  return <main className="account-layout"><div><p className="eyebrow">YOUR ACCOUNT</p><h1>Hello, {user.displayName}.</h1><p className="muted">{user.email}</p></div><section><div className="section-heading"><div><p className="eyebrow">ORDER HISTORY</p><h2>Your orders</h2></div></div>{error && <p className="error-text" role="alert">{error}</p>}{!error && orders.length === 0 && <p className="state">Your completed orders will appear here.</p>}{orders.map((order) => <article className="order-card" key={order.id}><div><strong>Order {order.id.slice(0, 8)}</strong><span>{order.status.replace('_', ' ')}</span></div><strong>{money(order.total)}</strong><ul>{order.items.map((item) => <li key={`${order.id}-${item.productName}`}>{item.quantity} × {item.productName}</li>)}</ul></article>)}</section></main>
}

function Login({ onLogin }: { onLogin: (user: User) => void }) {
  usePageMeta('Sign in')
  const [registering, setRegistering] = useState(false)
  const [form, setForm] = useState({ email: '', password: '', displayName: '' })
  const [captchaToken, setCaptchaToken] = useState('')
  const [error, setError] = useState('')
  const captchaSiteKey = import.meta.env.VITE_CAPTCHA_SITE_KEY
  const submit = (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (!captchaSiteKey || !captchaToken) {
      setError('Complete the CAPTCHA verification before continuing.')
      return
    }
    const action = registering
      ? api.register(form.email, form.password, form.displayName, captchaToken)
      : api.login(form.email, form.password, captchaToken)
    action.then(onLogin).catch((reason: Error) => setError(reason.message))
  }
  const switchMode = () => { setRegistering(!registering); setCaptchaToken(''); setError('') }
  return <main className="auth-layout"><div><p className="eyebrow">{registering ? 'JOIN THE EDIT' : 'WELCOME BACK'}</p><h1>{registering ? 'Create your account.' : 'Enter quietly.'}</h1><p>{registering ? 'Save your selections and follow your orders.' : 'Sign in to continue your fragrance journey.'}</p></div><form className="auth-form" onSubmit={submit}>{registering && <label>Display name<input required value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label>}<label>Email<input type="email" required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label><label>Password<input type="password" required value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>{captchaSiteKey ? <HCaptcha sitekey={captchaSiteKey} onVerify={setCaptchaToken} onExpire={() => setCaptchaToken('')} onError={() => setError('CAPTCHA could not load. Try again.')} /> : <p className="error-text" role="alert">CAPTCHA is not configured.</p>}{error && <p className="error-text" role="alert">{error}</p>}<button className="button" disabled={!captchaSiteKey || !captchaToken}>{registering ? 'Create account' : 'Sign in'} <span>↗</span></button><button type="button" className="text-button" onClick={switchMode}>{registering ? 'Already have an account? Sign in' : 'New here? Create an account'}</button></form></main>
}

function App() {
  const [current, setCurrent] = useState(route())
  const [user, setUser] = useState<User | null>(null)
  const [cart, setCart] = useState<Cart | null>(null)
  useEffect(() => { const changed = () => setCurrent(route()); window.addEventListener('hashchange', changed); api.me().then(setUser).catch(() => setUser(null)); return () => window.removeEventListener('hashchange', changed) }, [])
  useEffect(() => { if (user) api.cart().then(setCart).catch(() => setCart(null)) }, [user])
  const refreshCart = () => { if (user) api.cart().then(setCart) }
  const logout = () => api.logout().then(() => { setUser(null); setCart(null) }).then(() => { window.location.hash = '#/' })
  const visibleCart = user ? cart : null
  return <><Header user={user} cartCount={visibleCart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0} onLogout={logout} /><div className="page">{current.page === 'catalog' && <Catalog onOpen={(id) => { window.location.hash = `#/product/${id}` }} />}{current.page === 'product' && current.id && <ProductDetails key={current.id} id={current.id} onAdded={refreshCart} />}{current.page === 'cart' && <CartPage cart={visibleCart} onChange={refreshCart} user={user} />}{current.page === 'login' && <Login onLogin={(next) => { setUser(next); window.location.hash = '#/' }} />}{current.page === 'account' && <Account user={user} />}{current.page === 'admin' && <Admin user={user} />}{current.page === 'payment-success' && <PaymentResult />}{current.page === 'payment-cancel' && <PaymentResult canceled />}</div><footer><span>NOCTURNE FRAGRANCE</span><span>Educational shop · Stripe test mode only</span></footer></>
}

export default App
