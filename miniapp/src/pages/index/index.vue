<template>
  <view class="home">
    <view class="banner">
      <text class="title">图书馆服务</text>
      <text class="subtitle">查书 · 借阅 · 归还</text>
    </view>

    <view class="grid">
      <view class="cell" @click="go('/pages/book-search/book-search')">
        <text class="cell-icon">🔍</text>
        <text class="cell-text">书籍查询</text>
        <text class="cell-desc">查书名 / ISBN / 位置 / 可借</text>
      </view>
      <view class="cell" @click="go('/pages/my-loans/my-loans')">
        <text class="cell-icon">📚</text>
        <text class="cell-text">我的在借</text>
        <text class="cell-desc">按借书证号查在借书籍</text>
      </view>
      <view class="cell" @click="go('/pages/staff-entry/staff-entry')">
        <text class="cell-icon">📷</text>
        <text class="cell-text">拍照录入</text>
        <text class="cell-desc">工作人员拍照识别录入</text>
      </view>
      <view class="cell" @click="go('/pages/shelving/shelving')">
        <text class="cell-icon">🗄️</text>
        <text class="cell-text">上架办理</text>
        <text class="cell-desc">待上架图书位置拍照上架</text>
      </view>
    </view>
  </view>
</template>

<script setup>
function go(url) {
  uni.switchTab({ url })
}
</script>

<style scoped>
.home {
  padding: 24rpx;
}
.banner {
  background: linear-gradient(135deg, #409eff, #79bbff);
  border-radius: 16rpx;
  padding: 48rpx 32rpx;
  margin-bottom: 24rpx;
}
.title {
  display: block;
  color: #fff;
  font-size: 44rpx;
  font-weight: 600;
}
.subtitle {
  display: block;
  color: rgba(255, 255, 255, 0.85);
  font-size: 26rpx;
  margin-top: 8rpx;
}
.grid {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.cell {
  background: #fff;
  border-radius: 16rpx;
  padding: 36rpx 32rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.cell-icon {
  font-size: 48rpx;
}
.cell-text {
  font-size: 32rpx;
  font-weight: 600;
}
.cell-desc {
  font-size: 24rpx;
  color: #909399;
}
</style>
