<template>
  <view class="page">
    <view class="search-bar">
      <input
        v-model="cardNo"
        class="search-input"
        placeholder="输入借书证号"
        confirm-type="search"
        @confirm="search"
      />
      <button class="search-btn" size="mini" @click="search">查询</button>
    </view>

    <view v-if="reader" class="reader-card">
      <text class="reader-name">{{ reader.name }}</text>
      <text class="reader-no">借书证号：{{ reader.cardNo }}</text>
    </view>

    <view v-if="loading" class="tip">查询中...</view>

    <view v-if="!loading && searched && loans.length === 0 && reader" class="tip">当前没有在借书籍</view>

    <view class="loan-list">
      <view v-for="loan in loans" :key="loan.id" class="loan-card">
        <view class="loan-title-row">
          <text class="loan-title">{{ loan.bookTitle }}</text>
          <text v-if="loan.overdue" class="tag tag-overdue">已逾期</text>
          <text v-else class="tag tag-ok">在借</text>
        </view>
        <view class="loan-line">
          <text class="label">借出时间</text>
          <text>{{ formatTime(loan.borrowedAt) }}</text>
        </view>
        <view class="loan-line">
          <text class="label">应还时间</text>
          <text :class="{ overdue: loan.overdue }">{{ formatTime(loan.dueAt) }}</text>
        </view>
        <view class="tip-line">请按时归还，逾期可能影响后续借阅</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../../utils/api'

/** 默认当前登录人员：借阅人潘昕 R9003 */
const cardNo = ref('R9003')
const reader = ref(null)
const loans = ref([])
const loading = ref(false)
const searched = ref(false)

async function search() {
  const no = cardNo.value.trim()
  if (!no) {
    uni.showToast({ title: '请输入借书证号', icon: 'none' })
    return
  }
  loading.value = true
  reader.value = null
  loans.value = []
  try {
    // 借书证号 → 读者 → 当前在借（含逾期标记）
    const r = await api.readerByCardNo(no)
    reader.value = r
    loans.value = await api.loansByReader(r.id)
    searched.value = true
  } catch {
    // 错误已由封装提示（如读者不存在）
    searched.value = true
  } finally {
    loading.value = false
  }
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

/** 进入页面自动查询默认借阅人（潘昕）的在借书籍 */
onMounted(search)
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.search-bar {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.search-input {
  flex: 1;
  background: #fff;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
}
.search-btn {
  background: var(--brand-600);
  color: #fff;
  border: none;
}
.reader-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.reader-name {
  font-size: 30rpx;
  font-weight: 600;
}
.reader-no {
  font-size: 24rpx;
  color: var(--text-caption);
}
.tip {
  text-align: center;
  color: var(--text-caption);
  padding: 48rpx 0;
}
.loan-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.loan-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
}
.loan-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.loan-title {
  font-size: 34rpx;
  font-weight: 600;
}
.tag {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
}
.tag-ok {
  background: var(--success-bg);
  color: var(--success);
}
.tag-overdue {
  background: var(--danger-bg);
  color: var(--danger);
}
.loan-line {
  display: flex;
  gap: 24rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
  line-height: 44rpx;
}
.label {
  width: 120rpx;
  color: var(--text-caption);
  flex-shrink: 0;
}
.overdue {
  color: var(--danger);
}
.tip-line {
  margin-top: 16rpx;
  font-size: 22rpx;
  color: var(--text-disabled);
}
</style>
