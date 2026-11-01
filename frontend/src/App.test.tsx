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
})
