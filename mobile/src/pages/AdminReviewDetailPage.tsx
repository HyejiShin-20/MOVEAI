import { useState } from 'react'
import { AdminShell } from '../components/AdminShell'

const assets = {
  save: 'https://www.figma.com/api/mcp/asset/51d587f3-f769-4b0c-8f4b-a266c4c6058b.svg',
  done: 'https://www.figma.com/api/mcp/asset/764093fb-0b7c-42dd-a2d5-16a285d897f0.svg',
  source: 'https://www.figma.com/api/mcp/asset/e23a37c3-1f09-45ce-818d-48cfc9585f4e.svg',
  pin: 'https://www.figma.com/api/mcp/asset/1bce6de9-ee0e-4081-ad80-302051fdb46f.svg',
  play: 'https://www.figma.com/api/mcp/asset/2ec1114d-50bd-46f4-9ba7-e9abe00db650.svg',
  transcript: 'https://www.figma.com/api/mcp/asset/e8b93c35-2f45-4e6a-a0c5-78494fb9b707.svg',
  ai: 'https://www.figma.com/api/mcp/asset/7ea20510-7198-42df-8806-3b402d77ef42.svg',
  count: 'https://www.figma.com/api/mcp/asset/f3213649-12c3-4c8e-bc97-f8b51e55cf56.svg',
  add: 'https://www.figma.com/api/mcp/asset/12f5fa18-62cb-4e42-aa67-eb5719065b16.svg',
  reject: 'https://www.figma.com/api/mcp/asset/4302f05a-1227-4cbd-b330-b8f317785efc.svg',
  edit: 'https://www.figma.com/api/mcp/asset/d68d782b-d6da-4600-a2f3-41e651e67bd2.svg',
  approve: 'https://www.figma.com/api/mcp/asset/3d4f7eea-8f54-473d-93da-adfd0900c452.svg',
  map: 'https://www.figma.com/api/mcp/asset/fd0075e7-3e22-4138-a2d4-910d961c8dd0.png',
} as const

type KnowledgeStatus = 'idle' | 'approved' | 'rejected' | 'editing'

type KnowledgeCardProps = {
  type: string
  category: string
  confidence: number
  sentence: string
  target?: string
  evidence: string
}

function KnowledgeCard({ type, category, confidence, sentence, target, evidence }: KnowledgeCardProps) {
  const [status, setStatus] = useState<KnowledgeStatus>('idle')
  return (
    <article className={`admin-knowledge-card is-${status}`}>
      <div className="admin-knowledge-card__head">
        <div><span>{type}</span><em>{category}</em></div>
        <b><i />신뢰도 {confidence}%</b>
      </div>
      <div className="admin-knowledge-grid">
        <label className={target ? '' : 'span-2'}><span>안내 문장 (생성됨)</span><textarea readOnly={status !== 'editing'} defaultValue={sentence} /></label>
        {target && <label><span>이동 방식 / 대상</span><div className="admin-static-field">{target}</div></label>}
        <label className="span-2"><span>근거 원문 (매칭)</span><blockquote>{evidence}</blockquote></label>
      </div>
      <div className="admin-knowledge-actions">
        <button className="reject" type="button" onClick={() => setStatus('rejected')}><img src={assets.reject} alt="" />반려</button>
        <button type="button" onClick={() => setStatus('editing')}><img src={assets.edit} alt="" />수정</button>
        <button className="approve" type="button" onClick={() => setStatus('approved')}><img src={assets.approve} alt="" />승인</button>
      </div>
      {status !== 'idle' && <span className="admin-knowledge-result">{status === 'approved' ? '승인됨' : status === 'rejected' ? '반려됨' : '수정 가능 상태'}</span>}
    </article>
  )
}

export function AdminReviewDetailPage() {
  const [playing, setPlaying] = useState(false)
  const waveform = [8,16,12,24,32,28,20,8,12,8,16,24,20,32,16,8,12,20,8,16,12,24]

  return (
    <AdminShell active="review" profileMode="initials">
      <div className="admin-review-detail-page">
        <header className="admin-review-detail-heading">
          <div>
            <div className="admin-review-detail-meta"><span>REP-2023-0891A</span><em>검수 대기</em></div>
            <h1>제보 상세 검수</h1>
            <p>원본 오디오 파일과 AI가 추출한 지식 데이터를 대조하여 개별 승인/반려를 진행합니다.</p>
          </div>
          <div className="admin-review-detail-actions"><button type="button"><img src={assets.save} alt="" />임시 저장</button><button className="primary" type="button"><img src={assets.done} alt="" />검수 완료</button></div>
        </header>

        <div className="admin-review-detail-grid">
          <section className="admin-source-panel">
            <h2><img src={assets.source} alt="" />원본 제보 데이터</h2>
            <div className="admin-source-location"><img src={assets.pin} alt="" /><div><strong>현장 위치 정보</strong><span>서울특별시 강남구 테헤란로 152, 강남파이낸스센터 B1 하역장</span></div></div>
            <div className="admin-source-map"><img src={assets.map} alt="현장 지도" /></div>
            <div className="admin-audio-box">
              <div><strong>제보 녹음본</strong><span>{playing ? '00:08' : '00:00'} / 00:24</span></div>
              <div className="admin-audio-controls"><button type="button" onClick={() => setPlaying((v) => !v)}><img src={assets.play} alt={playing ? '일시정지' : '재생'} /></button><div className="admin-waveform">{waveform.map((height,index)=><i key={index} className={playing && index < 7 ? 'is-active' : ''} style={{height}} />)}</div></div>
            </div>
            <div className="admin-transcript"><h3><img src={assets.transcript} alt="" />원본 STT 텍스트</h3><p>“어, 여기 강남파이낸스센터 지하 1층 하역장인데요. 화물차 탑차 높이가 2.3미터 이상이면 못 들어갑니다. 그리고 하역장 입구 들어오자마자 우측으로 돌아서 기둥 번호 B-04번 앞에 정차해야 엘리베이터 쓰기 편해요. 다른데 세우면 관리인이 차 빼라고 엄청 뭐라 합니다.”</p></div>
          </section>

          <section className="admin-ai-panel">
            <div className="admin-ai-heading"><div><img src={assets.ai} alt="" /><h2>AI 추출 지식</h2><span><img src={assets.count} alt="" />총 2건 추출됨</span></div><button type="button"><img src={assets.add} alt="" />지식 수동 추가</button></div>
            <KnowledgeCard type="진입 제한" category="높이 제한" confidence={98} sentence="화물차 (탑차) 높이 2.3m 이상 진입 불가" target="화물차, 탑차" evidence="“화물차 탑차 높이가 2.3미터 이상이면 못 들어갑니다.”" />
            <KnowledgeCard type="정차 위치" category="하역 구역" confidence={92} sentence="하역장 입구 진입 후 우회전하여 기둥 B-04번 앞에 정차 요망. (타 구역 정차시 제재 있음)" evidence="“하역장 입구 들어오자마자 우측으로 돌아서 기둥 번호 B-04번 앞에 정차... 다른데 세우면 관리인이 차 빼라고 엄청 뭐라 합니다.”" />
          </section>
        </div>
      </div>
    </AdminShell>
  )
}
