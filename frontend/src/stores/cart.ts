import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface CartItem {
  productId: number
  productName: string
  productImage: string
  price: number
  quantity: number
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const merchantId = ref<number | null>(null)
  const merchantName = ref('')

  const totalAmount = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )
  const totalCount = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  function setMerchant(id: number, name: string) {
    if (merchantId.value !== id) {
      items.value = []
      merchantId.value = id
      merchantName.value = name
      loadFromStorage(id)
    }
  }

  function addItem(product: any, quantity = 1) {
    const exist = items.value.find(i => i.productId === product.id)
    if (exist) {
      exist.quantity += quantity
    } else {
      items.value.push({
        productId: product.id,
        productName: product.name,
        productImage: product.image,
        price: product.price,
        quantity
      })
    }
    saveToStorage()
  }

  function removeItem(productId: number) {
    items.value = items.value.filter(i => i.productId !== productId)
    saveToStorage()
  }

  function updateQuantity(productId: number, quantity: number) {
    const item = items.value.find(i => i.productId === productId)
    if (item) {
      item.quantity = quantity
      if (item.quantity <= 0) removeItem(productId)
    }
    saveToStorage()
  }

  function clear() {
    items.value = []
    if (merchantId.value)
      localStorage.removeItem(`waimai_cart_${merchantId.value}`)
  }

  function saveToStorage() {
    if (merchantId.value)
      localStorage.setItem(`waimai_cart_${merchantId.value}`, JSON.stringify(items.value))
  }

  function loadFromStorage(id: number) {
    const saved = localStorage.getItem(`waimai_cart_${id}`)
    if (saved) {
      try { items.value = JSON.parse(saved) } catch { items.value = [] }
    }
  }

  return { items, merchantId, merchantName, totalAmount, totalCount,
           setMerchant, addItem, removeItem, updateQuantity, clear }
})
