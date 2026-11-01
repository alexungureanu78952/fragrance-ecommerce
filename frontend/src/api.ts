export type Product = {
  id: string
  brandName: string
  categoryName: string
  name: string
  description: string
  price: number
  stockQuantity: number
}

export type ProductPage = {
  items: Product[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type Cart = {
  items: Array<{
    productId: string
    productName: string
    unitPrice: number
    quantity: number
    lineTotal: number
  }>
  total: number
}

export type User = {
  id: string
  email: string
  displayName: string
  roles: string[]
}

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (init.method && init.method !== 'GET') {
    const csrf = await fetch(`${API_URL}/auth/csrf`, { credentials: 'include' })
    if (csrf.ok) {
      const token = (await csrf.json()) as { token: string }
      headers.set('X-XSRF-TOKEN', token.token)
    }
  }
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { detail?: string }
    throw new Error(body.detail ?? `Request failed (${response.status})`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  products: (search = '') => request<ProductPage>(`/products?size=24${search ? `&search=${encodeURIComponent(search)}` : ''}`),
  product: (id: string) => request<Product>(`/products/${id}`),
  me: () => request<User>('/auth/me'),
  login: (email: string, password: string) => request<User>('/auth/login', {
    method: 'POST', body: JSON.stringify({ email, password }),
  }),
  register: (email: string, password: string, displayName: string) => request<User>('/auth/register', {
    method: 'POST', body: JSON.stringify({ email, password, displayName }),
  }),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
  cart: () => request<Cart>('/cart'),
  addToCart: (productId: string, quantity: number) => request<Cart>('/cart/items', {
    method: 'POST', body: JSON.stringify({ productId, quantity }),
  }),
  updateCart: (productId: string, quantity: number) => request<Cart>(`/cart/items/${productId}`, {
    method: 'PATCH', body: JSON.stringify({ quantity }),
  }),
  removeFromCart: (productId: string) => request<Cart>(`/cart/items/${productId}`, { method: 'DELETE' }),
}
