import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const backIcon = 'https://www.figma.com/api/mcp/asset/aeaac1af-4fe7-4014-a797-914aee185414.svg'
const homeIcon = 'https://www.figma.com/api/mcp/asset/42f02dbd-f3b3-477e-9bb4-50e3471a5b3d.svg'
const micIcon = 'https://www.figma.com/api/mcp/asset/46314bf2-577f-4412-8980-9040090a7e32.svg'
const trashIcon = 'https://www.figma.com/api/mcp/asset/414857f3-4e7b-46ae-a0e8-99e0b044aefc.svg'
const missingIcon = 'https://www.figma.com/api/mcp/asset/8c442866-b280-4a84-a970-87f6fa07bea6.svg'
const failedIcon = 'https://www.figma.com/api/mcp/asset/e68b7bb3-06ee-48de-a43b-cc8b528cdf3e.svg'
const arrowIcon = 'https://www.figma.com/api/mcp/asset/c9b3f038-99cd-415c-b4ac-acd86d6ec41a.svg'
const placeIcon = 'https://www.figma.com/api/mcp/asset/b4842ad5-9719-446b-96c2-58b149e6b204.svg'
const draftIcon = 'https://www.figma.com/api/mcp/asset/5924edfb-5c6a-47d9-9a21-053290f138d7.svg'

type Draft = {
  id: number
  date: string
  duration: string
  place?: string
  failed?: boolean
}

const initialDrafts: Draft[] = [
  { id: 1, date: '2023.10.24 14:30', duration: '00:45', failed: true },
  { id: 2, date: '2023.10.24 11:15', duration: '01:20', place: '강남구 역삼동' },
  { id: 3, date: '2023.10.23 09:00', duration: '00:15' },
]

export function DraftReportsPage() {
  const navigate = useNavigate()
<<<<<<< Updated upstream
  const [drafts, setDrafts] = useState(initialDrafts)
=======
  const [drafts, setDrafts] = useState<Draft[]>(() => {
    const saved = reportDraftStore.get()
    const hasCurrentDraft = saved.stage !== 'recording' || Boolean(saved.transcript) || Boolean(saved.selectedPlace)
    if (!hasCurrentDraft) return initialDrafts

    const savedDraft: Draft = {
      id: -1,
      date: new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(saved.updatedAt)),
      duration: '00:15',
      place: saved.selectedPlace?.name,
      stage: saved.stage,
      persisted: true,
    }
    return [savedDraft, ...initialDrafts]
  })

  const deleteDraft = (draft: Draft) => {
    if (draft.persisted) reportDraftStore.clear()
    setDrafts((current) => current.filter((item) => item.id !== draft.id))
  }

  const clearDrafts = () => {
    reportDraftStore.clear()
    setDrafts([])
  }
>>>>>>> Stashed changes

  return (
    <div className="mobile-page drafts-page" data-figma-node="118:8354">
      <header className="transaction-header drafts-header">
<<<<<<< Updated upstream
        <button type="button" aria-label="뒤로가기" onClick={() => navigate('/home')}><img src={backIcon} alt="" /></button>
=======
        <button type="button" aria-label="뒤로가기" onClick={() => navigate(routes.home)}><img src={backIcon} alt="" /></button>
>>>>>>> Stashed changes
        <strong>작성 중인 제보</strong>
        <span />
      </header>

      <main className="drafts-main">
        <div className="drafts-summary">
          <span>총 {drafts.length}건</span>
<<<<<<< Updated upstream
          <button type="button" disabled={drafts.length === 0} onClick={() => setDrafts([])}>전체 삭제</button>
=======
          <button type="button" disabled={drafts.length === 0} onClick={clearDrafts}>전체 삭제</button>
>>>>>>> Stashed changes
        </div>

        {drafts.map((draft) => (
          <article className="draft-card" key={draft.id}>
            <div className="draft-card__head">
              <div>
                <strong>{draft.date}</strong>
                <span><img src={micIcon} alt="" />{draft.duration}</span>
              </div>
<<<<<<< Updated upstream
              <button type="button" aria-label={`${draft.date} 제보 삭제`} onClick={() => setDrafts((current) => current.filter((item) => item.id !== draft.id))}>
=======
              <button type="button" aria-label={`${draft.date} 제보 삭제`} onClick={() => deleteDraft(draft)}>
>>>>>>> Stashed changes
                <img src={trashIcon} alt="" />
              </button>
            </div>

            <div className="draft-tags">
              {draft.place ? (
                <span className="draft-tag draft-tag--place"><img src={placeIcon} alt="" />{draft.place}</span>
              ) : (
                <span className="draft-tag draft-tag--missing"><img src={missingIcon} alt="" />장소 미지정</span>
              )}
              {draft.failed ? (
                <span className="draft-tag draft-tag--failed"><img src={failedIcon} alt="" />업로드 실패</span>
              ) : (
                <span className="draft-tag draft-tag--saved"><img src={draftIcon} alt="" />임시 저장</span>
              )}
            </div>

            <div className="draft-card__divider" />
<<<<<<< Updated upstream
            <button className="draft-continue" type="button" onClick={() => navigate(draft.place ? '/reports/transcription' : '/reports/place')}>
=======
            <button className="draft-continue" type="button" onClick={() => navigate(draft.persisted ? resumePath(draft.stage) : (draft.place ? routes.reportTranscription : routes.reportPlace))}>
>>>>>>> Stashed changes
              이어서 작성<img src={arrowIcon} alt="" />
            </button>
          </article>
        ))}

        {drafts.length === 0 && <p className="drafts-empty">작성 중인 제보가 없습니다.</p>}
      </main>

      <footer className="drafts-footer">
<<<<<<< Updated upstream
        <button type="button" onClick={() => navigate('/home')}><img src={homeIcon} alt="" />메인 화면으로 돌아가기</button>
=======
        <button type="button" onClick={() => navigate(routes.home)}><img src={homeIcon} alt="" />메인 화면으로 돌아가기</button>
>>>>>>> Stashed changes
      </footer>
    </div>
  )
}
