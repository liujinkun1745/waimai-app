import request from './request'

export const consumerApi = {
  // 首页
  getHome: (keyword?: string, sort?: string) =>
    request.get('/consumer/index', { params: { keyword, sort } }),
  // 商家详情
  getMerchantDetail: (id: number) => request.get(`/consumer/merchant/${id}`),
  // 按分类加载商品
  getProductsByCategory: (merchantId: number, categoryId: number) =>
    request.get(`/consumer/merchant/${merchantId}/category/${categoryId}`),
  // 订单
  getOrders: (status?: string) => request.get('/consumer/orders', { params: { status } }),
  getOrderDetail: (id: number) => request.get(`/consumer/order/${id}`),
  cancelOrder: (id: number) => request.post(`/consumer/order/cancel/${id}`),
  confirmReceived: (id: number) => request.post(`/consumer/order/confirm/${id}`),
  submitOrder: (data: any) => request.post('/consumer/order/submit', data),
  // 评价
  submitReview: (orderId: number, data: any) => request.post(`/consumer/order/${orderId}/review`, data),
  // 地址
  getAddresses: () => request.get('/consumer/address'),
  addAddress: (data: any) => request.post('/consumer/address', data),
  editAddress: (id: number, data: any) => request.put(`/consumer/address/${id}`, data),
  deleteAddress: (id: number) => request.delete(`/consumer/address/${id}`),
  // 优惠券
  getCoupons: () => request.get('/consumer/coupons'),
  getDailyCoupons: () => request.get('/consumer/coupons/daily'),
  claimCoupon: (data: any) => request.post('/consumer/coupons/claim', data),
  // 个人中心
  getProfile: () => request.get('/consumer/profile'),
  updateProfile: (data: any) => request.put('/consumer/profile', data),
  changePassword: (data: any) => request.put('/consumer/profile/password', data),
  // 余额
  getBalance: () => request.get('/consumer/balance'),
  recharge: (data: any) => request.post('/consumer/balance/recharge', data),
  // 搜索
  search: (keyword?: string) => request.get('/consumer/search', { params: { keyword } }),
}
