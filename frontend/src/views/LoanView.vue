<template>
  <div>
    <el-card class="mb">
      <el-form :inline="true">
        <el-form-item label="读者查找">
          <el-input v-model="cardNo" clearable placeholder="借书证号 / 姓名" style="width: 180px" @keyup.enter="findReader" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="findReader">查询读者</el-button>
        </el-form-item>
        <el-form-item v-if="reader" label="读者">
          <el-tag type="success">{{ reader.name }}（在借 {{ activeLoansCount }} 册）</el-tag>
          <el-tag v-if="!reader.active" type="danger" class="ml">已注销</el-tag>
        </el-form-item>
      </el-form>
      <el-table
        v-if="readerCandidates.length"
        :data="readerCandidates"
        size="small"
        class="candidates"
        @row-click="selectReader"
      >
        <el-table-column prop="cardNo" label="借书证号" width="180" />
        <el-table-column prop="name" label="姓名" width="140" />
        <el-table-column prop="phone" label="联系方式" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '有效' : '已注销' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="selectReader(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-divider />
      <el-form :inline="true">
        <el-form-item label="图书查找">
          <el-input v-model="isbn" clearable placeholder="ISBN / 书名" style="width: 180px" @keyup.enter="findBook" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="findBook">查询图书</el-button>
        </el-form-item>
        <el-form-item v-if="book" label="图书">
          <el-tag :type="book.availableCopies > 0 ? 'info' : 'warning'">
            《{{ book.title }}》{{ book.availableCopies > 0 ? `可借 ${book.availableCopies} 册` : '待上架，暂不可借' }}
          </el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :disabled="!reader || !book || book.availableCopies <= 0" @click="borrow">办理借出</el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-if="bookCandidates.length"
        :data="bookCandidates"
        size="small"
        class="candidates"
        @row-click="selectBook"
      >
        <el-table-column prop="title" label="书名" min-width="180" />
        <el-table-column prop="isbn" label="ISBN" width="160" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="availableCopies" label="可借" width="80" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="selectBook(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header>在借记录（按读者查询）</template>
      <el-table :data="loans" v-loading="loading">
        <el-table-column prop="bookTitle" label="书名" min-width="180" />
        <el-table-column prop="bookIsbn" label="ISBN" width="150" />
        <el-table-column label="借出日期" width="170">
          <template #default="{ row }">{{ formatTime(row.borrowedAt) }}</template>
        </el-table-column>
        <el-table-column label="应还日期" width="170">
          <template #default="{ row }">{{ formatTime(row.dueAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.overdue ? 'danger' : 'success'">{{ row.overdue ? '已超期' : '在借' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="returnBook(row)">办理归还</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && (!reader || loans.length === 0)" description="请先查询读者查看在借记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'
import type { BookVO, LoanVO, PageResult, ReaderVO } from '../types'

const cardNo = ref('')
const isbn = ref('')
const reader = ref<ReaderVO | null>(null)
const book = ref<BookVO | null>(null)
const loans = ref<LoanVO[]>([])
const loading = ref(false)

/** 姓名模糊匹配的候选读者 / 书名模糊匹配的候选图书（多条时供点选） */
const readerCandidates = ref<ReaderVO[]>([])
const bookCandidates = ref<BookVO[]>([])

const activeLoansCount = computed(() => loans.value.length)

/** 查询读者：借书证号精确命中优先，未命中按姓名模糊列出候选 */
async function findReader() {
  const kw = cardNo.value.trim()
  if (!kw) return
  readerCandidates.value = []
  const exact = await http.get<ReaderVO>(`/readers/card-no/${encodeURIComponent(kw)}`)
  if (exact.data) {
    await selectReader(exact.data)
    return
  }
  const { data } = await http.get<PageResult<ReaderVO>>('/readers', {
    params: { name: kw, page: 1, size: 10 }
  })
  if (data.data.length === 0) {
    reader.value = null
    loans.value = []
    ElMessage.warning('未找到匹配的读者')
    return
  }
  if (data.data.length === 1) {
    await selectReader(data.data[0])
    return
  }
  readerCandidates.value = data.data
}

/** 选中读者并加载其在借记录 */
async function selectReader(r: ReaderVO) {
  reader.value = r
  readerCandidates.value = []
  book.value = null
  await loadActiveLoans()
}

/** 查询图书：ISBN 精确命中优先，未命中按书名模糊列出候选 */
async function findBook() {
  const kw = isbn.value.trim()
  if (!kw) return
  bookCandidates.value = []
  const exact = await http.get<BookVO>(`/books/isbn/${encodeURIComponent(kw)}`)
  if (exact.data) {
    book.value = exact.data
    return
  }
  const { data } = await http.get<PageResult<BookVO>>('/books', {
    params: { title: kw, page: 1, size: 10 }
  })
  if (data.data.length === 0) {
    book.value = null
    ElMessage.warning('未找到匹配的图书')
    return
  }
  if (data.data.length === 1) {
    book.value = data.data[0]
    return
  }
  bookCandidates.value = data.data
}

/** 选中候选图书 */
function selectBook(b: BookVO) {
  book.value = b
  bookCandidates.value = []
}

/** 加载读者在借记录（含超期标记） */
async function loadActiveLoans() {
  if (!reader.value) return
  loading.value = true
  try {
    const { data } = await http.get<LoanVO[]>(`/loans/reader/${reader.value.id}`)
    loans.value = data
  } finally {
    loading.value = false
  }
}

/** 办理借出 */
async function borrow() {
  if (!reader.value || !book.value) return
  await http.post('/loans', { readerId: reader.value.id, bookId: book.value.id })
  ElMessage.success(`《${book.value.title}》借出成功`)
  book.value = null
  bookCandidates.value = []
  isbn.value = ''
  await loadActiveLoans()
}

/** 办理归还 */
async function returnBook(row: LoanVO) {
  try {
    await ElMessageBox.confirm(`确定归还《${row.bookTitle}》吗？归还后图书进入待上架状态。`, '归还确认', {
      type: 'warning'
    })
    await http.post(`/loans/${row.id}/return`)
    ElMessage.success('归还成功，图书已进入待上架清单')
    await loadActiveLoans()
  } catch {
    // 取消或请求失败
  }
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}
</script>

<style scoped>
.mb {
  margin-bottom: 12px;
}

.ml {
  margin-left: 8px;
}

.candidates {
  margin-bottom: 8px;
}

.candidates :deep(.el-table__row) {
  cursor: pointer;
}
</style>
