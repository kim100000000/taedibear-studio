import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import Toast from '../components/Toast';
import ConfirmModal from '../components/ConfirmModal';
import { listSchedules, updateSchedule, deleteSchedule } from '../api/scheduled';
import type { ScheduledPost, ToastData } from '../types';

// P-11 콘텐츠 캘린더 (/calendar) — Phase 1-1
// 기존 GET /api/scheduled, PUT /api/scheduled/:id, DELETE /api/scheduled/:id 활용.
// 새 백엔드 API 없음.
export default function CalendarPage() {
  const { t } = useTranslation();

  const [currentDate, setCurrentDate] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [items, setItems] = useState<ScheduledPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedItem, setSelectedItem] = useState<ScheduledPost | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);
  const [draggingId, setDraggingId] = useState<number | null>(null);
  const [draggingTime, setDraggingTime] = useState<string | null>(null);
  const [dragOverDate, setDragOverDate] = useState<string | null>(null);
  const [toast, setToast] = useState<ToastData | null>(null);

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await listSchedules();
      setItems(res.data.data);
    } catch {
      setToast({ type: 'error', message: t('common.loading') });
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    load();
  }, [load]);

  // ── 캘린더 날짜 계산 헬퍼 ────────────────────────────────────────────────
  const toDateStr = (d: Date): string => {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  const isToday = (d: Date) => toDateStr(d) === toDateStr(new Date());

  /** 현재 월의 날짜 배열 (앞쪽은 null로 패딩) */
  const getDays = (): (Date | null)[] => {
    const firstWeekday = new Date(year, month, 1).getDay(); // 0=일
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const days: (Date | null)[] = [];
    for (let i = 0; i < firstWeekday; i++) days.push(null);
    for (let d = 1; d <= daysInMonth; d++) days.push(new Date(year, month, d));
    return days;
  };

  const getItemsForDate = (d: Date) =>
    items.filter((item) => item.scheduled_at.startsWith(toDateStr(d)));

  // ── 월 이동 ──────────────────────────────────────────────────────────────
  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));

  // ── 드래그 & 드롭 ────────────────────────────────────────────────────────
  const handleDragStart = (e: React.DragEvent, item: ScheduledPost) => {
    if (item.status !== 'pending') return;
    setDraggingId(item.id);
    setDraggingTime(item.scheduled_at);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent, dateStr: string) => {
    if (!draggingId) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setDragOverDate(dateStr);
  };

  const handleDrop = async (e: React.DragEvent, targetDate: Date) => {
    e.preventDefault();
    if (!draggingId || !draggingTime) return;
    setDragOverDate(null);

    // 기존 시/분은 유지하고 날짜만 변경
    const original = new Date(draggingTime);
    const newDt = new Date(targetDate);
    newDt.setHours(original.getHours(), original.getMinutes(), 0, 0);

    // 10분 이후 제약
    if (newDt <= new Date(Date.now() + 10 * 60 * 1000)) {
      setToast({ type: 'error', message: t('calendar.toast.rescheduleTooSoon') });
      setDraggingId(null);
      setDraggingTime(null);
      return;
    }

    const scheduleId = draggingId;
    setDraggingId(null);
    setDraggingTime(null);

    try {
      await updateSchedule(scheduleId, { scheduled_at: newDt.toISOString() });
      setToast({ type: 'success', message: t('calendar.toast.rescheduled') });
      await load();
    } catch {
      setToast({ type: 'error', message: t('calendar.toast.rescheduleFailed') });
    }
  };

  const handleDragEnd = () => {
    setDraggingId(null);
    setDraggingTime(null);
    setDragOverDate(null);
  };

  // ── 예약 취소 ─────────────────────────────────────────────────────────────
  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteSchedule(deleteTarget);
      setToast({ type: 'success', message: t('calendar.toast.deleted') });
      setSelectedItem(null);
      await load();
    } catch {
      setToast({ type: 'error', message: t('calendar.toast.deleteFailed') });
    } finally {
      setDeleteTarget(null);
    }
  };

  // ── 포맷 헬퍼 ─────────────────────────────────────────────────────────────
  const formatTime = (iso: string) =>
    new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });

  const formatDateTime = (iso: string) =>
    new Date(iso).toLocaleString(undefined, {
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

  // ScheduleStatus → PostStatus 클래스 매핑 (기존 status-badge CSS 재사용)
  const statusBadgeClass = (s: ScheduledPost['status']) =>
    s === 'pending' ? 'scheduled' : s === 'done' ? 'posted' : 'failed';

  const weekdays = t('calendar.weekdays', { returnObjects: true }) as string[];
  const days = getDays();
  const monthLabel = currentDate.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
  });

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content calendar-page">

        {/* 헤더 */}
        <div className="calendar-header">
          <button
            type="button"
            className="calendar-nav-btn"
            onClick={prevMonth}
            aria-label={t('calendar.prev')}
          >
            ‹
          </button>
          <h1 className="calendar-title">{monthLabel}</h1>
          <button
            type="button"
            className="calendar-nav-btn"
            onClick={nextMonth}
            aria-label={t('calendar.next')}
          >
            ›
          </button>
        </div>

        {/* 범례 */}
        <div className="calendar-legend">
          <span className="legend-item">
            <span className="legend-dot legend-pending" />
            {t('calendar.legend.pending')}
          </span>
          <span className="legend-item">
            <span className="legend-dot legend-done" />
            {t('calendar.legend.done')}
          </span>
          <span className="legend-item">
            <span className="legend-dot legend-failed" />
            {t('calendar.legend.failed')}
          </span>
          <span className="calendar-drag-hint">{t('calendar.dragHint')}</span>
        </div>

        {loading ? (
          <Spinner label={t('common.loading')} />
        ) : (
          <div className="calendar-grid">
            {/* 요일 헤더 */}
            {weekdays.map((wd) => (
              <div key={wd} className="calendar-weekday">
                {wd}
              </div>
            ))}

            {/* 날짜 셀 */}
            {days.map((day, idx) => {
              if (!day) {
                // eslint-disable-next-line react/no-array-index-key
                return <div key={`empty-${idx}`} className="calendar-day calendar-day-empty" />;
              }
              const dateStr = toDateStr(day);
              const dayItems = getItemsForDate(day);
              const isOver = dragOverDate === dateStr;

              return (
                <div
                  key={dateStr}
                  className={[
                    'calendar-day',
                    isToday(day) ? 'calendar-day-today' : '',
                    isOver ? 'calendar-day-drag-over' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  onDragOver={(e) => handleDragOver(e, dateStr)}
                  onDragLeave={() => {
                    if (dragOverDate === dateStr) setDragOverDate(null);
                  }}
                  onDrop={(e) => handleDrop(e, day)}
                >
                  <div className="calendar-day-number">{day.getDate()}</div>
                  <div className="calendar-items">
                    {dayItems.map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        className={`calendar-item calendar-item-${item.status}`}
                        draggable={item.status === 'pending'}
                        onDragStart={(e) => handleDragStart(e, item)}
                        onDragEnd={handleDragEnd}
                        onClick={() => setSelectedItem(item)}
                        title={`${formatTime(item.scheduled_at)} — ${item.post?.caption?.slice(0, 30) ?? ''}`}
                      >
                        {item.post?.image_url && (
                          <img src={item.post.image_url} alt="" className="calendar-item-thumb" />
                        )}
                        <span className="calendar-item-time">{formatTime(item.scheduled_at)}</span>
                      </button>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* 상세 모달 */}
        {selectedItem && (
          <div
            className="calendar-modal-overlay"
            role="presentation"
            onClick={() => setSelectedItem(null)}
          >
            <div
              className="calendar-modal"
              role="dialog"
              aria-modal="true"
              onClick={(e) => e.stopPropagation()}
            >
              {selectedItem.post?.image_url && (
                <img
                  src={selectedItem.post.image_url}
                  alt=""
                  className="calendar-modal-img"
                />
              )}
              <div className="calendar-modal-body">
                <p className="calendar-modal-caption">
                  {selectedItem.post?.caption ?? t('history.noCaption')}
                </p>
                <div className="calendar-modal-meta">
                  <span className={`status-badge status-${statusBadgeClass(selectedItem.status)}`}>
                    {t(`calendar.status.${selectedItem.status}`)}
                  </span>
                  <span className="calendar-modal-time">
                    {formatDateTime(selectedItem.scheduled_at)}
                  </span>
                </div>
                <div className="calendar-modal-actions">
                  {selectedItem.status === 'pending' && (
                    <button
                      type="button"
                      className="btn-danger"
                      onClick={() => setDeleteTarget(selectedItem.id)}
                    >
                      {t('calendar.modal.deleteSchedule')}
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn-outline"
                    onClick={() => setSelectedItem(null)}
                  >
                    {t('common.cancel')}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      <ConfirmModal
        open={!!deleteTarget}
        title={t('calendar.modal.deleteSchedule')}
        message={t('calendar.modal.deleteConfirm')}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
