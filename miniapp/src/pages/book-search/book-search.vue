<template>
  <view class="page">
    <view class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="输入书名或 ISBN"
        confirm-type="search"
        @confirm="search"
      />
      <button class="search-btn" size="mini" @click="search">查询</button>
    </view>

    <view v-if="loading" class="tip">查询中...</view>

    <view v-if="!loading && searched && books.length === 0" class="tip">未找到相关图书</view>

    <view class="book-list">
      <view v-for="book in books" :key="book.id" class="book-card">
        <view class="book-title-row">
          <text class="book-title">{{ book.title }}</text>
          <text class="tag" :class="book.availableCopies > 0 ? 'tag-ok' : 'tag-off'">
            {{ book.availableCopies > 0 ? '可借 ' + book.availableCopies : '不可借' }}
          </text>
        </view>
        <view class="book-line">
          <text class="label">作者</text>
          <text>{{ book.author || '—' }}</text>
        </view>
        <view class="book-line">
          <text class="label">ISBN</text>
          <text>{{ book.isbn }}</text>
        </view>
        <view class="book-line">
          <text class="label">分类</text>
          <text>{{ book.categoryName }}</text>
        </view>
        <view class="book-line">
          <text class="label">位置</text>
          <text v-if="book.location">{{ book.location }}</text>
          <text v-else class="warn">待上架</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { api } from '../../utils/api'

const keyword = ref('')
const books = ref([])
const loading = ref(false)
const searched = ref(false)

async function search() {
  const kw = keyword.value.trim()
  if (!kw) {
    uni.showToast({ title: '请输入书名或 ISBN', icon: 'none' })
    return
  }
  loading.value = true
  try {
    // 关键字同时匹配书名 / ISBN（后端 OR 命中）
    const res = await api.searchBooks({ keyword: kw, page: 1, size: 50 })
    books.value = res.data || []
    searched.value = true
  } catch {
    // 错误已由封装提示
  } finally {
    loading.value = false
  }
}
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
  padding: 28rpx;
}
.book-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.book-title {
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
.tag-off {
  background: var(--info-bg);
  color: var(--info);
}
.book-line {
  display: flex;
  gap: 24rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
  line-height: 44rpx;
}
.label {
  width: 88rpx;
  color: var(--text-caption);
  flex-shrink: 0;
}
.warn {
  color: var(--warning);
}
</style>
