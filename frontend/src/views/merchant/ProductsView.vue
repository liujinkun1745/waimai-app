<template>
  <div class="products-page">
    <!-- 头部 -->
    <div class="header-gradient" style="justify-content:space-between;">
      <span>📦 商品管理</span>
      <div style="display:flex;align-items:center;gap:8px;">
        <el-button type="warning" size="small" @click="openCatDialog">+ 分类</el-button>
        <span style="font-size:20px;cursor:pointer;" @click="$router.push('/merchant/shop')">⚙️</span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!categories.length && !products.length" style="padding:40px;text-align:center;">
      <el-empty description="暂无商品，请先添加分类" />
      <el-button type="warning" @click="openCatDialog" style="width:100%;margin-top:12px;">添加第一个分类</el-button>
    </div>

    <!-- 分类折叠 -->
    <el-collapse v-model="activeCats" class="product-collapse">
      <el-collapse-item
        v-for="cat in categories"
        :key="cat.id"
        :name="cat.id"
      >
        <template #title>
          <div class="pc-title">
            <span class="pc-cat-name">{{ cat.name }}</span>
            <el-tag size="small" round>{{ getCatProductCount(cat.id) }} 件</el-tag>
            <span style="flex:1;"></span>
            <el-button
              text
              size="small"
              type="danger"
              @click.stop="handleDeleteCat(cat.id, cat.name)"
              class="pc-cat-del"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </template>

        <!-- 空分类 -->
        <div v-if="getCatProducts(cat.id).length === 0" class="pc-empty-cat">
          此分类暂无商品
        </div>

        <!-- 商品行 -->
        <div
          v-for="p in getCatProducts(cat.id)"
          :key="p.id"
          class="pc-product-row"
        >
          <div class="pcp-avatar">
            <el-image :src="p.image" fit="cover" style="width:56px;height:56px;border-radius:8px;">
              <template #error><span style="font-size:28px;">🍽️</span></template>
            </el-image>
          </div>
          <div class="pcp-info">
            <div class="pcp-name">{{ p.name }}</div>
            <div class="pcp-meta">
              <span class="pcp-price">¥{{ p.price }}</span>
              <span>库存 {{ p.stock }}</span>
              <span>已售 {{ p.sales }}</span>
            </div>
            <div class="pcp-status">
              <el-switch
                :model-value="p.status === '上架'"
                @change="toggleStatus(p.id)"
                size="small"
                active-text="上架"
                inactive-text="下架"
              />
            </div>
          </div>
          <div class="pcp-actions">
            <el-button circle size="small" @click.stop="openEdit(p)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button circle size="small" type="danger" @click.stop="handleDelete(p.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <el-button type="primary" text @click="openAdd(cat.id)" class="pcp-add-btn">
          <el-icon><Plus /></el-icon> 添加商品到此分类
        </el-button>
      </el-collapse-item>
    </el-collapse>

    <!-- 商品弹窗 -->
    <el-dialog
      v-model="showProductDialog"
      :title="isEdit ? '编辑商品' : '添加商品'"
      width="90%"
      top="5vh"
    >
      <el-form ref="prodFormRef" :model="prodForm" label-width="70px" size="large">
        <el-form-item label="名称" required>
          <el-input v-model="prodForm.name" placeholder="商品名称" />
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input v-model.number="prodForm.price" type="number" placeholder="0.00">
            <template #prefix>¥</template>
          </el-input>
        </el-form-item>
        <el-form-item label="库存" required>
          <el-input v-model.number="prodForm.stock" type="number" placeholder="999" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="prodForm.description" type="textarea" :rows="2" placeholder="商品描述（选填）" />
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="prodForm.image" placeholder="图片URL（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProductDialog = false">取消</el-button>
        <el-button type="warning" @click="handleSaveProduct" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分类弹窗 -->
    <el-dialog v-model="showCatDialog" title="添加分类" width="80%">
      <el-form size="large">
        <el-form-item label="名称">
          <el-input v-model="catForm.name" placeholder="如：热销推荐" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model.number="catForm.sortOrder" type="number" placeholder="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCatDialog = false">取消</el-button>
        <el-button type="warning" @click="handleAddCat" :loading="catSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantApi } from '@/api/merchant'

const activeCats = ref<number[]>([])
const products = ref<any[]>([])
const categories = ref<any[]>([])
const showProductDialog = ref(false)
const showCatDialog = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const catSaving = ref(false)
const prodFormRef = ref()

const prodForm = reactive({
  categoryId: 0, name: '', price: 0 as number,
  stock: 999, description: '', image: '',
})

const catForm = reactive({ name: '', sortOrder: 0 })

function getCatProductCount(catId: number) {
  return products.value.filter(p => p.category?.id === catId).length
}

function getCatProducts(catId: number) {
  return products.value.filter(p => p.category?.id === catId)
}

async function loadData() {
  const res: any = await merchantApi.getProducts()
  products.value = res.products || []
  categories.value = res.categories || []
  if (categories.value.length > 0 && activeCats.value.length === 0) {
    activeCats.value = [categories.value[0].id]
  }
}

function openAdd(catId: number) {
  isEdit.value = false; editId.value = null
  Object.assign(prodForm, {
    categoryId: catId, name: '', price: 0,
    stock: 999, description: '', image: '',
  })
  showProductDialog.value = true
}

function openEdit(p: any) {
  isEdit.value = true; editId.value = p.id
  Object.assign(prodForm, {
    categoryId: p.category?.id || 0, name: p.name,
    price: p.price, stock: p.stock,
    description: p.description || '', image: p.image || '',
  })
  showProductDialog.value = true
}

async function handleSaveProduct() {
  if (!prodForm.name || !prodForm.price) {
    ElMessage.warning('请填写名称和价格')
    return
  }
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await merchantApi.editProduct(editId.value, prodForm)
    } else {
      await merchantApi.addProduct(prodForm)
    }
    ElMessage.success(isEdit.value ? '商品已更新' : '商品已添加')
    showProductDialog.value = false
    loadData()
  } finally { saving.value = false }
}

async function toggleStatus(id: number) {
  await merchantApi.toggleProduct(id)
  ElMessage.success('状态已切换')
  loadData()
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该商品？', '提示', { type: 'warning' })
    await merchantApi.deleteProduct(id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled */ }
}

function openCatDialog() {
  catForm.name = ''
  catForm.sortOrder = 0
  showCatDialog.value = true
}

async function handleAddCat() {
  if (!catForm.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  catSaving.value = true
  try {
    await merchantApi.addCategory(catForm.name, catForm.sortOrder)
    ElMessage.success('分类已添加')
    showCatDialog.value = false
    loadData()
  } finally { catSaving.value = false }
}

async function handleDeleteCat(catId: number, catName: string) {
  const count = getCatProductCount(catId)
  const msg = count > 0
    ? `分类「${catName}」下有 ${count} 件商品，删除分类将同时删除这些商品，确定继续？`
    : `确定删除分类「${catName}」？`
  try {
    await ElMessageBox.confirm(msg, '删除分类', { type: 'warning', confirmButtonText: '确定删除' })
    await merchantApi.deleteCategory(catId)
    ElMessage.success('分类已删除')
    loadData()
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>

<style scoped>
.product-collapse {
  margin: 10px 12px;
}

.product-collapse :deep(.el-collapse-item__header) {
  font-size: 15px;
  padding: 12px 8px;
}

.pc-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.pc-cat-name {
  font-weight: 600;
  font-size: 15px;
}

.pc-empty-cat {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
}

/* 商品行 */
.pc-product-row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 8px;
  border-bottom: 1px solid #F0F0F0;
  margin: 0 4px;
}

.pcp-avatar {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #FFF8E1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pcp-info {
  flex: 1;
  min-width: 0;
}

.pcp-name {
  font-size: 15px;
  font-weight: 600;
}

.pcp-meta {
  font-size: 12px;
  color: #999;
  margin: 4px 0;
  display: flex;
  gap: 10px;
}

.pcp-price {
  color: #FF6B35;
  font-weight: 700;
  font-size: 14px;
}

.pcp-status {
  margin-top: 2px;
}

.pcp-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pcp-add-btn {
  width: 100%;
  margin-top: 8px;
}

.pc-cat-del {
  opacity: 0.5;
  transition: opacity 0.15s;
}

.pc-cat-del:hover {
  opacity: 1;
}
</style>
