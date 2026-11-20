// 后端接口类型定义

/** 通用分页响应 */
export interface PageResult<T> {
  total: number
  data: T[]
}

/** 图书 */
export interface BookVO {
  id: string
  isbn: string
  title: string
  author?: string
  categoryCode: string
  categoryName: string
  totalCopies: number
  availableCopies: number
  pendingShelving: number
  location?: string
}

/** 字典项（树形） */
export interface DictVO {
  id: string
  code: string
  name: string
  pcode?: string
  sortOrder: number
  children?: DictVO[]
}

/** 读者 */
export interface ReaderVO {
  id: string
  cardNo: string
  name: string
  phone?: string
  active: boolean
}

/** 借阅记录 */
export interface LoanVO {
  id: string
  bookId: string
  readerId: string
  borrowedAt: string
  dueAt: string
  returnedAt?: string
  overdue: boolean
  bookTitle: string
  bookIsbn: string
  bookAuthor?: string
  readerName: string
  readerCardNo: string
}

/** 上架记录 */
export interface ShelvingRecordVO {
  id: string
  bookId: string
  location: string
  photoPath: string
  shelvedAt: string
  operator: string
  shelvedCount: number
  bookTitle?: string
  bookIsbn?: string
  reviewStatus?: string
  reviewer?: string
  reviewedAt?: string
  rejectReason?: string
}
