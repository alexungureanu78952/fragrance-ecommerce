export type Product = {
  id: string
  brandId: string
  brandName: string
  categoryId: string
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

export type Order = {
  id: string
  status: string
  total: number
  items: Array<{
    productName: string
    unitPrice: number
    quantity: number
    lineTotal: number
  }>
}

export type OrderPage = {
  items: Order[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type AdminOrder = {
  id: string
  customerId: string
  customerEmail: string
  customerName: string
  status: string
  total: number
  createdAt: string
  items: Array<{
    productName: string
    unitPrice: number
    quantity: number
    lineTotal: number
  }>
}

export type AdminOrderPage = {
  items: AdminOrder[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type Reference = { id: string; name: string; description: string }

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (init.method && init.method !== 'GET') {
    const csrf = await fetch(`${API_URL}/auth/csrf`, { credentials: 'include' })
    if (csrf.ok) {
      const token = await csrf.json() as { token?: string; headerName?: string }
      if (!token.token || !token.headerName) {
        throw new Error('CSRF token response was invalid')
      }
      headers.set(token.headerName, token.token)
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
  login: (email: string, password: string, captchaToken?: string) => request<User>('/auth/login', {
    method: 'POST', body: JSON.stringify({ email, password, captchaToken }),
  }),
  register: (email: string, password: string, displayName: string, captchaToken?: string) => request<User>('/auth/register', {
    method: 'POST', body: JSON.stringify({ email, password, displayName, captchaToken }),
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
  createOrder: (idempotencyKey: string) => request<Order>('/orders', {
    method: 'POST', body: JSON.stringify({ idempotencyKey }),
  }),
  checkout: (orderId: string) => request<{ providerSessionId: string; checkoutUrl: string }>('/payments/checkout', {
    method: 'POST', body: JSON.stringify({ orderId }),
  }),
  orders: () => request<OrderPage>('/orders?size=20'),
  brands: () => request<Reference[]>('/brands'),
  categories: () => request<Reference[]>('/categories'),
  createProduct: (product: Omit<Product, 'id' | 'brandName' | 'categoryName'> & { brandId: string; categoryId: string }) => request<Product>('/admin/products', {
    method: 'POST', body: JSON.stringify(product),
  }),
  updateProduct: (id: string, product: Omit<Product, 'id' | 'brandName' | 'categoryName'> & { brandId: string; categoryId: string }) => request<Product>(`/admin/products/${id}`, {
    method: 'PATCH', body: JSON.stringify(product),
  }),
  deactivateProduct: (id: string) => request<void>(`/admin/products/${id}`, { method: 'DELETE' }),
  adminOrders: (status = '', page = 0) =>
    request<AdminOrderPage>(`/admin/orders?page=${page}&size=20${status ? `&status=${encodeURIComponent(status)}` : ''}`),
  updateOrderStatus: (orderId: string, status: string) =>
    request<AdminOrder>(`/admin/orders/${orderId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
}
