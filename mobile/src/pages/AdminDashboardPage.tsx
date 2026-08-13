import { useNavigate } from 'react-router-dom'
import { AdminShell } from '../components/AdminShell'

const assets = {
  total: 'https://www.figma.com/api/mcp/asset/55e67fe7-2655-4fe1-b146-5cf605eff54b.svg',
  pending: 'https://www.figma.com/api/mcp/asset/5043bd30-7345-43af-abf2-83b12c3eda0e.svg',
  approved: 'https://www.figma.com/api/mcp/asset/b89243e3-70a2-4304-9761-15c02176bf15.svg',
  revision: 'https://www.figma.com/api/mcp/asset/da41d484-c3f7-4407-ba53-858fbe3c5392.svg',
  up: 'https://www.figma.com/api/mcp/asset/830d5017-7b23-4287-b2d0-ca3036677902.svg',
  checkUp: 'https://www.figma.com/api/mcp/asset/75f29b13-d3db-4195-b887-525d63adf9df.svg',
  eye: 'https://www.figma.com/api/mcp/asset/065183a5-727a-4120-b49d-148883643817.svg',
  warning: 'https://www.figma.com/api/mcp/asset/20305703-4744-4bd2-9f5b-f19076ae5b5a.svg',
  request: 'https://www.figma.com/api/mcp/asset/14dbaeee-2d92-46b0-8f3c-3e2ec76b3040.svg',
} as const

const recentReports = [
  { id: '#8902', name: '강남대로 물류센터 A동', address: '서울 강남구 강남대로 123', type: '출입구', status: '검수 대기', tone: 'pending' },
  { id: '#8901', name: '판교 테크노밸리 지하 2층', address: '경기 성남시 분당구 판교역로', type: '하역장', status: '승인 완료', tone: 'approved' },
  { id: '#8900', name: '여의도 파크원 타워', address: '서울 영등포구 여의대로 108', type: '경로', status: '보완 필요', tone: 'revision' },
  { id: '#8899', name: '부산항 신항 물류센터', address: '부산 강서구 신항남로', type: '출입구', status: '검수 대기', tone: 'pending' },
  { id: '#8898', name: '인천공항 화물터미널', address: '인천 중구 공항동로', type: '하역장', status: '승인 완료', tone: 'approved' },
]

const missingRoutes = [
  ['잠실 롯데월드몰 지하 3층', '최근 7일 기사 요청 급증', '요청 142건'],
  ['코엑스몰 화물 진입로 B', '복잡도 매우 높음', '요청 98건'],
  ['동대문 DDP 패션몰', '주차장 입구 혼선 잦음', '요청 76건'],
  ['송도 센트럴파크 푸르지오', '신규 입주 단지', '요청 65건'],
  ['판교 알파돔 타워', '동선 데이터 부족', '요청 52건'],
]

export function AdminDashboardPage() {
  const navigate = useNavigate()
  const bars = [42, 63, 89, 58, 49, 73, 68]
  return (
    <AdminShell active="dashboard">
      <div className="admin-page admin-dashboard-page">
        <header className="admin-page-heading">
          <h1>대시보드 요약</h1>
          <p>전체 제보 현황 및 운영 주요 지표입니다.</p>
        </header>

        <section className="admin-metric-grid">
          <article className="admin-metric-card">
            <div className="admin-metric-card__label"><span>전체 제보</span><img src={assets.total} alt="" /></div>
            <strong>12,405</strong>
            <span className="admin-metric-card__delta is-good"><img src={assets.up} alt="" /> +4.2% 이번 달</span>
          </article>
          <article className="admin-metric-card">
            <div className="admin-metric-card__label"><span>검수 대기</span><img src={assets.pending} alt="" /></div>
            <strong>248</strong>
            <span className="admin-metric-card__delta is-warn">우선 처리 필요 42건</span>
          </article>
          <article className="admin-metric-card admin-metric-card--revision">
            <div className="admin-metric-card__label"><span>보완 필요</span><img src={assets.revision} alt="" /></div>
            <strong>65</strong>
            <span className="admin-metric-card__delta">AI 자동 분류 항목 포함</span>
          </article>
          <article className="admin-metric-card">
            <div className="admin-metric-card__label"><span>오늘 승인</span><img src={assets.approved} alt="" /></div>
            <strong className="is-blue">856</strong>
            <span className="admin-metric-card__delta is-good"><img src={assets.checkUp} alt="" /> 어제 대비 +12%</span>
          </article>
        </section>

        <section className="admin-dashboard-charts">
          <article className="admin-panel admin-trend-panel">
            <div className="admin-panel-heading">
              <h2>일별 제보 등록 추이</h2>
              <select defaultValue="7"><option value="7">최근 7일</option><option value="30">최근 30일</option></select>
            </div>
            <div className="admin-bar-chart">
              <div className="admin-bar-chart__y"><span>400</span><span>300</span><span>200</span><span>100</span><span>0</span></div>
              <div className="admin-bar-chart__plot">
                {bars.map((value, index) => (
                  <div className="admin-bar-chart__col" key={index}>
                    <div className={`admin-bar-chart__bar ${index === 2 ? 'is-active' : ''}`} style={{ height: `${value}%` }}>
                      {index === 2 && <span className="admin-chart-tooltip">340</span>}
                    </div>
                    <span>{index === 6 ? '오늘' : `10/${12 + index}`}</span>
                  </div>
                ))}
              </div>
            </div>
          </article>

          <article className="admin-panel admin-donut-panel">
            <div className="admin-panel-heading"><h2>상태별 제보 비중</h2></div>
            <div className="admin-donut-wrap">
              <div className="admin-donut"><div><span>총 처리 건수</span><strong>3,248</strong></div></div>
            </div>
            <div className="admin-donut-legend">
              <div><span><i className="approved" />승인 완료</span><b>65%</b></div>
              <div><span><i className="pending" />검수 대기</span><b>20%</b></div>
              <div><span><i className="revision" />보완 필요</span><b>15%</b></div>
            </div>
          </article>
        </section>

        <section className="admin-dashboard-tables">
          <article className="admin-panel admin-recent-panel">
            <div className="admin-panel-heading admin-panel-heading--bordered">
              <h2>최근 등록 제보</h2>
              <button type="button" onClick={() => navigate('/admin/reviews')}>전체보기</button>
            </div>
            <div className="admin-recent-table">
              <div className="admin-recent-table__row is-head"><span>ID</span><span>장소명 / 위치</span><span>유형</span><span>상태</span><span>동작</span></div>
              {recentReports.map((report) => (
                <div className="admin-recent-table__row" key={report.id}>
                  <span className="muted">{report.id}</span>
                  <span><strong>{report.name}</strong><small>{report.address}</small></span>
                  <span>{report.type}</span>
                  <span><em className={`admin-status admin-status--${report.tone}`}>{report.status}</em></span>
                  <span><button className="admin-eye-button" type="button" onClick={() => navigate('/admin/reviews/REP-9923')}><img src={assets.eye} alt="보기" /></button></span>
                </div>
              ))}
            </div>
          </article>

          <article className="admin-panel admin-missing-panel">
            <div className="admin-panel-heading admin-panel-heading--bordered admin-missing-heading"><h2><img src={assets.warning} alt="" /> 경로 미등록 장소 TOP 5</h2></div>
            <ol className="admin-missing-list">
              {missingRoutes.map(([name, note, count], index) => (
                <li key={name}>
                  <span className={`admin-rank ${index === 0 ? 'is-first' : ''}`}>{index + 1}</span>
                  <span className="admin-missing-list__copy"><strong>{name}</strong><small>{note}</small></span>
                  <span className={index === 0 ? 'is-danger' : ''}>{index === 0 && <img src={assets.request} alt="" />}{count}</span>
                </li>
              ))}
            </ol>
          </article>
        </section>
      </div>
    </AdminShell>
  )
}
