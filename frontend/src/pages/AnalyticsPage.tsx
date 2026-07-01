import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import { getAnalyticsSummary } from '../api/analytics';
import type { AnalyticsSummary } from '../api/analytics';
import type { ToastData } from '../types';
import Toast from '../components/Toast';

// P-13 분석 대시보드 (/analytics) — Phase 1-3
// 자체 DB 데이터 기반 (Meta API 없음)

const PIE_COLORS_LIGHT = ['#2e9e44', '#d33'];  // 성공, 실패
const PIE_COLORS_DARK  = ['#4cc26a', '#ff6b6b'];

function isDark() {
  return document.documentElement.getAttribute('data-theme') === 'dark';
}

export default function AnalyticsPage() {
  const { t } = useTranslation();
  const [data, setData] = useState<AnalyticsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<ToastData | null>(null);

  useEffect(() => {
    getAnalyticsSummary()
      .then((res) => setData(res.data.data))
      .catch((err: any) => {
        const msg = err.isNetworkError
          ? t('error.network')
          : err.response?.data?.error || t('error.loadFailed');
        setToast({ type: 'error', message: msg });
      })
      .finally(() => setLoading(false));
  }, [t]);

  const pieColors = isDark() ? PIE_COLORS_DARK : PIE_COLORS_LIGHT;

  // 성공률 파이 데이터
  const successPie = data
    ? [
        { name: t('analytics.posted'), value: data.success_rate },
        { name: t('analytics.failed'), value: Math.max(0, 100 - data.success_rate) },
      ]
    : [];

  // 예약 비율 파이 데이터
  const schedulePie = data
    ? [
        { name: t('analytics.scheduled'), value: data.scheduled_ratio },
        { name: t('analytics.immediate'), value: Math.max(0, 100 - data.scheduled_ratio) },
      ]
    : [];

  const schedulePieColors = isDark()
    ? ['#93c5fd', '#fca5a5']
    : ['#1d4ed8', '#991b1b'];

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content analytics-page">
        <h1>{t('analytics.title')}</h1>

        {loading ? (
          <Spinner label={t('common.loading')} />
        ) : !data ? (
          <EmptyState emoji="📊" message={t('analytics.noData')} />
        ) : (
          <div className="analytics-grid">

            {/* ── 이번 달 사용량 요약 카드 ── */}
            <section className="analytics-section analytics-section-full">
              <h2 className="analytics-section-title">{t('analytics.thisMonth')}</h2>
              <div className="analytics-stat-cards">
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">{data.this_month_used}</span>
                  <span className="analytics-stat-label">{t('analytics.uploads')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">{data.success_rate}%</span>
                  <span className="analytics-stat-label">{t('analytics.successRate')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">{data.scheduled_ratio}%</span>
                  <span className="analytics-stat-label">{t('analytics.scheduledRatio')}</span>
                </div>
              </div>
            </section>

            {/* ── 월별 업로드 추이 ── */}
            <section className="analytics-section analytics-section-full">
              <h2 className="analytics-section-title">{t('analytics.monthlyTitle')}</h2>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart
                  data={data.monthly_uploads}
                  margin={{ top: 4, right: 16, left: -10, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                  <XAxis
                    dataKey="month"
                    tick={{ fontSize: 11, fill: 'var(--text-secondary)' }}
                    tickFormatter={(v: string) => v.slice(5)}  // "YYYY-MM" → "MM"
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: 'var(--text-secondary)' }}
                    allowDecimals={false}
                  />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 8,
                      color: 'var(--text-primary)',
                      fontSize: 13,
                    }}
                    formatter={(v) => [v, t('analytics.uploads')]}
                  />
                  <Bar dataKey="count" fill="var(--color-primary)" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </section>

            {/* ── 요일별 업로드 패턴 ── */}
            <section className="analytics-section analytics-section-half">
              <h2 className="analytics-section-title">{t('analytics.dailyTitle')}</h2>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart
                  data={data.daily_pattern}
                  margin={{ top: 4, right: 8, left: -16, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                  <XAxis
                    dataKey="day"
                    tick={{ fontSize: 11, fill: 'var(--text-secondary)' }}
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: 'var(--text-secondary)' }}
                    allowDecimals={false}
                  />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 8,
                      color: 'var(--text-primary)',
                      fontSize: 13,
                    }}
                    formatter={(v) => [v, t('analytics.uploads')]}
                  />
                  <Bar dataKey="count" fill="#a78bfa" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </section>

            {/* ── 업로드 성공률 ── */}
            <section className="analytics-section analytics-section-quarter">
              <h2 className="analytics-section-title">{t('analytics.successTitle')}</h2>
              {data.success_rate === 0 && successPie[1]?.value === 0 ? (
                <p className="muted analytics-no-data">{t('analytics.noData')}</p>
              ) : (
                <ResponsiveContainer width="100%" height={200}>
                  <PieChart>
                    <Pie
                      data={successPie}
                      cx="50%"
                      cy="50%"
                      innerRadius={50}
                      outerRadius={75}
                      paddingAngle={3}
                      dataKey="value"
                    >
                      {successPie.map((_, i) => (
                        // eslint-disable-next-line react/no-array-index-key
                        <Cell key={i} fill={pieColors[i]} />
                      ))}
                    </Pie>
                    <Legend
                      iconType="circle"
                      iconSize={10}
                      wrapperStyle={{ fontSize: 12, color: 'var(--text-secondary)' }}
                    />
                    <Tooltip
                      contentStyle={{
                        background: 'var(--bg-card)',
                        border: '1px solid var(--border-color)',
                        borderRadius: 8,
                        fontSize: 13,
                      }}
                      formatter={(v) => [`${v}%`]}
                    />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </section>

            {/* ── 예약 vs 즉시 업로드 비율 ── */}
            <section className="analytics-section analytics-section-quarter">
              <h2 className="analytics-section-title">{t('analytics.scheduleTitle')}</h2>
              {data.scheduled_ratio === 0 ? (
                <p className="muted analytics-no-data">{t('analytics.noData')}</p>
              ) : (
                <ResponsiveContainer width="100%" height={200}>
                  <PieChart>
                    <Pie
                      data={schedulePie}
                      cx="50%"
                      cy="50%"
                      innerRadius={50}
                      outerRadius={75}
                      paddingAngle={3}
                      dataKey="value"
                    >
                      {schedulePie.map((_, i) => (
                        // eslint-disable-next-line react/no-array-index-key
                        <Cell key={i} fill={schedulePieColors[i]} />
                      ))}
                    </Pie>
                    <Legend
                      iconType="circle"
                      iconSize={10}
                      wrapperStyle={{ fontSize: 12, color: 'var(--text-secondary)' }}
                    />
                    <Tooltip
                      contentStyle={{
                        background: 'var(--bg-card)',
                        border: '1px solid var(--border-color)',
                        borderRadius: 8,
                        fontSize: 13,
                      }}
                      formatter={(v) => [`${v}%`]}
                    />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </section>

          </div>
        )}
      </div>

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
