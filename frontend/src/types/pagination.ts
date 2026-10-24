export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export function getTotalElements<T>(page: PageResponse<T> | null): number {
  if (!page) return 0;
  if (page.page?.totalElements !== undefined) return page.page.totalElements;
  if (page.totalElements !== undefined) return page.totalElements;
  return page.content?.length || 0;
}

export function getTotalPages<T>(page: PageResponse<T> | null): number {
  if (!page) return 1;
  if (page.page?.totalPages !== undefined) return Math.max(1, page.page.totalPages);
  if (page.totalPages !== undefined) return Math.max(1, page.totalPages);
  return 1;
}

export function getPageNumber<T>(page: PageResponse<T> | null): number {
  if (!page) return 0;
  if (page.page?.number !== undefined) return page.page.number;
  if (page.number !== undefined) return page.number;
  return 0;
}
