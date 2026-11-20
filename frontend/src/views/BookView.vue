<template>
  <div>
    <el-card class="mb">
      <el-form :inline="true">
        <el-form-item label="ISBN">
          <el-input v-model="query.isbn" clearable placeholder="精确匹配" style="width: 160px" />
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="query.title" clearable placeholder="模糊匹配" style="width: 160px" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="query.author" clearable placeholder="模糊匹配" style="width: 140px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="query.categoryCode"
            :data="categoryTree"
            node-key="code"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            default-expand-all
            clearable
            placeholder="全部分类"
            style="width: 180px"
          />
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
          <span>图书列表</span>
          <el-button type="success" @click="openCreate">新增图书</el-button>
        </div>
      </template>
      <el-table :data="books" v-loading="loading">
        <el-table-column prop="isbn" label="ISBN" width="150" />
        <el-table-column prop="title" label="书名" min-width="180" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="totalCopies" label="馆藏总量" width="90" />
        <el-table-column prop="availableCopies" label="可借" width="70" />
        <el-table-column prop="pendingShelving" label="待上架" width="80" />
        <el-table-column label="位置" width="110">
          <template #default="{ row }">{{ row.location || '待上架' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="ISBN" required>
          <el-input v-model="form.isbn" :disabled="dupMode" clearable placeholder="录入后自动查重" @blur="checkIsbn" />
        </el-form-item>
        <el-form-item label="书名" required>
          <el-input v-model="form.title" :disabled="dupMode" clearable />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" :disabled="dupMode" clearable />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-tree-select
            v-model="form.categoryCode"
            :data="categoryTree"
            node-key="code"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            default-expand-all
            :disabled="dupMode"
            clearable
            placeholder="请选择分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="!editingId" :label="dupMode ? '本次新增副本数' : '馆藏库存数量'" required>
          <el-input-number v-model="form.totalCopies" :min="1" />
        </el-form-item>
        <el-alert
          v-if="dupMode"
          type="warning"
          :closable="false"
          :title="`馆藏已存在该 ISBN 图书：《${dupBook?.title ?? ''}》，提交将读取原有图书信息并做数量叠加`"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'
import type { BookVO, DictVO, PageResult } from '../types'

/** 分类树（检索筛选与新增/编辑弹窗共用，category 的直接子树） */
const categoryTree = ref<DictVO[]>([])
const books = ref<BookVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const query = reactive({ isbn: '', title: '', author: '', categoryCode: '' })

const dialogVisible = ref(false)
const editingId = ref('')
const dupMode = ref(false)
const dupBook = ref<BookVO | null>(null)
const form = reactive({ isbn: '', title: '', author: '', categoryCode: '', totalCopies: 1 })

const dialogTitle = computed(() =>
  dupMode.value ? '重复 ISBN - 数量叠加确认' : editingId.value ? '编辑图书' : '新增图书'
)

/** 分类选择取自系统字典：category 子树以树形展示，检索筛选与新增/编辑弹窗共用 */
async function loadCategories() {
  const { data } = await http.get<DictVO[]>('/dicts')
  const root = data.find((d) => d.code === 'category')
  categoryTree.value = root?.children ?? []
}

async function search() {
  loading.value = true
  try {
    const { data } = await http.get<PageResult<BookVO>>('/books', {
      params: { ...query, page: page.value, size: size.value }
    })
    books.value = data.data
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(query, { isbn: '', title: '', author: '', categoryCode: '' })
  page.value = 1
  search()
}

function onPageChange(p: number) {
  page.value = p
  search()
}

function openCreate() {
  editingId.value = ''
  dupMode.value = false
  dupBook.value = null
  Object.assign(form, { isbn: '', title: '', author: '', categoryCode: '', totalCopies: 1 })
  dialogVisible.value = true
}

function openEdit(row: BookVO) {
  editingId.value = row.id
  dupMode.value = false
  dupBook.value = null
  Object.assign(form, {
    isbn: row.isbn,
    title: row.title,
    author: row.author ?? '',
    categoryCode: row.categoryCode,
    totalCopies: row.totalCopies
  })
  dialogVisible.value = true
}

/** 录入 ISBN 查重：馆藏已有同 ISBN 图书时提示，确认后回填原信息做数量叠加 */
async function checkIsbn() {
  if (editingId.value || dupMode.value || !form.isbn.trim()) return
  try {
    const { data } = await http.get<BookVO>(`/books/isbn/${encodeURIComponent(form.isbn.trim())}`)
    if (!data) return
    try {
      await ElMessageBox.confirm(
        `馆藏已存在该 ISBN 图书：《${data.title}》。是否继续？继续将读取原有图书信息并做数量叠加。`,
        '重复 ISBN 提示',
        { confirmButtonText: '继续叠加', cancelButtonText: '录入错误', type: 'warning' }
      )
      dupMode.value = true
      dupBook.value = data
      form.title = data.title
      form.author = data.author ?? ''
      form.categoryCode = data.categoryCode
    } catch {
      // 用户选择录入错误，保持表单供手动修改
    }
  } catch {
    // 请求失败已由拦截器统一提示
  }
}

async function submit() {
  if (dupMode.value && dupBook.value) {
    await http.post(`/books/${dupBook.value.id}/add-copies`, { copies: form.totalCopies })
    ElMessage.success('数量叠加成功，新增副本已进入待上架清单')
  } else if (editingId.value) {
    await http.put(`/books/${editingId.value}`, {
      isbn: form.isbn.trim(),
      title: form.title,
      author: form.author,
      categoryCode: form.categoryCode
    })
    ElMessage.success('修改成功')
  } else {
    await http.post('/books', {
      isbn: form.isbn.trim(),
      title: form.title,
      author: form.author,
      categoryCode: form.categoryCode,
      totalCopies: form.totalCopies
    })
    ElMessage.success('录入成功，图书已进入待上架清单，请到上架办理确认上架')
  }
  dialogVisible.value = false
  search()
}

async function remove(row: BookVO) {
  try {
    await ElMessageBox.confirm(`确定删除图书《${row.title}》吗？`, '删除确认', { type: 'warning' })
    await http.delete(`/books/${row.id}`)
    ElMessage.success('删除成功')
    search()
  } catch {
    // 取消或请求失败
  }
}

onMounted(() => {
  loadCategories()
  search()
})
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
