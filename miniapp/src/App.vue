<script>
export default {
  onLaunch: function () {
    console.log('App Launch')
  },
  onShow: function () {
    console.log('App Show')
  },
  onHide: function () {
    console.log('App Hide')
  }
}
</script>

<style>
/* 全局基础样式 + 设计 token（与 PC 端 tokens.css 同源，一处定义处处引用） */
page {
  --brand-500: #2e8b68;
  --brand-600: #1f7352;
  --brand-700: #175c42;
  --success: #1f7a5a;
  --success-bg: #e7f5f0;
  --warning: #a85a12;
  --warning-bg: #fff4e3;
  --danger: #c7362f;
  --danger-bg: #fcedea;
  --info: #5b6b63;
  --info-bg: #edf1ef;
  --text-primary: #1e2b26;
  --text-secondary: #47584f;
  --text-caption: #7c8d85;
  --text-disabled: #b4c1ba;
  --bg-page: #f3f6f4;
  --bg-input: #f3f6f4;
  --border-strong: #c6d5cc;
  background-color: var(--bg-page);
  color: var(--text-primary);
  font-size: 28rpx;
}
</style>
