import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { ModerationDraftSummary } from '../api/models'
import { AdminShell } from '../components/AdminShell'

const pinIcon = 'https://www.figma.com/api/mcp/asset/a6047dc0-5d92-4482-9268-f6980d692d1b.svg'

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

export function AdminReviewListPage() {
  const navigate = useNavigate()
  const [drafts, setDrafts] = useState<ModerationDraftSummary[]>([])
  const [place, setPlace] = useState('전체 장소')
  const [date, setDate] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true); setError('')
    api.moderationDrafts().then(setDrafts).catch((reason) => setError(reason instanceof Error ? reason.message : '검수 목록을 불러오지 못했습니다.')).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const places = useMemo(() => [...new Set(drafts.map((draft) => draft.placeName))], [drafts])
  const filtered = useMemo(() => drafts.filter((draft) => (place === '전체 장소' || draft.placeName === place) && (!date || draft.createdAt.startsWith(date))), [date, drafts, place])

  return (
    <AdminShell active="review" searchPlaceholder="제보 번호 또는 장소 검색...">
      <div className="admin-page admin-review-list-page">
        <header className="admin-review-list-heading"><div><h1>검수 대기 목록</h1><p>AI가 구조화한 실제 제보 데이터를 검토하고 승인하세요.</p></div><span className="admin-count-pill">총 <b>{drafts.length}</b>건 대기 중</span></header>
        <section className="admin-filter-card admin-filter-card--moderation">
          <label><span>등록 장소</span><select value={place} onChange={(event) => setPlace(event.target.value)}><option>전체 장소</option>{places.map((name) => <option key={name}>{name}</option>)}</select></label>
          <label><span>등록 날짜</span><input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></label>
          <button className="admin-refresh-button" type="button" onClick={load} disabled={loading}>{loading ? '불러오는 중...' : '새로고침'}</button>
        </section>
        <section className="admin-review-table-card">
          <div className="admin-review-table__row is-head"><span>초안 번호</span><span>장소</span><span>등록 일시</span><span>제보 번호</span><span>AI 추출 요약</span><span>현재 상태</span><span>액션</span></div>
          {filtered.map((draft) => <div className="admin-review-record" key={draft.draftId}><div className="admin-review-table__row">
            <span>DRAFT-{draft.draftId}</span><span className="admin-review-place"><img src={pinIcon} alt="" />{draft.placeName}</span><span>{formatDate(draft.createdAt)}</span><span className="align-right">REP-{draft.reportId}</span><span className="admin-review-summary">{draft.summary}</span><span className="align-center"><em className="admin-review-state">검수 대기</em></span><span className="align-center"><button className="admin-table-action" type="button" onClick={() => navigate(`/admin/reviews/${draft.draftId}`)}>검수하기</button></span>
          </div></div>)}
          {!loading && !error && filtered.length === 0 && <p className="admin-table-state">조건에 맞는 검수 대기 초안이 없습니다.</p>}
          {error && <p className="admin-table-state is-error" role="alert">{error}</p>}
          <footer className="admin-pagination"><span>Showing {filtered.length} of {drafts.length} entries</span><div><button type="button" disabled>‹</button><button type="button" className="is-active">1</button><button type="button" disabled>›</button></div></footer>
        </section>
      </div>
    </AdminShell>
  )
}
