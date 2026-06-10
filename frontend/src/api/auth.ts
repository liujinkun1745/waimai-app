import request from './request'

export interface LoginParams { username: string; password: string }
export interface RegisterConsumerParams { username: string; phone: string; password: string; email?: string }
export interface RegisterMerchantParams {
  username: string; phone: string; password: string
  shopName: string; shopAddress: string; businessLicense: string; description?: string
}

export const authApi = {
  login: (params: LoginParams) => request.post('/auth/login', params),
  registerConsumer: (params: RegisterConsumerParams) => request.post('/auth/register/consumer', params),
  registerMerchant: (params: RegisterMerchantParams) => request.post('/auth/register/merchant', params),
}
