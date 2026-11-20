<template>
  <div>
    <el-tabs v-model="activeTab" class="mb">
      <el-tab-pane label="待上架清单" name="pending">
        <el-card>
          <template #header>
            <div class="header-row">
              <span>待上架清单</span>
              <el-button size="small" @click="load">刷新</el-button>
            </div>
          </template>
          <el-table :data="books" v-loading="loading">
            <el-table-column prop="title" label="书名" min-width="180" />
            <el-table-column prop="isbn" label="ISBN" width="160" />
            <el-table-column prop="categoryName" label="分类" width="100" />
            <el-table-column prop="pendingShelving" label="待上架数量" width="110" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openConfirm(row)">上架确认</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && books.length === 0" description="暂无待上架图书" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="待审核" name="reviews">
        <el-card>
          <template #header>
            <div class="header-row">
              <span>待审核清单</span>
              <el-button size="small" @click="loadReviews">刷新</el-button>
            </div>
          </template>
          <el-table :data="reviews" v-loading="reviewsLoading">
            <el-table-column prop="bookTitle" label="书名" min-width="180" />
            <el-table-column prop="bookIsbn" label="ISBN" width="160" />
            <el-table-column prop="location" label="位置" width="150" />
            <el-table-column label="现场照片" width="110">
              <template #default="{ row }">
                <el-image
                  :src="row.photoPath"
                  fit="cover"
                  class="record-photo"
                  :preview-src-list="[row.photoPath]"
                  preview-teleported
                />
              </template>
            </el-table-column>
            <el-table-column prop="shelvedCount" label="本次册数" width="90" />
            <el-table-column prop="operator" label="操作人" width="100" />
            <el-table-column label="上架时间" width="170">
              <template #default="{ row }">{{ formatTime(row.shelvedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="approve(row)">通过</el-button>
                <el-button size="small" type="danger" @click="openReject(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!reviewsLoading && reviews.length === 0" description="暂无待审核记录" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="上架记录" name="records">
        <el-card>
          <el-form :inline="true">
            <el-form-item label="书名">
              <el-input v-model="recordsQuery.title" clearable placeholder="模糊匹配" style="width: 160px" />
            </el-form-item>
            <el-form-item label="操作人">
              <el-input v-model="recordsQuery.operator" clearable placeholder="模糊匹配" style="width: 140px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchRecords">检索</el-button>
              <el-button @click="resetRecords">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="records" v-loading="recordsLoading">
            <el-table-column label="上架时间" width="170">
              <template #default="{ row }">{{ formatTime(row.shelvedAt) }}</template>
            </el-table-column>
            <el-table-column prop="bookTitle" label="书名" min-width="180" />
            <el-table-column prop="bookIsbn" label="ISBN" width="160" />
            <el-table-column prop="location" label="位置" width="150" />
            <el-table-column prop="shelvedCount" label="本次册数" width="90" />
            <el-table-column prop="operator" label="操作人" width="100" />
            <el-table-column label="现场照片" width="110">
              <template #default="{ row }">
                <el-image
                  :src="row.photoPath"
                  fit="cover"
                  class="record-photo"
                  :preview-src-list="[row.photoPath]"
                  preview-teleported
                />
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="mt"
            layout="total, prev, pager, next"
            :total="recordsTotal"
            :page-size="recordsSize"
            :current-page="recordsPage"
            @current-change="onRecordsPageChange"
          />
          <el-empty v-if="!recordsLoading && records.length === 0" description="暂无上架记录" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="上架确认" width="540px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="图书">
          <span>《{{ currentBook?.title }}》（{{ currentBook?.isbn }}）</span>
        </el-form-item>
        <el-form-item label="书架位置" required>
          <el-tree-select
            v-model="form.locationCode"
            :data="locationTree"
            node-key="code"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            default-expand-all
            clearable
            placeholder="请选择书架位置（区 / 排 / 号）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="现场照片" required>
          <div>
            <el-upload
              :show-file-list="false"
              accept="image/jpeg,image/png"
              :http-request="doUpload"
              :disabled="uploading"
            >
              <el-button :loading="uploading">选择并上传照片</el-button>
            </el-upload>
            <el-image
              v-if="form.photoPath"
              :src="form.photoPath"
              fit="cover"
              class="photo-preview"
              :preview-src-list="[form.photoPath]"
            />
          </div>
        </el-form-item>
        <el-form-item label="本次上架数量" required>
          <el-input-number v-model="form.count" :min="1" :max="currentBook?.pendingShelving ?? 1" />
          <span class="hint">共待上架 {{ currentBook?.pendingShelving ?? 0 }} 册</span>
        </el-form-item>
        <el-form-item label="操作人" required>
          <el-input v-model="form.operator" clearable placeholder="操作人姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="uploading" @click="submit">确认上架</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回上架" width="480px">
      <el-form label-width="80px">
        <el-form-item label="图书">
          <span>《{{ rejectRow?.bookTitle }}》</span>
        </el-form-item>
        <el-form-item label="审核人" required>
          <el-input v-model="rejectForm.reviewer" clearable placeholder="审核人姓名" />
        </el-form-item>
        <el-form-item label="驳回理由">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="选填，例如：位置照片不符"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import http from '../api'
import type { BookVO, DictVO, PageResult, ShelvingRecordVO } from '../types'

const books = ref<BookVO[]>([])
const loading = ref(false)

/** 书架位置树（取自「书架位置」字典的多级子树：区 / 排 / 号） */
const locationTree = ref<DictVO[]>([])

const dialogVisible = ref(false)
const uploading = ref(false)
const currentBook = ref<BookVO | null>(null)
const form = reactive({ locationCode: '', photoPath: '', operator: '', count: 1 })

/** 上架记录页签 */
const activeTab = ref('pending')
const records = ref<ShelvingRecordVO[]>([])
const recordsTotal = ref(0)
const recordsPage = ref(1)
const recordsSize = ref(10)
const recordsLoading = ref(false)
const recordsQuery = reactive({ title: '', operator: '' })

/** 待审核页签 */
const reviews = ref<ShelvingRecordVO[]>([])
const reviewsLoading = ref(false)
const rejectVisible = ref(false)
const rejectRow = ref<ShelvingRecordVO | null>(null)
const rejectForm = reactive({ reviewer: '', reason: '' })

async function load() {
  loading.value = true
  try {
    const { data } = await http.get<BookVO[]>('/shelvings/pending')
    books.value = data
  } finally {
    loading.value = false
  }
}

/** 加载书架位置树（shelf-location 的子树，支持任意层级） */
async function loadLocations() {
  const { data } = await http.get<DictVO[]>('/dicts')
  const root = data.find((d) => d.code === 'shelf-location')
  locationTree.value = root?.children ?? []
}

/** 在位置树中查找 code 对应节点，返回「区 排 号」完整路径名（自根到节点 name 拼接） */
function findPathName(code: string, nodes: DictVO[]): string {
  for (const n of nodes) {
    if (n.code === code) return n.name
    const sub = findPathName(code, n.children ?? [])
    if (sub) return `${n.name} ${sub}`
  }
  return ''
}

/** 加载上架记录（按上架时间倒序，可按书名 / 操作人筛选） */
async function loadRecords() {
  recordsLoading.value = true
  try {
    const { data } = await http.get<PageResult<ShelvingRecordVO>>('/shelvings/records', {
      params: { ...recordsQuery, page: recordsPage.value, size: recordsSize.value }
    })
    records.value = data.data
    recordsTotal.value = data.total
  } finally {
    recordsLoading.value = false
  }
}

function searchRecords() {
  recordsPage.value = 1
  loadRecords()
}

function resetRecords() {
  Object.assign(recordsQuery, { title: '', operator: '' })
  recordsPage.value = 1
  loadRecords()
}

function onRecordsPageChange(p: number) {
  recordsPage.value = p
  loadRecords()
}

function openConfirm(book: BookVO) {
  currentBook.value = book
  // 本次上架数量默认带入全部待上架数量，可手动调小分批上架
  Object.assign(form, { locationCode: '', photoPath: '', operator: '', count: book.pendingShelving ?? 1 })
  dialogVisible.value = true
}

/** 照片上传：POST /api/uploads/photo，返回相对路径 */
async function doUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const { data } = await http.post<{ photoPath: string }>('/uploads/photo', fd)
    form.photoPath = data.photoPath
    ElMessage.success('照片上传成功')
  } catch {
    // 失败已由拦截器提示
  } finally {
    uploading.value = false
  }
}

async function submit() {
  if (!currentBook.value) return
  // 树形下拉 value 为字典 code，提交时映射为「区 排 号」完整路径名（上架记录与图书当前位置存完整名称）
  const locationName = findPathName(form.locationCode, locationTree.value)
  await http.post('/shelvings/confirm', {
    bookId: currentBook.value.id,
    location: locationName,
    photoPath: form.photoPath,
    operator: form.operator.trim(),
    count: form.count
  })
  ElMessage.success('上架确认成功，已提交审核')
  dialogVisible.value = false
  await load()
  await loadRecords()
  await loadReviews()
}

/** 加载待审核清单（待审核记录按上架时间倒序） */
async function loadReviews() {
  reviewsLoading.value = true
  try {
    const { data } = await http.get<ShelvingRecordVO[]>('/shelvings/reviews')
    reviews.value = data
  } finally {
    reviewsLoading.value = false
  }
}

/** 审核通过：图书恢复可借并记录当前位置（审核人必填） */
async function approve(row: ShelvingRecordVO) {
  const { value } = await ElMessageBox.prompt('请输入审核人姓名', '审核通过', {
    confirmButtonText: '确认通过',
    cancelButtonText: '取消',
    inputPlaceholder: '审核人姓名'
  })
  await http.post(`/shelvings/reviews/${row.id}/approve`, { reviewer: value.trim() })
  ElMessage.success('审核通过，图书已恢复可借')
  await loadReviews()
  await load()
}

/** 打开驳回弹窗 */
function openReject(row: ShelvingRecordVO) {
  rejectRow.value = row
  Object.assign(rejectForm, { reviewer: '', reason: '' })
  rejectVisible.value = true
}

/** 审核驳回：本次册数回补待上架数量，回到待上架清单 */
async function submitReject() {
  if (!rejectRow.value || !rejectForm.reviewer.trim()) {
    ElMessage.warning('请填写审核人')
    return
  }
  await http.post(`/shelvings/reviews/${rejectRow.value.id}/reject`, {
    reviewer: rejectForm.reviewer.trim(),
    reason: rejectForm.reason.trim() || undefined
  })
  ElMessage.success('已驳回，图书回到待上架清单')
  rejectVisible.value = false
  await loadReviews()
  await load()
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

watch(activeTab, (tab) => {
  if (tab === 'records') loadRecords()
  if (tab === 'reviews') loadReviews()
})

onMounted(() => {
  load()
  loadLocations()
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

.photo-preview {
  margin-top: 8px;
  width: 160px;
  height: 120px;
  border-radius: 4px;
}

.record-photo {
  width: 72px;
  height: 54px;
  border-radius: 4px;
}

.hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
