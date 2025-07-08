import { useState, useCallback, useRef, useEffect } from 'react';

interface UseInfiniteScrollOptions<T> {
  fetchData: (page: number) => Promise<T[]>;
  pageSize?: number;
  initialPage?: number;
}

interface UseInfiniteScrollReturn<T> {
  data: T[];
  loading: boolean;
  hasMore: boolean;
  error: string | null;
  loadMore: () => Promise<void>;
  reset: () => void;
}

export function useInfiniteScroll<T>({
  fetchData,
  pageSize = 10,
  initialPage = 0,
}: UseInfiniteScrollOptions<T>): UseInfiniteScrollReturn<T> {
  const [data, setData] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(initialPage);
  const loadingRef = useRef(false);

  const loadPage = useCallback(
    async (pageToLoad: number) => {
      // Prevent multiple simultaneous requests
      if (loadingRef.current) return;

      loadingRef.current = true;
      setLoading(true);
      setError(null);

      try {
        const newData = await fetchData(pageToLoad);

        if (newData.length === 0) {
          setHasMore(false);
        } else {
          setData((prev) => (pageToLoad === initialPage ? newData : [...prev, ...newData]));
          setPage(pageToLoad + 1);
          setHasMore(newData.length === pageSize);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load data');
        setHasMore(false);
      } finally {
        setLoading(false);
        loadingRef.current = false;
      }
    },
    [fetchData, pageSize, initialPage],
  );

  const loadMore = useCallback(async () => {
    if (!hasMore) return;
    await loadPage(page);
  }, [loadPage, page, hasMore]);

  const reset = useCallback(() => {
    setData([]);
    setLoading(false);
    setHasMore(true);
    setError(null);
    setPage(initialPage);
    loadingRef.current = false;
  }, [initialPage]);

  // Load initial data
  useEffect(() => {
    loadPage(initialPage);
  }, [loadPage, initialPage]);

  return {
    data,
    loading,
    hasMore,
    error,
    loadMore,
    reset,
  };
}
