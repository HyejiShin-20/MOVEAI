import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const backIcon = 'https://www.figma.com/api/mcp/asset/38a46000-d10f-43c3-8f80-f7ed55acf238.svg'
const homeIcon = 'https://www.figma.com/api/mcp/asset/23e8c18e-0052-4c17-8d61-0b319cb14f61.svg'
const micIcon = 'https://www.figma.com/api/mcp/asset/2c611a36-4583-43da-9df4-c0f7d0ed0683.svg'
const trashIcon = 'https://www.figma.com/api/mcp/asset/d42883de-41ae-42d5-9371-09829afaf05b.svg'
const missingIcon = 'https://www.figma.com/api/mcp/asset/f5241140-d5d4-4aa3-ace4-c6d106d4a85e.svg'
const failedIcon = 'https://www.figma.com/api/mcp/asset/a4ecac44-38c0-4053-be48-047c771e9544.svg'
const arrowIcon = 'https://www.figma.com/api/mcp/asset/2d3918b8-c681-4102-99f6-300ba087cdc0.svg'
const placeIcon = 'https://www.figma.com/api/mcp/asset/742de8aa-3cf3-45d8-be5d-a0cccf4b883d.svg'
const draftIcon = 'https://www.figma.com/api/mcp/asset/8d4e2619-f6c1-4f18-ab2e-c3acaa776fdd.svg'

type Draft = { id: number; date: string; duration: string; place?: string; failed?: boolean }
const initial: Draft[] = [
  { id: 1, date: '2023.10.24 14:30', duration: '00:45', failed: true },
  { id: 2, date: '2023.10.24 11:15', duration: '01:20', place: '강남구 역삼동' },
  { id: 3, date: '2023.10.23 09:00', duration: '00:15' },
]

export function DraftReportsPage() {
  const navigate = useNavigate()
  const [drafts, setDrafts] = useState(initial)
  return (
    <div className="mobile-page drafts-page">
      <header className="transaction-header drafts-header"><button type="button" onClick={() => navigate('/home')}><img src={backIcon} alt="" /></button><strong>작성 중인 제보</strong><span /></header>
      <main className="drafts-main">
        <div className="drafts-summary"><span>총 {drafts.length}건</span><button type="button" onClick={() => setDrafts([])}>전체 삭제</button></div>
        {drafts.map((draft) => (
          <article className="draft-card" key={draft.id}>
            <div className="draft-card__head">
              <div><strong>{draft.date}</strong><span><img src={micIcon} alt="" />{draft.duration}</span></div>
              <button type="button" aria-label="삭제" onClick={() => setDrafts((current) => current.filter((item) => item.id !== draft.id))}><img src={trashIcon} alt="" /></button>
            </div>
            <div className="draft-tags">
              {draft.place ? <span className="draft-tag draft-tag--place"><img src={placeIcon} alt="" />{draft.place}</span> : <span className="draft-tag draft-tag--missing"><img src={missingIcon} alt="" />장소 미지정</span>}
              {draft.failed ? <span className="draft-tag draft-tag--failed"><img src={failedIcon} alt="" />업로드 실패</span> : <span className="draft-tag draft-tag--saved"><img src={draftIcon} alt="" />임시 저장</span>}
            </div>
            <div className="draft-card__divider" />
            <button className="draft-continue" type="button" onClick={() => navigate(draft.place ? '/reports/transcription' : '/reports/place')}>이어서 작성<img src={arrowIcon} alt="" /></button>
          </article>
        ))}
      </main>
      <footer className="drafts-footer"><button type="button" onClick={() => navigate('/home')}><img src={homeIcon} alt="" />메인 화면으로 돌아가기</button></footer>
    </div>
  )
}
