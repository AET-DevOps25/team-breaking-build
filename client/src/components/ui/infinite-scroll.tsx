import { useEffect, useRef, useCallback } from 'react';

interface InfiniteScrollProps {
  onLoadMore: () => void;
  hasMore: boolean;
  loading: boolean;
  children: React.ReactNode;
  threshold?: number;
  rootMargin?: string;
}

export function InfiniteScroll({
  onLoadMore,
  hasMore,
  loading,
  children,
  threshold = 0.1,
  rootMargin = '100px',
}: InfiniteScrollProps) {
  const observerTarget = useRef<HTMLDivElement>(null);

  const handleIntersection = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const [entry] = entries;
      if (entry.isIntersecting && hasMore && !loading) {
        onLoadMore();
      }
    },
    [hasMore, loading, onLoadMore],
  );

  useEffect(() => {
    const observer = new IntersectionObserver(handleIntersection, {
      threshold,
      rootMargin,
    });

    const currentTarget = observerTarget.current;
    if (currentTarget) {
      observer.observe(currentTarget);
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget);
      }
    };
  }, [handleIntersection, threshold, rootMargin]);

  return (
    <>
      {children}
      {/* Loading indicator and intersection observer target */}
      <div
        ref={observerTarget}
        className='flex h-10 items-center justify-center'
      >
        {loading && <div className='size-8 animate-spin rounded-full border-b-2 border-[#FF7C75]'></div>}
      </div>
    </>
  );
}
