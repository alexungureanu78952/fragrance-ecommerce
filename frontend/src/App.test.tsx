import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { api } from './api'

vi.mock('./api', () => ({
  api: {
    products: vi.fn(),
    product: vi.fn(),
    me: vi.fn(),
    cart: vi.fn(),
    brands: vi.fn(),
    categories: vi.fn(),
    adminOrders: vi.fn(),
    updateOrderStatus: vi.fn(),
  },
}))

const mockedApi = vi.mocked(api)

describe('storefront', () => {
  beforeEach(() => {
    window.location.hash = '#/'
    mockedApi.products.mockResolvedValue({
      items: [{
        id: 'product-1',
        brandId: 'brand-1',
        brandName: 'Nocturne',
        categoryId: 'category-1',
        categoryName: 'Woody',
        name: 'Dusk',
        description: 'Warm cedar and smoke.',
        price: 42,
        stockQuantity: 4,
      }],
      page: 0,
      size: 24,
      totalItems: 1,
      totalPages: 1,
    })
    mockedApi.me.mockRejectedValue(new Error('Not authenticated'))
  })

  it('renders a product and opens the accessible carousel detail view', async () => {
    render(<App />)
    expect(await screen.findByText('Dusk')).toBeInTheDocument()
    mockedApi.product.mockResolvedValue({
      id: 'product-1',
      brandId: 'brand-1',
      brandName: 'Nocturne',
      categoryId: 'category-1',
      categoryName: 'Woody',
      name: 'Dusk',
      description: 'Warm cedar and smoke.',
      price: 42,
      stockQuantity: 4,
    })
    window.location.hash = '#/product/product-1'
    window.dispatchEvent(new HashChangeEvent('hashchange'))
    await waitFor(() => expect(screen.getByRole('region', { name: /dusk image carousel/i })).toBeInTheDocument())
    expect(document.title).toBe('Dusk | Nocturne Fragrance')
    const structured = document.head.querySelector('script[type="application/ld+json"][data-product-id="product-1"]')
    expect(JSON.parse(structured?.textContent ?? '{}')).toMatchObject({
      '@type': 'Product', name: 'Dusk', brand: { name: 'Nocturne' },
      offers: { price: '42.00', priceCurrency: 'EUR', availability: 'https://schema.org/InStock' },
    })
    const nextImage = screen.getByRole('button', { name: 'Show product image 2' })
    fireEvent.click(nextImage)
    expect(nextImage).toBeInTheDocument()
  })

  it('shows an explicit empty state', async () => {
    mockedApi.products.mockResolvedValue({ items: [], page: 0, size: 24, totalItems: 0, totalPages: 0 })
    render(<App />)
    await waitFor(() => expect(screen.getByText('No fragrances match that search.')).toBeInTheDocument())
  })

  it('renders catalogue text as text instead of executable markup', async () => {
    mockedApi.products.mockResolvedValue({
      items: [{
        id: 'product-xss',
        brandId: 'brand-1',
        brandName: 'Nocturne',
        categoryId: 'category-1',
        categoryName: 'Woody',
        name: '<img src=x onerror=alert(1)>',
        description: 'Safe text',
        price: 42,
        stockQuantity: 4,
      }],
      page: 0,
      size: 24,
      totalItems: 1,
      totalPages: 1,
    })
    render(<App />)
    expect(await screen.findByText('<img src=x onerror=alert(1)>')).toBeInTheDocument()
    expect(document.querySelector('img[src="x"]')).toBeNull()
  })

  it('lets administrators advance an order through the allowed status transitions', async () => {
    const order = {
      id: 'order-1234abcd', customerId: 'customer-1', customerEmail: 'ana@example.test', customerName: 'Ana',
      status: 'PAID', total: 84, createdAt: '2026-11-01T10:00:00Z',
      items: [{ productName: 'Dusk', unitPrice: 42, quantity: 2, lineTotal: 84 }],
    }
    mockedApi.me.mockResolvedValue({ id: 'admin-1', email: 'admin@example.test', displayName: 'Admin', roles: ['ADMINISTRATOR'] })
    mockedApi.cart.mockResolvedValue({ items: [], total: 0 })
    mockedApi.brands.mockResolvedValue([])
    mockedApi.categories.mockResolvedValue([])
    mockedApi.adminOrders.mockResolvedValueOnce({ items: [order], page: 0, size: 20, totalItems: 1, totalPages: 1 })
      .mockResolvedValue({ items: [{ ...order, status: 'SHIPPED' }], page: 0, size: 20, totalItems: 1, totalPages: 1 })
    mockedApi.updateOrderStatus.mockResolvedValue({ ...order, status: 'SHIPPED' })
    window.location.hash = '#/admin'
    render(<App />)
    const table = await screen.findByRole('table', { name: /customer orders/i })
    expect(table).toHaveTextContent('ana@example.test')
    expect(screen.queryByRole('button', { name: 'Mark delivered' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Mark shipped' }))
    await waitFor(() => expect(mockedApi.updateOrderStatus).toHaveBeenCalledWith('order-1234abcd', 'SHIPPED'))
    expect(await screen.findByRole('button', { name: 'Mark delivered' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('is now shipped')
  })
})
