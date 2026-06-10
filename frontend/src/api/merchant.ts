import request from './request'

export const merchantApi = {
  // 店铺
  getShop: () => request.get('/merchant/shop'),
  updateShop: (data: any) => request.put('/merchant/shop', data),
  toggleStatus: () => request.post('/merchant/shop/toggle-status'),
  // 商品+分类
  getProducts: () => request.get('/merchant/products'),
  addProduct: (data: any) => request.post('/merchant/product', data),
  editProduct: (id: number, data: any) => request.put(`/merchant/product/${id}`, data),
  toggleProduct: (id: number) => request.post(`/merchant/product/toggle/${id}`),
  deleteProduct: (id: number) => request.delete(`/merchant/product/${id}`),
  moveProductCategory: (productId: number, categoryId: number) =>
    request.post('/merchant/product/move-category', null, { params: { productId, categoryId } }),
  addCategory: (name: string, sortOrder: number) =>
    request.post('/merchant/category', null, { params: { name, sortOrder } }),
  editCategory: (id: number, name: string, sortOrder: number) =>
    request.put(`/merchant/category/${id}`, null, { params: { name, sortOrder } }),
  deleteCategory: (id: number) => request.delete(`/merchant/category/${id}`),
  // 订单
  getOrders: (status?: string) => request.get('/merchant/orders', { params: { status } }),
  getOrderDetail: (id: number) => request.get(`/merchant/order/${id}`),
  acceptOrder: (id: number) => request.post(`/merchant/order/accept/${id}`),
  deliverOrder: (id: number) => request.post(`/merchant/order/deliver/${id}`),
  completeOrder: (id: number) => request.post(`/merchant/order/complete/${id}`),
  // 评价
  getReviews: () => request.get('/merchant/reviews'),
  replyReview: (id: number, reply: string) =>
    request.post(`/merchant/review/reply/${id}`, { reply }),
  // 收益
  getEarnings: () => request.get('/merchant/earnings'),
}
