/**
 * 后端 API 基础配置。
 * 真机预览连接本机热点时使用本机可访问 IP（需手机浏览器验证可达，如 http://10.88.91.102:8080）；
 * 仅用微信开发者工具调试时可改回 http://localhost:8080。
 */
export const BASE_URL = 'http://10.88.91.102:8080'

/**
 * 统一请求封装：返回 Promise<data>，非 2xx 与业务错误抛出带提示的 Error。
 * fail 时把微信底层 errMsg 中的关键原因透出（超时 / 域名校验拦截 / 连接失败），
 * 便于真机联调时一眼定位问题。
 */
export function request(path, { method = 'GET', params, data } = {}) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + path,
      method,
      data: method === 'GET' ? params : data,
      header: {
        'Content-Type': 'application/json'
      },
      timeout: 10000,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
          return
        }
        const message = (res.data && res.data.message) || `请求失败（${res.statusCode}）`
        uni.showToast({ title: message, icon: 'none' })
        reject(new Error(message))
      },
      fail: (err) => {
        const errMsg = (err && err.errMsg) || ''
        console.error('[api] 请求失败:', path, errMsg)
        let title = '网络异常，请检查后端服务'
        if (errMsg.includes('timeout')) {
          title = '请求超时，请检查网络'
        } else if (errMsg.includes('domain')) {
          title = '请求被拦截：请打开调试模式'
        } else if (errMsg.includes('abort')) {
          title = '请求已中断'
        }
        uni.showToast({ title, icon: 'none', duration: 2500 })
        reject(new Error(errMsg || 'network error'))
      }
    })
  })
}

/** 文件上传：POST multipart，返回响应体 data。 */
export function upload(path, filePath, formData = {}) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + path,
      filePath,
      name: 'file',
      formData,
      timeout: 10000,
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(body)
            return
          }
          uni.showToast({ title: body.message || `上传失败（${res.statusCode}）`, icon: 'none' })
          reject(new Error(body.message || 'upload failed'))
        } catch {
          uni.showToast({ title: '上传失败', icon: 'none' })
          reject(new Error('upload failed'))
        }
      },
      fail: (err) => {
        const errMsg = (err && err.errMsg) || ''
        console.error('[api] 上传失败:', path, errMsg)
        let title = '网络异常，请检查后端服务'
        if (errMsg.includes('timeout')) {
          title = '请求超时，请检查网络'
        } else if (errMsg.includes('domain')) {
          title = '请求被拦截：请打开调试模式'
        }
        uni.showToast({ title, icon: 'none', duration: 2500 })
        reject(new Error(errMsg || 'network error'))
      }
    })
  })
}

export const api = {
  /** 字典树（含分类 / 书架位置） */
  getDicts: () => request('/api/dicts'),
  /** 书籍检索：title / isbn 关键字 */
  searchBooks: (params) => request('/api/books', { params }),
  /** 按借书证号查读者 */
  readerByCardNo: (cardNo) => request(`/api/readers/card-no/${encodeURIComponent(cardNo)}`),
  /** 查某读者的当前在借 */
  loansByReader: (readerId) => request(`/api/loans/reader/${readerId}`),
  /** 照片识别书籍信息（multipart） */
  recognizeBook: (filePath) => upload('/api/books/recognize', filePath),
  /** 下一个可用的馆内 ISBN（自动生成规则，返回 { isbn }） */
  nextIsbn: () => request('/api/books/isbn/next'),
  /** 按 ISBN 查已有图书（查重用，无则返回 null） */
  bookByIsbn: (isbn) => request(`/api/books/isbn/${encodeURIComponent(isbn)}`),
  /** 同一本书查重：书名+出版社+作者+年份全部一致才命中，无则返回 null */
  matchBook: (params) => request('/api/books/match', { params }),
  /** 录入馆藏 */
  createBook: (data) => request('/api/books', { method: 'POST', data }),
  /** 待上架清单 */
  pendingShelvings: () => request('/api/shelvings/pending'),
  /** 上传上架现场照片，返回 { photoPath } */
  uploadPhoto: (filePath) => upload('/api/uploads/photo', filePath),
  /** 上架确认（进入待审核） */
  confirmShelving: (data) => request('/api/shelvings/confirm', { method: 'POST', data })
}
