<template>
  <view class="page">
    <view class="card">
      <view class="card-title">1. 拍摄书籍封面</view>
      <view class="photo-area" @click="choosePhoto">
        <image v-if="photoPath" :src="photoPath" class="photo" mode="aspectFit" />
        <text v-else class="photo-placeholder">点击拍照或选择图片</text>
      </view>
      <button class="btn-primary" :disabled="recognizing" @click="recognize">
        {{ recognizing ? '识别中...' : '开始识别' }}
      </button>
    </view>

    <view class="card">
      <view class="card-title">2. 确认 / 修正书籍信息</view>
      <view class="form-item">
        <text class="form-label">书名 *</text>
        <input v-model="form.title" class="form-input" placeholder="书名" />
      </view>
      <view class="form-item">
        <text class="form-label">作者</text>
        <input v-model="form.author" class="form-input" placeholder="作者" />
      </view>
      <view class="form-item">
        <text class="form-label">出版社</text>
        <input v-model="form.publisher" class="form-input" placeholder="出版社" />
      </view>
      <view class="form-item">
        <text class="form-label">年份</text>
        <input v-model="form.publishYear" type="number" class="form-input" placeholder="出版年份" />
      </view>
      <view class="form-item">
        <text class="form-label">ISBN *</text>
        <input
          v-model="form.isbn"
          class="form-input"
          placeholder="ISBN，留空将自动生成"
          @blur="checkIsbn"
        />
        <button class="btn-gen" size="mini" @click="scanIsbn">扫码</button>
        <button class="btn-gen" size="mini" @click="generateIsbn">生成</button>
      </view>
      <view v-if="existingBook" class="dup-tip">
        馆内已有《{{ existingBook.title }}》，提交将累加副本（现有 {{ existingBook.totalCopies }} 册）
      </view>
      <view class="form-item">
        <text class="form-label">分类 *</text>
        <picker
          mode="multiSelector"
          :range="categoryColumns"
          @columnchange="onCategoryColumnChange"
          @change="onCategoryChange"
        >
          <view class="form-input picker">
            {{ form.subCategoryName ? `${form.categoryName} / ${form.subCategoryName}` : form.categoryName || '请选择分类' }}
          </view>
        </picker>
      </view>
      <view class="form-item">
        <text class="form-label">册数 *</text>
        <input v-model="form.copies" type="number" class="form-input" placeholder="馆藏册数" />
      </view>
      <button class="btn-primary" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交录入' }}
      </button>
      <view class="hint">录入成功后图书进入待上架清单，需在 PC 端上架确认并通过审核后方可借阅</view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../utils/api'

const photoPath = ref('')
const recognizing = ref(false)
const submitting = ref(false)
/** ISBN 查重结果：非空表示馆内已有该书，提交将累加副本 */
const existingBook = ref(null)
/** 分类层级：一级分类（文学/历史…）→ 子分类（如 历史 → 中国历史），多列选择器联动展示 */
const topCategories = ref([])
/** 选择器两列当前索引：一级分类 / 子分类 */
const categoryPick = ref([0, 0])

/** 多列选择器列数据：一级分类 / 子分类（无子分类时第二列为占位） */
const categoryColumns = computed(() => {
  const top = topCategories.value[categoryPick.value[0]]
  const subs = top?.children ?? []
  return [
    topCategories.value.length ? topCategories.value.map((c) => c.name) : ['—'],
    subs.length ? subs.map((s) => s.name) : ['—']
  ]
})
const form = reactive({
  title: '',
  author: '',
  publisher: '',
  publishYear: '',
  isbn: '',
  categoryCode: '',
  categoryName: '',
  subCategoryName: '',
  copies: '1'
})

onMounted(async () => {
  try {
    // 拉取分类字典：一级为 category 的直接子节点，二级为其 children
    const res = await api.getDicts()
    const root = res.find((d) => d.code === 'category')
    topCategories.value = root?.children ?? []
  } catch {
    // 错误已由封装提示
  }
})

function choosePhoto() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (res) => {
      photoPath.value = res.tempFilePaths[0]
    },
    fail: (err) => {
      const msg = (err && err.errMsg) || ''
      if (!msg.includes('cancel')) {
        uni.showToast({ title: '无法获取照片，请检查相机权限', icon: 'none' })
      }
    }
  })
}

/** 上传照片调用识别接口，结果回填表单供确认修正；识别无 ISBN 时自动生成 */
async function recognize() {
  if (!photoPath.value) {
    uni.showToast({ title: '请先拍照或选择图片', icon: 'none' })
    return
  }
  recognizing.value = true
  try {
    const info = await api.recognizeBook(photoPath.value)
    form.title = info.title || ''
    form.author = info.author || ''
    form.publisher = info.publisher || ''
    form.publishYear = info.publishYear != null ? String(info.publishYear) : ''
    form.isbn = info.isbn || ''
    if (!form.isbn) {
      // 识别不到 ISBN：按馆内规则自动生成（仍可修改）
      await generateIsbn()
    } else {
      await checkIsbn()
    }
    uni.showToast({ title: '识别完成，请核对信息', icon: 'success' })
  } catch {
    // 错误已由封装提示
  } finally {
    recognizing.value = false
  }
}

/** 扫码识别 ISBN 条码（封底 EAN-13），结果填入并查重 */
function scanIsbn() {
  uni.scanCode({
    scanType: ['barCode'],
    success: async (res) => {
      const code = (res.result || '').trim()
      if (!code) {
        uni.showToast({ title: '未识别到条码内容', icon: 'none' })
        return
      }
      form.isbn = code
      await checkIsbn()
      uni.showToast({ title: '已填入 ISBN，请核对', icon: 'success' })
    },
    fail: (err) => {
      const msg = (err && err.errMsg) || ''
      if (!msg.includes('cancel')) {
        uni.showToast({ title: '扫码失败，请重试或手动输入', icon: 'none' })
      }
    }
  })
}

/**
 * 生成 ISBN：书名 + 出版社 + 作者 + 年份四要素齐全时先查重，
 * 馆内已有同一本书则复用其 ISBN（提交将累加副本）；否则按馆内规则生成新号（可修改后提交）。
 */
async function generateIsbn() {
  const title = form.title.trim()
  const publisher = form.publisher.trim()
  const author = form.author.trim()
  const year = Number(form.publishYear)
  if (title && publisher && author && year) {
    try {
      const same = await api.matchBook({ title, publisher, author, year })
      if (same) {
        form.isbn = same.isbn
        existingBook.value = same
        uni.showToast({ title: `馆内已有同一本书《${same.title}》，已复用现有 ISBN`, icon: 'none', duration: 2500 })
        return
      }
    } catch {
      // 查重失败不阻断，继续按规则生成
    }
  }
  const { isbn } = await api.nextIsbn()
  form.isbn = isbn
  uni.showToast({ title: '已生成馆内 ISBN，可修改', icon: 'none' })
  await checkIsbn()
}

/** ISBN 查重：已存在则提示提交将累加副本（用现有馆藏） */
async function checkIsbn() {
  existingBook.value = null
  const isbn = form.isbn.trim()
  if (!isbn) return
  try {
    const book = await api.bookByIsbn(isbn)
    if (book) {
      existingBook.value = book
    }
  } catch {
    // 查重失败不阻断录入，提交时后端仍会兜底处理
  }
}

function onCategoryColumnChange(e) {
  const col = Number(e.detail.column)
  const val = Number(e.detail.value)
  categoryPick.value[col] = val
  // 一级分类变化时重置子分类列索引，联动刷新子分类列数据
  if (col === 0) {
    categoryPick.value[1] = 0
  }
}

function onCategoryChange(e) {
  const [ti, si] = (e.detail.value ?? []).map(Number)
  categoryPick.value = [ti, si]
  const top = topCategories.value[ti]
  const subs = top?.children ?? []
  const sub = subs[si]
  if (sub) {
    // 选中子分类：取子分类编码（如 历史 → 中国历史）
    form.categoryCode = sub.code
    form.categoryName = sub.name
    form.subCategoryName = sub.name
  } else {
    // 无子分类：取一级分类编码
    form.categoryCode = top.code
    form.categoryName = top.name
    form.subCategoryName = ''
  }
}

async function submit() {
  if (!form.title.trim()) {
    uni.showToast({ title: '请填写书名', icon: 'none' })
    return
  }
  if (!form.isbn.trim()) {
    // ISBN 留空：按规则自动生成后提交（后端同样兜底）
    try {
      await generateIsbn()
    } catch {
      return
    }
  }
  if (!form.categoryCode) {
    uni.showToast({ title: '请选择分类', icon: 'none' })
    return
  }
  const copies = Number(form.copies)
  if (!copies || copies < 1) {
    uni.showToast({ title: '册数至少为 1', icon: 'none' })
    return
  }
  const existed = existingBook.value != null
  submitting.value = true
  try {
    await api.createBook({
      isbn: form.isbn.trim(),
      title: form.title.trim(),
      author: form.author.trim(),
      publisher: form.publisher.trim(),
      publishYear: form.publishYear ? Number(form.publishYear) : null,
      categoryCode: form.categoryCode,
      totalCopies: copies
    })
    uni.showToast({ title: existed ? '已累加副本，进入待上架清单' : '录入成功，进入待上架清单', icon: 'success' })
    reset()
  } catch {
    // 错误已由封装提示
  } finally {
    submitting.value = false
  }
}

function reset() {
  photoPath.value = ''
  existingBook.value = null
  Object.assign(form, { title: '', author: '', publisher: '', publishYear: '', isbn: '', categoryCode: '', categoryName: '', subCategoryName: '', copies: '1' })
  categoryPick.value = [0, 0]
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}
.photo-area {
  height: 360rpx;
  background: var(--bg-input);
  border: 2rpx dashed var(--border-strong);
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
  overflow: hidden;
}
.photo {
  width: 100%;
  height: 100%;
}
.photo-placeholder {
  color: var(--text-caption);
  font-size: 26rpx;
}
.btn-primary {
  background: var(--brand-600);
  color: #fff;
  border: none;
  font-size: 28rpx;
}
.form-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
}
.form-label {
  width: 100rpx;
  color: var(--text-secondary);
  font-size: 26rpx;
  flex-shrink: 0;
}
.form-input {
  flex: 1;
  background: var(--bg-input);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
}
.picker {
  color: var(--text-primary);
}
.btn-gen {
  background: var(--bg-input);
  color: var(--brand-600);
  border: 1rpx solid var(--border-strong);
  flex-shrink: 0;
}
.dup-tip {
  margin: -8rpx 0 20rpx 116rpx;
  font-size: 22rpx;
  color: var(--warning);
  background: var(--warning-bg);
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  line-height: 32rpx;
}
.hint {
  margin-top: 16rpx;
  font-size: 22rpx;
  color: var(--text-disabled);
  line-height: 36rpx;
}
</style>
