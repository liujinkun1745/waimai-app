<template>
  <div class="addr-page">
    <div class="header-gradient" style="gap:12px;">
      <el-icon :size="20" @click="$router.back()" style="cursor:pointer;"><ArrowLeft /></el-icon>
      <span style="font-size:18px;font-weight:700;">收货地址</span>
    </div>
    <div class="addr-list">
      <div v-for="a in addresses" :key="a.id" class="card" style="margin:10px 12px;">
        <div class="addr-info">
          <div class="addr-name">{{ a.receiverName }} <span class="addr-phone">{{ a.receiverPhone }}</span></div>
          <div class="addr-detail">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detailAddress }}</div>
          <el-tag v-if="a.isDefault" size="small" type="warning" style="margin-top:4px;">默认</el-tag>
        </div>
        <div class="addr-actions">
          <el-button text size="small" @click="openEdit(a)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(a.id)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!addresses.length" description="暂无地址" />
    </div>
    <div style="padding:0 12px;">
      <el-button type="warning" @click="openAdd" style="width:100%;">+ 添加新地址</el-button>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑地址' : '添加地址'" width="90%">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName"><el-input v-model="form.receiverName" /></el-form-item>
        <el-form-item label="电话" prop="receiverPhone"><el-input v-model="form.receiverPhone" /></el-form-item>
        <el-form-item label="省市区">
          <el-row :gutter="8">
            <el-col :span="8"><el-input v-model="form.province" placeholder="省" /></el-col>
            <el-col :span="8"><el-input v-model="form.city" placeholder="市" /></el-col>
            <el-col :span="8"><el-input v-model="form.district" placeholder="区" /></el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="默认地址"><el-switch v-model="form.isDefault" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { consumerApi } from '@/api/consumer'

const addresses = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  receiverName: '', receiverPhone: '', province: '', city: '',
  district: '', detailAddress: '', isDefault: false,
})
const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

async function load() { addresses.value = await consumerApi.getAddresses() || [] }
function openAdd() {
  isEdit.value = false; editId.value = null
  Object.assign(form, { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false })
  dialogVisible.value = true
}
function openEdit(a: any) {
  isEdit.value = true; editId.value = a.id
  Object.assign(form, a)
  dialogVisible.value = true
}
async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value && editId.value) await consumerApi.editAddress(editId.value, form)
    else await consumerApi.addAddress(form)
    ElMessage.success(isEdit.value ? '已更新' : '已添加')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}
async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
    await consumerApi.deleteAddress(id)
    ElMessage.success('已删除')
    load()
  } catch { /* */ }
}
onMounted(load)
</script>

<style scoped>
.addr-list { padding-top: 4px; }
.addr-name { font-weight: 700; font-size: 15px; }
.addr-phone { font-weight: 400; color: #666; margin-left: 8px; }
.addr-detail { font-size: 13px; color: #666; margin: 4px 0; }
.addr-actions { display: flex; justify-content: flex-end; gap: 4px; margin-top: 4px; }
</style>
