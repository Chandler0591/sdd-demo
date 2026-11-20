<template>
  <div>
    <el-card class="mb">
      <el-form :inline="true">
        <el-form-item label="借书证号">
          <el-input v-model="query.cardNo" clearable placeholder="精确匹配" style="width: 180px" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.name" clearable placeholder="模糊匹配" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">检索</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="header-row">
          <span>读者列表</span>
          <el-button type="success" @click="openCreate">注册读者</el-button>
        </div>
      </template>
      <el-table :data="readers" v-loading="loading">
        <el-table-column prop="cardNo" label="借书证号" width="180" />
        <el-table-column prop="name" label="姓名" width="140" />
        <el-table-column prop="phone" label="联系方式" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '有效' : '已注销' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.active" size="small" type="danger" @click="deactivate(row)">注销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="mt"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑读者' : '注册读者'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="借书证号" required>
          <el-input v-model="form.cardNo" clearable placeholder="全局唯一" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" clearable />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.phone" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'
import type { PageResult, ReaderVO } from '../types'

const readers = ref<ReaderVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const query = reactive({ cardNo: '', name: '' })

const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive({ cardNo: '', name: '', phone: '' })

async function search() {
  loading.value = true
  try {
    const { data } = await http.get<PageResult<ReaderVO>>('/readers', {
      params: { ...query, page: page.value, size: size.value }
    })
    readers.value = data.data
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(query, { cardNo: '', name: '' })
  page.value = 1
  search()
}

function onPageChange(p: number) {
  page.value = p
  search()
}

function openCreate() {
  editingId.value = ''
  Object.assign(form, { cardNo: '', name: '', phone: '' })
  dialogVisible.value = true
}

function openEdit(row: ReaderVO) {
  editingId.value = row.id
  Object.assign(form, { cardNo: row.cardNo, name: row.name, phone: row.phone ?? '' })
  dialogVisible.value = true
}

async function submit() {
  if (editingId.value) {
    await http.put(`/readers/${editingId.value}`, {
      cardNo: form.cardNo.trim(),
      name: form.name,
      phone: form.phone
    })
    ElMessage.success('修改成功')
  } else {
    await http.post('/readers', { cardNo: form.cardNo.trim(), name: form.name, phone: form.phone })
    ElMessage.success('注册成功')
  }
  dialogVisible.value = false
  search()
}

async function deactivate(row: ReaderVO) {
  try {
    await ElMessageBox.confirm(
      `确定注销读者「${row.name}」（${row.cardNo}）吗？注销后无法再办理借阅。`,
      '注销确认',
      { type: 'warning' }
    )
    await http.post(`/readers/${row.id}/deactivate`)
    ElMessage.success('已注销')
    search()
  } catch {
    // 取消或请求失败
  }
}

onMounted(search)
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mb {
  margin-bottom: 12px;
}

.mt {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
