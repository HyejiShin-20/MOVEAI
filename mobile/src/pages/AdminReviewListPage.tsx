import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AdminShell } from '../components/AdminShell'

const assets = {
  pin: 'https://www.figma.com/api/mcp/asset/a6047dc0-5d92-4482-9268-f6980d692d1b.svg',
  ai: 'https://www.figma.com/api/mcp/asset/05cd2927-1e2f-4f14-bb6f-3163b24bef55.svg',
} as const

type Report = {
  id: string
  place: string
  date: string
  duration: string
  knowledge: number
  status: '검수 중' | '검수 대기'
}

const reports: Report[] = [
  { id: 'REP-9923', place: '서울 물류센터 A동 (하역장 3번)', date: '2023.10.24 14:30', duration: '00:45', knowledge: 3, status: '검수 중' },
  { id: 'REP-9922', place: '경기 남부 허브 (B구역 외곽)', date: '2023.10.24 13:15', duration: '01:12', knowledge: 1, status: '검수 대기' },
  { id: 'REP-9920', place: '인천항 제2부두 (진입로)', date: '2023.10.24 11:05', duration: '00:20', knowledge: 2, status: '검수 대기' },
  { id: 'REP-9918', place: '서울 송파구 문정동 (골목 진입)', date: '2023.10.24 09:40', duration: '02:05', knowledge: 5, status: '검수 대기' },
]

export function AdminReviewListPage() {
  const navigate = useNavigate()
  const [location, setLocation] = useState('전체 구역')
  const [category, setCategory] = useState('전체 카테고리')
  const [movement, setMovement] = useState('전체 방식')
  const filtered = useMemo(() => reports, [location, category, movement])

  return (
    <AdminShell active="review" searchPlaceholder="제보 번호 또는 장소 검색...">
      <div className="admin-page admin-review-list-page">
        <header className="admin-review-list-heading">
          <div><h1>검수 대기 목록</h1><p>AI가 구조화한 제보 데이터를 검토하고 승인하세요.</p></div>
          <span className="admin-count-pill">총 <b>142</b>건 대기 중</span>
        </header>

        <section className="admin-filter-card">
          <label><span>등록 장소</span><select value={location} onChange={(e) => setLocation(e.target.value)}><option>전체 구역</option><option>서울</option><option>경기</option><option>인천</option></select></label>
          <label><span>등록 날짜</span><input type="date" /></label>
          <label><span>지식 카테고리</span><select value={category} onChange={(e) => setCategory(e.target.value)}><option>전체 카테고리</option><option>진입 제한</option><option>정차 위치</option><option>내부 동선</option></select></label>
          <label><span>이동 방식</span><div className="admin-filter-action"><select value={movement} onChange={(e) => setMovement(e.target.value)}><option>전체 방식</option><option>DRIVE</option><option>WALK</option></select><button type="button">검색</button></div></label>
        </section>

        <section className="admin-review-table-card">
          <div className="admin-review-table__row is-head"><span>제보 번호</span><span>장소 (세부 구역)</span><span>등록 일시</span><span>음성 길이</span><span>AI 추출 지식 수</span><span>현재 상태</span><span>액션</span></div>
          {filtered.map((report, index) => (
            <div className="admin-review-record" key={report.id}>
              <div className="admin-review-table__row">
                <span>{report.id}</span>
                <span className="admin-review-place"><img src={assets.pin} alt="" />{report.place}</span>
                <span>{report.date}</span>
                <span className="align-right">{report.duration}</span>
                <span className="align-center"><b className="admin-knowledge-count">{report.knowledge}</b></span>
                <span className="align-center"><em className={`admin-review-state ${report.status === '검수 중' ? 'is-reviewing' : ''}`}>{report.status}</em></span>
                <span className="align-center"><button className="admin-table-action" type="button" onClick={() => navigate(`/admin/reviews/${report.id}`)}>검수하기</button></span>
              </div>
              {index === 0 && (
                <div className="admin-ai-preview-row">
                  <img src={assets.ai} alt="AI" />
                  <div><small>지식 1 (위험 요소)</small><strong>하역장 바닥 미끄럼 주의</strong></div>
                  <div><small>지식 2 (시설물)</small><strong>3번 게이트 센서 오작동</strong></div>
                  <div><small>지식 3 (경로)</small><strong>우회로 진입 폭 좁음</strong></div>
                </div>
              )}
            </div>
          ))}
          <footer className="admin-pagination"><span>Showing 1 to 10 of 142 entries</span><div><button type="button" disabled>‹</button><button type="button" className="is-active">1</button><button type="button">2</button><button type="button">3</button><span>...</span><button type="button">15</button><button type="button">›</button></div></footer>
        </section>
      </div>
    </AdminShell>
  )
}
