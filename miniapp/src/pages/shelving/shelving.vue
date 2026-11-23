<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中...</view>

    <view v-if="!loading && books.length === 0" class="tip">暂无待上架图书</view>

    <view class="book-list">
      <view v-for="book in books" :key="book.id" class="book-card">
        <view class="book-info">
          <text class="book-title">{{ book.title }}</text>
          <text class="book-sub">ISBN {{ book.isbn }} · 待上架 {{ book.pendingShelving }} 册</text>
        </view>
        <button class="btn-ok" size="mini" @click="openForm(book)">上架确认</button>
      </view>
    </view>

    <!-- 上架确认表单 -->
    <view v-if="current" class="form-card">
      <view class="form-title">上架确认</view>
      <view class="form-book">《{{ current.title }}》（{{ current.isbn }}）</view>

      <view class="form-item">
        <text class="form-label">书架位置 *</text>
        <picker
          mode="multiSelector"
          :range="locationColumns"
          @columnchange="onLocationColumnChange"
          @change="onLocationChange"
        >
          <view class="form-input picker-box">
            {{ form.areaName ? `${form.areaName} ${form.rowName} ${form.cellName}` : '点击选择书架位置（区 / 排 / 号）' }}
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">现场照片 *</text>
        <view class="photo-area" @click="choosePhoto">
          <image v-if="photoPath" :src="photoPath" class="photo" mode="aspectFill" />
          <text v-else class="photo-placeholder">点击拍照（上架现场）</text>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">操作人 *</text>
        <input v-model="form.operator" class="form-input" placeholder="操作人姓名" />
      </view>

      <view class="form-item">
        <text class="form-label">上架册数</text>
        <input v-model="form.count" type="number" class="form-input" />
        <text class="hint">共待上架 {{ current.pendingShelving }} 册</text>
      </view>

      <view class="btn-row">
        <button class="btn-plain" size="mini" @click="closeForm">取消</button>
        <button class="btn-primary" size="mini" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '确认上架' }}
        </button>
      </view>
      <view class="hint-line">上架确认后进入待审核，审核通过后方可借阅</view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const books = ref([])
const loading = ref(false)
const submitting = ref(false)
const current = ref(null)
/** 本地临时图片路径（拍照后立即预览用） */
const photoPath = ref('')
/** 上传成功后后端返回的相对路径（提交时使用） */
const uploadedPath = ref('')
const form = reactive({
  areaCode: '',
  areaName: '',
  rowCode: '',
  rowName: '',
  cellCode: '',
  cellName: '',
  operator: '潘昕',
  count: '1'
})

/** 位置树：区 → 排 → 号（取自 shelf-location 字典子树），多列选择器联动展示 */
const areas = ref([])
/** 选择器三列当前索引：区 / 排 / 号 */
const pick = ref([0, 0, 0])

/** 多列选择器列数据：区 / 排 / 号，随前级选择联动刷新 */
const locationColumns = computed(() => {
  const area = areas.value[pick.value[0]]
  const rows = area?.children ?? []
  const row = rows[pick.value[1]]
  const cells = row?.children ?? []
  return [
    areas.value.length ? areas.value.map((a) => a.name) : ['—'],
    rows.length ? rows.map((r) => r.name) : ['—'],
    cells.length ? cells.map((c) => c.name) : ['—']
  ]
})

async function load() {
  loading.value = true
  try {
    books.value = await api.pendingShelvings()
  } catch {
    // 错误已由封装提示
  } finally {
    loading.value = false
  }
}

async function loadLocations() {
  try {
    const res = await api.getDicts()
    const root = res.find((d) => d.code === 'shelf-location')
    areas.value = root?.children ?? []
  } catch {
    // 错误已由封装提示
  }
}

function onLocationColumnChange(e) {
  const col = Number(e.detail.column)
  const val = Number(e.detail.value)
  pick.value[col] = val
  // 前级变化时重置后级索引，联动刷新后级列数据
  if (col === 0) {
    pick.value[1] = 0
    pick.value[2] = 0
  } else if (col === 1) {
    pick.value[2] = 0
  }
}

function onLocationChange(e) {
  const [ai, ri, ci] = (e.detail.value ?? []).map(Number)
  pick.value = [ai, ri, ci]
  const area = areas.value[ai]
  const rows = area?.children ?? []
  const row = rows[ri]
  const cells = row?.children ?? []
  const cell = cells[ci]
  form.areaCode = area?.code ?? ''
  form.areaName = area?.name ?? ''
  form.rowCode = row?.code ?? ''
  form.rowName = row?.name ?? ''
  form.cellCode = cell?.code ?? ''
  form.cellName = cell?.name ?? ''
}

function openForm(book) {
  current.value = book
  Object.assign(form, {
    areaCode: '',
    areaName: '',
    rowCode: '',
    rowName: '',
    cellCode: '',
    cellName: '',
    operator: '潘昕',
    count: String(book.pendingShelving ?? 1)
  })
  photoPath.value = ''
  uploadedPath.value = ''
  pick.value = [0, 0, 0]
}

function closeForm() {
  current.value = null
  photoPath.value = ''
  uploadedPath.value = ''
}

/** 拍照/选图后立即本地预览，同时上传；提交时用上传后的服务端路径 */
async function choosePhoto() {
  const res = await new Promise((resolve) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      success: resolve,
      fail: (err) => {
        const msg = (err && err.errMsg) || ''
        if (!msg.includes('cancel')) {
          uni.showToast({ title: '无法获取照片，请检查相机权限', icon: 'none' })
        }
        resolve(null)
      }
    })
  })
  if (!res) return
  const filePath = res.tempFilePaths[0]
  // 先用本地临时路径展示预览
  photoPath.value = filePath
  uploadedPath.value = ''
  uni.showLoading({ title: '上传中' })
  try {
    const { photoPath: path } = await api.uploadPhoto(filePath)
    uploadedPath.value = path
    uni.showToast({ title: '照片上传成功', icon: 'success' })
  } catch {
    // 上传失败（api 封装已提示）：本地预览保留，提交校验会拦截提示重新拍照上传
  } finally {
    uni.hideLoading()
  }
}

async function submit() {
  if (!current.value) return
  if (!form.areaName || !form.rowName || !form.cellName) {
    uni.showToast({ title: '请选择完整的书架位置（区/排/号）', icon: 'none' })
    return
  }
  if (!photoPath.value || !uploadedPath.value) {
    uni.showToast({ title: '请拍摄上架现场照片（需上传成功）', icon: 'none' })
    return
  }
  if (!form.operator.trim()) {
    uni.showToast({ title: '请填写操作人', icon: 'none' })
    return
  }
  const count = Number(form.count)
  if (!count || count < 1) {
    uni.showToast({ title: '上架册数至少为 1', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    // 位置以「区 排 号」完整路径名提交（与 PC 端一致）
    await api.confirmShelving({
      bookId: current.value.id,
      location: `${form.areaName} ${form.rowName} ${form.cellName}`,
      photoPath: uploadedPath.value,
      operator: form.operator.trim(),
      count
    })
    uni.showToast({ title: '上架确认成功，已提交审核', icon: 'success' })
    closeForm()
    await load()
  } catch {
    // 错误已由封装提示
  } finally {
    submitting.value = false
  }
}

onMounted(loadLocations)
onShow(load)
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.tip {
  text-align: center;
  color: var(--text-caption);
  padding: 48rpx 0;
}
.book-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.book-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.book-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  flex: 1;
  margin-right: 16rpx;
}
.book-title {
  font-size: 30rpx;
  font-weight: 600;
}
.book-sub {
  font-size: 22rpx;
  color: var(--text-caption);
}
.btn-ok {
  background: var(--brand-600);
  color: #fff;
  border: none;
}
.form-card {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
}
.form-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}
.form-book {
  font-size: 24rpx;
  color: var(--text-caption);
  margin-bottom: 20rpx;
}
.form-item {
  margin-bottom: 20rpx;
}
.form-label {
  display: block;
  color: var(--text-secondary);
  font-size: 26rpx;
  margin-bottom: 10rpx;
}
.picker-box {
  background: var(--bg-input);
  border-radius: 12rpx;
  padding: 14rpx 20rpx;
  font-size: 26rpx;
  color: var(--text-primary);
}
.photo-area {
  height: 300rpx;
  background: var(--bg-input);
  border: 2rpx dashed var(--border-strong);
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
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
.form-input {
  background: var(--bg-input);
  border-radius: 12rpx;
  padding: 14rpx 20rpx;
  font-size: 28rpx;
}
.hint {
  margin-left: 12rpx;
  font-size: 22rpx;
  color: var(--text-disabled);
}
.btn-row {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
}
.btn-plain {
  background: #fff;
  color: var(--text-secondary);
  border: 1rpx solid var(--border-strong);
}
.btn-primary {
  background: var(--brand-600);
  color: #fff;
  border: none;
}
.hint-line {
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--text-disabled);
}
</style>
